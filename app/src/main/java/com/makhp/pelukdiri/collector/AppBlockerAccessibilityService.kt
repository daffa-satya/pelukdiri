package com.makhp.pelukdiri.collector

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import com.makhp.pelukdiri.core.domain.repository.UsageRepository
import com.makhp.pelukdiri.core.domain.usecase.EvaluateInterventionEligibilityUseCase
import com.makhp.pelukdiri.core.domain.usecase.AttemptInterventionLaunchUseCase
import com.makhp.pelukdiri.features.intervention.InterventionActivity
import com.makhp.pelukdiri.features.intervention.ActiveInterventionSession
import com.makhp.pelukdiri.core.domain.InterventionLaunchPolicy
import com.makhp.pelukdiri.core.domain.engine.InterventionChallengeType
import com.makhp.pelukdiri.core.domain.time.TimeProvider
import com.makhp.pelukdiri.core.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class AppBlockerAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var appUsageCollector: AppUsageCollector

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var usageRepository: UsageRepository

    @Inject
    lateinit var evaluateInterventionEligibilityUseCase: EvaluateInterventionEligibilityUseCase

    @Inject
    lateinit var attemptInterventionLaunchUseCase: AttemptInterventionLaunchUseCase

    @Inject
    lateinit var lockManager: com.makhp.pelukdiri.core.domain.InterventionLockManager

    @Inject lateinit var activeInterventionSession: ActiveInterventionSession
    @Inject lateinit var launchPolicy: InterventionLaunchPolicy
    @Inject lateinit var timeProvider: TimeProvider
    @Inject lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentMonitoredPackages = emptySet<String>()
    private var currentForegroundPackage: String? = null
    private var foregroundTrackingJob: Job? = null
    private val TICK_INTERVAL_MS = 5_000L
    private val EVALUATION_THROTTLE_MS = 30_000L
    private val EVALUATION_TIMEOUT_MS = 10_000L
    private val SYNC_INTERVAL_MS = 30_000L
    private val FORCED_TEST_INTERVAL_MINUTES = 5L
    private var lastSyncTimestamp: Long = 0L
    private var lastEvaluationTimestamp: Long = 0L

    // These packages are still recorded as usage; they are excluded only from interventions
    // so PelukDiri cannot block itself or Android navigation/recovery surfaces.
    private val nonInterventionPackages = setOf(
        "app.olauncher",
        "com.android.settings",
        "com.miui.securitycenter",
        "com.miui.home",
        "com.android.systemui",
        "com.mi.globalminusscreen",
        "juloo.keyboard2"
    )

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            userPreferencesRepository.monitoredPackages.collect { packages ->
                currentMonitoredPackages = packages

                // If the current app just became a target app, start tracking
                val foreground = currentForegroundPackage
                if (foreground != null && packages.contains(foreground) && foregroundTrackingJob == null) {
                    startForegroundTracking(foreground)
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            val eventType = event?.eventType
            val eventPackage = event?.packageName?.toString() ?: return
            val activeWindowPackage = rootInActiveWindow?.packageName?.toString()
            val packageName = ForegroundPackageResolver.resolve(
                eventPackage = eventPackage,
                activeWindowPackage = activeWindowPackage
            )
            
            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                handlePackageChanged(packageName)
            }
        } catch (_: Exception) {
            Log.e("AppBlockerService", "Accessibility event handling failed")
        }
    }

    private suspend fun evaluateIntervention(packageName: String) {
        val savedSession = activeInterventionSession.restore()
        if (savedSession != null) {
            lockManager.acquireLock()
            restoreActiveIntervention()
            return
        }
        
        val decision = evaluateInterventionEligibilityUseCase(packageName)
        val controlResult = decision.controlResult

        if (decision.shouldTrigger && controlResult != null) {
            attemptInterventionLaunchUseCase(controlResult) {
                launchInterventionOverlay(
                    packageName = packageName,
                    monitoredUsageMinutes = decision.monitoredUsageMinutes,
                    intervalMinutesAtLaunch = controlResult.intervalMinutes,
                    ambientLightLuxAtLaunch = decision.ambientLux,
                    deviation = controlResult.deviation ?: 0.0,
                    difficultyControlSignal = controlResult.normalizedDifficultyControl,
                    difficulty = controlResult.nextDifficulty,
                    challengeType = decision.challengeType,
                )
            }
        }
    }

    private fun handlePackageChanged(newPackage: String) {
        val shouldTrack = ForegroundTrackingPolicy.shouldTrack(
            resolvedPackage = newPackage,
            ownPackage = packageName,
            monitoredPackages = currentMonitoredPackages,
            excludedPackages = nonInterventionPackages,
        )
        if (newPackage == currentForegroundPackage) {
            if (shouldTrack && ForegroundTrackingPolicy.shouldRestart(
                    resolvedPackage = newPackage,
                    currentPackage = currentForegroundPackage,
                    monitoredPackages = currentMonitoredPackages,
                    trackingJobActive = foregroundTrackingJob?.isActive == true,
                )
            ) {
                if (lockManager.isLocked.value) restoreActiveIntervention() else startForegroundTracking(newPackage)
            }
            return
        }
        
        // 1. Stop previous tracking
        foregroundTrackingJob?.cancel()
        foregroundTrackingJob = null

        // Reset throttle on package change to allow immediate check if it's a new monitored app
        lastEvaluationTimestamp = 0

        // 2. Sync usage for previous package if it was a target app
        val previous = currentForegroundPackage
        if (previous != null && currentMonitoredPackages.contains(previous)) {
            serviceScope.launch { usageRepository.refreshUsageData() }
            appUsageCollector.stopLightSensor()
        }

        currentForegroundPackage = newPackage

        // 3. Start tracking if new package is a target app
        if (shouldTrack) {
            val forcedChallenge = launchPolicy.consumeForcedChallenge()
            if (forcedChallenge != null) {
                serviceScope.launch { launchForcedIntervention(newPackage, forcedChallenge) }
            } else if (lockManager.isLocked.value) {
                restoreActiveIntervention()
            } else {
                startForegroundTracking(newPackage)
            }
        }
    }

    private suspend fun launchForcedIntervention(
        packageName: String,
        challengeType: InterventionChallengeType,
    ) {
        if (!lockManager.acquireLock()) return
        val difficulty = userPreferencesRepository.currentDifficulty.first()
        val launched = launchInterventionOverlay(
            packageName = packageName,
            monitoredUsageMinutes = 90.0,
            intervalMinutesAtLaunch = FORCED_TEST_INTERVAL_MINUTES.toDouble(),
            ambientLightLuxAtLaunch = appUsageCollector.getCurrentAmbientLightLux(),
            deviation = 0.4,
            difficultyControlSignal = 0.5,
            difficulty = difficulty,
            challengeType = challengeType,
        )
        if (launched) {
            userPreferencesRepository.setNextEligibleInterventionAt(
                timeProvider.nowMillis() + FORCED_TEST_INTERVAL_MINUTES * 60_000L
            )
        } else {
            lockManager.releaseLock()
        }
    }

    private fun startForegroundTracking(packageName: String) {
        foregroundTrackingJob?.cancel()
        appUsageCollector.startLightSensor()
        val trackingJob = serviceScope.launch {
            while (isActive) {
                val currentTime = timeProvider.nowMillis()
                
                // Periodically flush data to Room/Prefs (every 30s)
                if (currentTime - lastSyncTimestamp > SYNC_INTERVAL_MS) {
                    runCatching { usageRepository.refreshUsageData() }
                        .onFailure { Log.e("AppBlockerService", "Periodic usage sync failed") }
                    lastSyncTimestamp = currentTime
                }

                // Throttle evaluation to 30s
                if (currentTime - lastEvaluationTimestamp >= EVALUATION_THROTTLE_MS) {
                    runCatching {
                        withTimeout(EVALUATION_TIMEOUT_MS) { evaluateIntervention(packageName) }
                    }
                        .onFailure { Log.e("AppBlockerService", "Periodic intervention evaluation failed") }
                    lastEvaluationTimestamp = currentTime
                }
                
                delay(TICK_INTERVAL_MS)
            }
        }
        foregroundTrackingJob = trackingJob
        trackingJob.invokeOnCompletion { cause ->
            if (cause != null && cause !is kotlinx.coroutines.CancellationException) {
                Log.e("AppBlockerService", "Foreground evaluator stopped unexpectedly")
            }
            if (foregroundTrackingJob === trackingJob) foregroundTrackingJob = null
        }
    }

    private fun launchInterventionOverlay(
        packageName: String,
        monitoredUsageMinutes: Double,
        intervalMinutesAtLaunch: Double,
        ambientLightLuxAtLaunch: Float,
        deviation: Double,
        difficultyControlSignal: Double,
        difficulty: Int,
        challengeType: com.makhp.pelukdiri.core.domain.engine.InterventionChallengeType,
    ): Boolean {
        if (launchPolicy.consumeForcedFailure()) {
            return false
        }
        val intent = Intent(this, InterventionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or 
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra(InterventionActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(InterventionActivity.EXTRA_MONITORED_USAGE, monitoredUsageMinutes)
            putExtra(
                InterventionActivity.EXTRA_INTERVAL_MINUTES_AT_LAUNCH,
                intervalMinutesAtLaunch,
            )
            putExtra(
                InterventionActivity.EXTRA_AMBIENT_LIGHT_LUX_AT_LAUNCH,
                ambientLightLuxAtLaunch,
            )
            putExtra(InterventionActivity.EXTRA_DEVIATION, deviation)
            putExtra(InterventionActivity.EXTRA_DIFFICULTY_CONTROL_SIGNAL, difficultyControlSignal)
            putExtra(InterventionActivity.EXTRA_DIFFICULTY, difficulty)
            putExtra(InterventionActivity.EXTRA_CHALLENGE_TYPE, challengeType.name)
        }
        
        notificationHelper.showInterventionReminderNotification()

        return try {
            startActivity(intent)
            true
        } catch (_: Exception) {
            Log.e("AppBlockerService", "Intervention launch failed")
            false
        }
    }

    private fun restoreActiveIntervention() {
        val intent = Intent(this, InterventionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra(InterventionActivity.EXTRA_RESTORE_ACTIVE, true)
        }

        try {
            startActivity(intent)
        } catch (_: Exception) {
            Log.e("AppBlockerService", "Active intervention restore failed")
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        appUsageCollector.stopLightSensor()
        foregroundTrackingJob?.cancel()
        serviceScope.cancel()
    }
}
