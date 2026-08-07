package com.makhp.pelukdiri.collector

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.makhp.pelukdiri.core.domain.repository.AdaptiveLimitRepository
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import com.makhp.pelukdiri.core.domain.usecase.CalculateRiskScoreUseCase
import com.makhp.pelukdiri.features.intervention.InterventionActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class AppBlockerAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var appUsageCollector: AppUsageCollector

    @Inject
    lateinit var calculateRiskScoreUseCase: CalculateRiskScoreUseCase

    @Inject
    lateinit var adaptiveLimitRepository: AdaptiveLimitRepository

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastInterventionTimestamp: Long = 0L
    private val COOLDOWN_MS = 3_000L // Reduced to 3 seconds for testing

    private var currentMonitoredPackages = emptySet<String>()

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
            userPreferencesRepository.monitoredPackages.collect {
                currentMonitoredPackages = it
                Log.d("AppBlockerService", "Updated Monitored Packages: $it")
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
                Log.d("AppBlockerService", ">>> Window Changed: $packageName")
                
                // 1. Ignore system noise
                if (systemApps.contains(packageName)) return
                
                // 2. ONLY evaluate target apps (Instagram/YouTube/etc)
                if (currentMonitoredPackages.contains(packageName)) {
                    evaluateIntervention(packageName)
                }
            }
        } catch (e: Exception) {
            Log.e("AppBlockerService", "Error in onAccessibilityEvent", e)
        }
    }

    private fun evaluateIntervention(packageName: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastInterventionTimestamp < COOLDOWN_MS) {
            Log.d("AppBlockerService", "Cooldown active for: $packageName")
            return
        }
        
        serviceScope.launch {
            // Check for emergency bypass
            val bypassUntil = userPreferencesRepository.emergencyBypassUntil.first()
            if (currentTime < bypassUntil) {
                Log.d("AppBlockerService", "Emergency bypass active until $bypassUntil")
                return@launch
            }

            // Update timestamp immediately before async launch to prevent race conditions
            lastInterventionTimestamp = currentTime

            val todayStr = LocalDate.now().toString()
            val adaptiveLimit = adaptiveLimitRepository.getLimitForDate(todayStr)
            
            // Fix: Fetch real-time daily usage directly from system instead of relying on Room history
            val totalScreenTimeMinutes = appUsageCollector.getTodayScreenTimeMinutes()
            val currentLimitMinutes = adaptiveLimit?.calculatedLimitMinutes ?: 60 // fallback to 60

            val launchFreq = appUsageCollector.getLaunchCountForPackage(packageName).toDouble()
            val ambientLux = appUsageCollector.getCurrentAmbientLightLux()

            val riskResult = calculateRiskScoreUseCase(
                screenTimeMinutes = totalScreenTimeMinutes,
                launchFrequency = launchFreq,
                ambientLightLux = ambientLux,
                baselineLimitMinutes = currentLimitMinutes.toDouble()
            )

            Log.d("AppBlockerService", "Evaluation for $packageName:")
            Log.d("AppBlockerService", " - ScreenTime: %.2f min / Limit: %d min".format(totalScreenTimeMinutes, currentLimitMinutes))
            Log.d("AppBlockerService", " - Risk Level: %d / LaunchFreq: %.0f".format(riskResult.level, launchFreq))
            Log.d("AppBlockerService", " - AmbientLux: %.2f".format(ambientLux))

            // Test-friendly condition: trigger if limit reached OR risk level is significant (>=1 for testing)
            if (totalScreenTimeMinutes >= currentLimitMinutes || riskResult.level >= 1) {
                launchInterventionOverlay(
                    packageName = packageName,
                    screenTimeMinutes = totalScreenTimeMinutes,
                    launchFreq = launchFreq,
                    ambientLux = ambientLux,
                    baselineLimit = currentLimitMinutes.toDouble()
                )
            } else {
                Log.d("AppBlockerService", "Condition not met for intervention")
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
        // Cancel scope if needed, though AccessibilityService lifecycle is managed by system
    }
}
