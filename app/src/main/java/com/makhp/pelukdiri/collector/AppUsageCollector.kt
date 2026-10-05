package com.makhp.pelukdiri.collector

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Process
import com.makhp.pelukdiri.core.domain.model.AppUsage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUsageCollector @Inject constructor(
    @param:ApplicationContext private val context: Context
) : SensorEventListener {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private var currentAmbientLux: Float = 0f
    private var isSensorRegistered = false

    fun startLightSensor() {
        if (isSensorRegistered) return
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            isSensorRegistered = true
        }
    }

    fun stopLightSensor() {
        if (!isSensorRegistered) return
        sensorManager.unregisterListener(this)
        isSensorRegistered = false
    }

    // --- SENSOR LIGHT LISTENER ---
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            currentAmbientLux = event.values[0]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun getCurrentAmbientLightLux(): Float = currentAmbientLux

    /**
     * Counts foreground-session starts for all packages in a single event pass.
     * Avoids redundant queryEvents calls when retrieving counts for multiple packages.
     */
    fun getLaunchCountsForAllPackages(startTime: Long, endTime: Long): Map<String, Int> {
        if (!isPermissionGranted()) return emptyMap()

        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        val events = mutableListOf<UsageEvent>()

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            events += UsageEvent(event.packageName, event.timeStamp, event.eventType, event.className)
        }

        return UsageEventReconstructor().countForegroundStarts(
            events = events,
            rangeStart = startTime,
            rangeEnd = endTime,
            interstitialPackages = setOf(context.packageName),
        )
    }

    fun isPermissionGranted(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun fetchRecentEvents(startTime: Long, endTime: Long): List<AppUsage> {
        if (!isPermissionGranted()) return emptyList()

        // queryAndAggregateUsageStats is more reliable for current-day cumulative stats
        val statsMap = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
        
        if (statsMap.isNullOrEmpty()) return emptyList()

        return statsMap.values
            .filter { it.totalTimeInForeground > 0 }
            .map { usageStats ->
                AppUsage(
                    packageName = usageStats.packageName,
                    appName = getAppName(usageStats.packageName),
                    usageDurationMillis = usageStats.totalTimeInForeground,
                    lastUsedTimestamp = usageStats.lastTimeUsed
                )
            }
    }

    fun getAppName(packageName: String): String {
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName
        }
    }

}
