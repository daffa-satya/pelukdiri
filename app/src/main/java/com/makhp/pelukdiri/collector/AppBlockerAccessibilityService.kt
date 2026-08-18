package com.makhp.pelukdiri.collector

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.makhp.pelukdiri.core.domain.repository.AdaptiveLimitRepository
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import com.makhp.pelukdiri.core.domain.engine.ControlEngine
import com.makhp.pelukdiri.core.domain.engine.DeviationEngine
import com.makhp.pelukdiri.core.domain.model.PerformanceMetrics
import com.makhp.pelukdiri.core.domain.repository.InterventionLogRepository
import com.makhp.pelukdiri.core.domain.repository.UsageRepository
import com.makhp.pelukdiri.core.domain.usecase.GetAdaptiveHistoryUseCase
import com.makhp.pelukdiri.features.intervention.InterventionActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@AndroidEntryPoint
class AppBlockerAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var appUsageCollector: AppUsageCollector

    @Inject
    lateinit var controlEngine: ControlEngine

    @Inject
    lateinit var deviationEngine: DeviationEngine

    @Inject
    lateinit var interventionLogRepository: InterventionLogRepository

    @Inject
    lateinit var adaptiveLimitRepository: AdaptiveLimitRepository

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var usageRepository: UsageRepository

    @Inject
    lateinit var getAdaptiveHistoryUseCase: GetAdaptiveHistoryUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastInterventionTimestamp: Long = 0L
    private val COOLDOWN_MS = 3_000L 

    private var currentMonitoredPackages = emptySet<String>()
    private var currentForegroundPackage: String? = null
    private var foregroundTrackingJob: Job? = null
    private val TICK_INTERVAL_MS = 5_000L
    private val SYNC_INTERVAL_MS = 30_000L
    private var lastSyncTimestamp: Long = 0L

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
            val packageName = event?.packageName?.toString() ?: return
            
            // TODO: Implement time-based monitoring instead of relying solely on Window State Changes.
            // This would involve checking the active app duration periodically (e.g., every 60s)
            // even if the user hasn't switched windows, to handle cases where they stay 
            // in a target app for hours.
            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                Log.d("AppBlockerService", ">>> Window Changed: $packageName (Event: $eventType)")
                handlePackageChanged(packageName)
            }
        } catch (e: Exception) {
            Log.e("AppBlockerService", "Error in onAccessibilityEvent", e)
        }
    }

    private suspend fun evaluateIntervention(packageName: String, sessionExtraMs: Long = 0L) {
        val currentTimeMs = System.currentTimeMillis()
        Log.d("AppBlockerService", ">>> evaluateIntervention called for $packageName")
        
        // Check next eligible time from Control Engine
        val nextEligible = userPreferencesRepository.nextEligibleInterventionAt.first()
        Log.d("AppBlockerService", ">>> nextEligible: $nextEligible, current: $currentTimeMs")
        
        // One-time bypass for clean validation: allow if this is the first evaluation in this service session
        val isFirstEvalInSession = lastInterventionTimestamp == 0L
        
        if (currentTimeMs < nextEligible && !isFirstEvalInSession) {
            Log.d("AppBlockerService", ">>> Too early, skipping")
            return
        }

        if (currentTimeMs - lastInterventionTimestamp < COOLDOWN_MS) {
            Log.d("AppBlockerService", "Cooldown active for: $packageName")
            return
        }
        
        // Check for emergency bypass
        val bypassUntil = userPreferencesRepository.emergencyBypassUntil.first()
        Log.d("AppBlockerService", ">>> bypassUntil: $bypassUntil")
        if (currentTimeMs < bypassUntil) {
            Log.d("AppBlockerService", "Emergency bypass active until $bypassUntil")
            return
        }

        // 1. Get Deviation Signal
        Log.d("AppBlockerService", ">>> Getting Deviation")
        val usageHistory = getAdaptiveHistoryUseCase()
        val sessionExtraMinutes = sessionExtraMs / 1000.0 / 60.0
        val currentUsage = appUsageCollector.getTodayScreenTimeMinutes() + sessionExtraMinutes
        val devResult = deviationEngine.calculate(currentUsage, usageHistory)
        val deviation = devResult.deviation
        Log.d("AppBlockerService", ">>> Deviation: $deviation")

        // 2. Get Performance Context
        Log.d("AppBlockerService", ">>> Getting Performance")
        val currentDiff = userPreferencesRepository.currentDifficulty.first()
        val latestLog = interventionLogRepository.getLatestLog()
        val lastPerformance = latestLog?.let { 
            PerformanceMetrics(
                responseTimeMs = it.responseTimeMs, 
                isSuccess = it.isSuccess, 
                difficulty = it.difficultyLevel
            ) 
        }
        val perfHistory = interventionLogRepository.getRecentValidSuccessfulLogsByDifficulty(currentDiff, 5)
            .map { it.responseTimeMs }

        // 3. Get Sensitivity Context
        Log.d("AppBlockerService", ">>> Getting Sensitivity")
        val lux = appUsageCollector.getCurrentAmbientLightLux()
        val sleepTime = parseLocalTime(userPreferencesRepository.bedtime.first())
        val wakeTime = parseLocalTime(userPreferencesRepository.wakeTime.first())

        // 4. Execute Control Engine
        Log.d("AppBlockerService", ">>> Executing Control Engine")
        val controlResult = controlEngine.calculateNextIntervention(
            deviation = deviation,
            lastPerformance = lastPerformance,
            performanceHistory = perfHistory,
            lux = lux,
            bedtime = sleepTime,
            wakeTime = wakeTime,
            currentLevel = currentDiff,
            timestampMs = currentTimeMs
        )

        Log.d("AppBlockerService", "Control Engine Result: Mode=${controlResult.mode}, TargetDiff=${controlResult.nextDifficulty}, Interval=${controlResult.intervalMinutes}m")

        // Update state
        userPreferencesRepository.setNextEligibleInterventionAt(controlResult.nextEligibleInterventionAt)
        userPreferencesRepository.setCurrentDifficulty(controlResult.nextDifficulty)

        // Trigger condition: Significant deviation (D > 0.05)
        // Insufficient history (deviation == null) suppresses the trigger
        val shouldTrigger = deviation != null && deviation > 0.05

        if (shouldTrigger) {
            lastInterventionTimestamp = currentTimeMs
            val launchFreq = appUsageCollector.getLaunchCountForPackage(packageName).toDouble()
            
            launchInterventionOverlay(
                packageName = packageName,
                screenTimeMinutes = currentUsage,
                launchFreq = launchFreq,
                ambientLux = lux,
                baselineLimit = 0.0 // Deprecated in v0.1 engine logic
            )
        }
    }

    private fun parseLocalTime(timeStr: String?): LocalTime? {
        return try {
            timeStr?.let { LocalTime.parse(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun handlePackageChanged(newPackage: String) {
        if (newPackage == currentForegroundPackage) return
        
        Log.d("AppBlockerService", "Package switched from $currentForegroundPackage to $newPackage")

        // 1. Stop previous tracking
        foregroundTrackingJob?.cancel()
        foregroundTrackingJob = null

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
            startForegroundTracking(newPackage)
        } else {
            Log.d("AppBlockerService", "$newPackage is not a monitored target app.")
        }
    }

    private fun startForegroundTracking(packageName: String) {
        appUsageCollector.startLightSensor()
        foregroundTrackingJob = serviceScope.launch {
            Log.d("AppBlockerService", "Started periodic tracking for: $packageName")
            val sessionStartTime = System.currentTimeMillis()
            
            while (isActive) {
                val currentTime = System.currentTimeMillis()
                val sessionDurationMs = currentTime - sessionStartTime
                
                // Periodically flush data to Room/Prefs (every 30s)
                if (currentTime - lastSyncTimestamp > SYNC_INTERVAL_MS) {
                    usageRepository.refreshUsageData()
                    lastSyncTimestamp = currentTime
                }

                // Sequential evaluation (no new coroutine launched here)
                evaluateIntervention(packageName, sessionDurationMs)
                
                delay(TICK_INTERVAL_MS)
            }
        }
    }

    private fun launchInterventionOverlay(
        packageName: String,
        screenTimeMinutes: Double,
        launchFreq: Double,
        ambientLux: Float,
        baselineLimit: Double
    ) {
        Log.d("AppBlockerService", ">>> ATTEMPTING LAUNCH FOR: $packageName")
        val intent = Intent(this, InterventionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or 
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra("EXTRA_PACKAGE_NAME", packageName)
            putExtra("EXTRA_SCREEN_TIME", screenTimeMinutes)
            putExtra("EXTRA_LAUNCH_FREQ", launchFreq)
            putExtra("EXTRA_AMBIENT_LUX", ambientLux)
            putExtra("EXTRA_BASELINE_LIMIT", baselineLimit)
        }
        try {
            startActivity(intent)
            Log.d("AppBlockerService", ">>> startActivity() called successfully")
        } catch (e: Exception) {
            Log.e("AppBlockerService", ">>> FAILED to start activity", e)
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
