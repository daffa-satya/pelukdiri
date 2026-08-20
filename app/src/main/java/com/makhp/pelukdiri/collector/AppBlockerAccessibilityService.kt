package com.makhp.pelukdiri.collector

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.makhp.pelukdiri.core.domain.repository.AdaptiveLimitRepository
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import com.makhp.pelukdiri.core.domain.repository.InterventionLogRepository
import com.makhp.pelukdiri.core.domain.repository.UsageRepository
import com.makhp.pelukdiri.core.domain.usecase.EvaluateInterventionEligibilityUseCase
import com.makhp.pelukdiri.features.intervention.InterventionActivity
import com.makhp.pelukdiri.features.intervention.ActiveInterventionSession
import com.makhp.pelukdiri.core.domain.InterventionLaunchPolicy
import com.makhp.pelukdiri.core.domain.time.TimeProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AppBlockerAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var appUsageCollector: AppUsageCollector

    @Inject
    lateinit var interventionLogRepository: InterventionLogRepository

    @Inject
    lateinit var adaptiveLimitRepository: AdaptiveLimitRepository

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var usageRepository: UsageRepository

    @Inject
    lateinit var evaluateInterventionEligibilityUseCase: EvaluateInterventionEligibilityUseCase

    @Inject
    lateinit var lockManager: com.makhp.pelukdiri.core.domain.InterventionLockManager

    @Inject lateinit var activeInterventionSession: ActiveInterventionSession
    @Inject lateinit var launchPolicy: InterventionLaunchPolicy
    @Inject lateinit var timeProvider: TimeProvider

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentMonitoredPackages = emptySet<String>()
    private var currentForegroundPackage: String? = null
    private var foregroundTrackingJob: Job? = null
    private val TICK_INTERVAL_MS = 5_000L
    private val EVALUATION_THROTTLE_MS = 30_000L
    private val SYNC_INTERVAL_MS = 30_000L
    private var lastSyncTimestamp: Long = 0L
    private var lastEvaluationTimestamp: Long = 0L

    private val systemApps = setOf(
        "com.makhp.pelukdiri",
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
        Log.d("AppBlockerService", "Service Created - Hilt Injection Success")
        
        serviceScope.launch {
            userPreferencesRepository.monitoredPackages.collect { packages ->
                currentMonitoredPackages = packages
                Log.d("AppBlockerService", "Updated Monitored Packages: $packages")

                // If the current app just became a target app, start tracking
                val foreground = currentForegroundPackage
                if (foreground != null && packages.contains(foreground) && foregroundTrackingJob == null) {
                    Log.d("AppBlockerService", "Current app $foreground is now monitored. Starting tracking.")
                    startForegroundTracking(foreground)
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("AppBlockerService", "Service Connected - Listening for events")
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
            
            // TODO: Implement time-based monitoring instead of relying solely on Window State Changes.
            // This would involve checking the active app duration periodically (e.g., every 60s)
            // even if the user hasn't switched windows, to handle cases where they stay 
            // in a target app for hours.
            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                Log.d(
                    "AppBlockerService",
                    ">>> Window Changed: event=$eventPackage active=$activeWindowPackage resolved=$packageName class=${event.className}"
                )
                handlePackageChanged(packageName)
            }
        } catch (e: Exception) {
            Log.e("AppBlockerService", "Error in onAccessibilityEvent", e)
        }
    }

    private suspend fun evaluateIntervention(packageName: String) {
        Log.d("AppBlockerService", ">>> evaluateIntervention called for $packageName")

        val savedSession = activeInterventionSession.restore()
        if (savedSession != null) {
            lockManager.acquireLock()
            restoreActiveIntervention()
            return
        }
        
        val decision = evaluateInterventionEligibilityUseCase(packageName)
        val controlResult = decision.controlResult

        if (decision.shouldTrigger && controlResult != null) {
            // Attempt to acquire lock before launching
            if (lockManager.acquireLock()) {
                val launchFreq = appUsageCollector.getLaunchCountForPackage(packageName).toDouble()

                val launched = launchInterventionOverlay(
                    packageName = packageName,
                    monitoredUsageMinutes = decision.monitoredUsageMinutes,
                    launchFreq = launchFreq,
                    ambientLux = decision.ambientLux,
                    deviation = controlResult.deviation ?: 0.0,
                    difficultyControlSignal = controlResult.normalizedDifficultyControl,
                    difficulty = controlResult.nextDifficulty
                )

                // Rollback lock if launch failed
                if (!launched) {
                    lockManager.releaseLock()
                } else {
                    userPreferencesRepository.setNextEligibleInterventionAt(controlResult.nextEligibleInterventionAt)
                    userPreferencesRepository.setCurrentDifficulty(controlResult.nextDifficulty)
                    Log.d("AppBlockerService", "Committed control result: Mode=${controlResult.mode}, Difficulty=${controlResult.nextDifficulty}, Interval=${controlResult.intervalMinutes}m")
                }
            } else {
                Log.d("AppBlockerService", "Skipping trigger: Intervention lock already held.")
            }
        }
    }

    private fun handlePackageChanged(newPackage: String) {
        if (newPackage == currentForegroundPackage) return
        
        Log.d("AppBlockerService", "Package switched from $currentForegroundPackage to $newPackage")

        // 1. Stop previous tracking
        foregroundTrackingJob?.cancel()
        foregroundTrackingJob = null

        // Reset throttle on package change to allow immediate check if it's a new monitored app
        lastEvaluationTimestamp = 0

        // 2. Sync usage for previous package if it was a target app
        val previous = currentForegroundPackage
        if (previous != null && currentMonitoredPackages.contains(previous)) {
            Log.d("AppBlockerService", "Exiting target app: $previous. Syncing data and stopping sensor.")
            serviceScope.launch { usageRepository.refreshUsageData() }
            appUsageCollector.stopLightSensor()
        }

        currentForegroundPackage = newPackage

        // 3. Start tracking if new package is a target app
        if (!systemApps.contains(newPackage) && currentMonitoredPackages.contains(newPackage)) {
            if (lockManager.isLocked.value) {
                Log.d("AppBlockerService", "Restoring unanswered intervention over: $newPackage")
                restoreActiveIntervention()
            } else {
                startForegroundTracking(newPackage)
            }
        } else {
            Log.d("AppBlockerService", "$newPackage is not a monitored target app.")
        }
    }

    private fun startForegroundTracking(packageName: String) {
        appUsageCollector.startLightSensor()
        foregroundTrackingJob = serviceScope.launch {
            Log.d("AppBlockerService", "Started periodic tracking for: $packageName")
            while (isActive) {
                val currentTime = timeProvider.nowMillis()
                
                // Periodically flush data to Room/Prefs (every 30s)
                if (currentTime - lastSyncTimestamp > SYNC_INTERVAL_MS) {
                    usageRepository.refreshUsageData()
                    lastSyncTimestamp = currentTime
                }

                // Throttle evaluation to 30s
                if (currentTime - lastEvaluationTimestamp >= EVALUATION_THROTTLE_MS) {
                    evaluateIntervention(packageName)
                    lastEvaluationTimestamp = currentTime
                }
                
                delay(TICK_INTERVAL_MS)
            }
        }
    }

    private fun launchInterventionOverlay(
        packageName: String,
        monitoredUsageMinutes: Double,
        launchFreq: Double,
        ambientLux: Float,
        deviation: Double,
        difficultyControlSignal: Double,
        difficulty: Int
    ): Boolean {
        Log.d("AppBlockerService", ">>> ATTEMPTING LAUNCH FOR: $packageName")
        if (launchPolicy.consumeForcedFailure()) {
            Log.w("AppBlockerService", ">>> Debug control forced launch failure")
            return false
        }
        val intent = Intent(this, InterventionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or 
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra(InterventionActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(InterventionActivity.EXTRA_MONITORED_USAGE, monitoredUsageMinutes)
            putExtra(InterventionActivity.EXTRA_LAUNCH_FREQ, launchFreq)
            putExtra(InterventionActivity.EXTRA_AMBIENT_LUX, ambientLux)
            putExtra(InterventionActivity.EXTRA_DEVIATION, deviation)
            putExtra(InterventionActivity.EXTRA_DIFFICULTY_CONTROL_SIGNAL, difficultyControlSignal)
            putExtra(InterventionActivity.EXTRA_DIFFICULTY, difficulty)
        }
        return try {
            startActivity(intent)
            Log.d("AppBlockerService", ">>> startActivity() called successfully")
            true
        } catch (e: Exception) {
            Log.e("AppBlockerService", ">>> FAILED to start activity", e)
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
            Log.d("AppBlockerService", "Restored the existing unanswered intervention")
        } catch (e: Exception) {
            Log.e("AppBlockerService", "Failed to restore active intervention", e)
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
