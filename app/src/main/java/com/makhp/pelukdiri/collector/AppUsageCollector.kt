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
import com.makhp.pelukdiri.core.domain.model.DailyUsageSummary
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUsageCollector @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorEventListener {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private var currentAmbientLux: Float = 0f

    init {
        // Inisialisasi Sensor Light (L)
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    // --- SENSOR LIGHT LISTENER ---
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            currentAmbientLux = event.values[0]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun getCurrentAmbientLightLux(): Float = currentAmbientLux

    // --- PER-PACKAGE QUERY UNTUK USAGESYNCWORKER ---

    /**
     * Mengambil durasi screen time (ms) untuk package spesifik sejak awal hari ini (00:00).
     */
    fun getScreenTimeForPackage(packageName: String): Long {
        if (!isPermissionGranted()) return 0L

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        return stats.find { it.packageName == packageName }?.totalTimeInForeground ?: 0L
    }

    /**
     * Menghitung frekuensi buka aplikasi (F) berdasarkan event ACTIVITY_RESUMED sejak awal hari ini.
     */
    fun getLaunchCountForPackage(packageName: String): Int {
        if (!isPermissionGranted()) return 0

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var openCount = 0

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.packageName == packageName && event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                openCount++
            }
        }

        return openCount
    }

    // --- EXISTING METHODS (TETAP SAMA) ---

    fun isPermissionGranted(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getPast7DaysScreenTimeHistory(): List<DailyUsageSummary> {
        if (!isPermissionGranted()) return emptyList()

        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startTime = calendar.timeInMillis

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        if (stats.isNullOrEmpty()) return emptyList()

        return stats.groupBy { getFormattedDate(it.firstTimeStamp) }
            .map { (date, dailyStats) ->
                DailyUsageSummary(
                    date = date,
                    totalScreenTimeMs = dailyStats.sumOf { it.totalTimeInForeground }
                )
            }
            .sortedByDescending { it.date }
    }

    private fun getFormattedDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun fetchRecentEvents(startTime: Long, endTime: Long): List<AppUsage> {
        if (!isPermissionGranted()) return emptyList()

        // queryAndAggregateUsageStats is more reliable for current-day cumulative stats
        val statsMap = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
        
        if (statsMap.isNullOrEmpty()) return emptyList()

        // List of common launchers and system packages to ignore to reduce DB noise
        val ignoredPackages = listOf(
            "com.miui.home", "com.android.launcher", "com.google.android.apps.nexuslauncher",
            "com.android.settings", "com.android.systemui", "app.olauncher", "com.makhp.pelukdiri"
        )

        return statsMap.values
            .filter { it.totalTimeInForeground > 0 && it.packageName !in ignoredPackages }
            .map { usageStats ->
                AppUsage(
                    packageName = usageStats.packageName,
                    appName = getAppName(usageStats.packageName),
                    usageDurationMillis = usageStats.totalTimeInForeground,
                    lastUsedTimestamp = usageStats.lastTimeUsed
                )
            }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    fun fetchRecentEventsPlainText(hoursAgo: Int): String {
        if (!isPermissionGranted()) return "Permission is required to view usage statistics."

        val endTime = System.currentTimeMillis()
        val startTime = endTime - (1000L * 60 * 60 * hoursAgo)

        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()

        val stringBuilder = StringBuilder()
        val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        var eventCount = 0

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            val appPackage = event.packageName
            val humanTime = timeFormatter.format(Date(event.timeStamp))

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    stringBuilder.append("[$humanTime] OPENED: $appPackage\n")
                    eventCount++
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    stringBuilder.append("[$humanTime] CLOSED: $appPackage\n")
                    eventCount++
                }
            }
        }

        return if (eventCount == 0) "No application events recorded." else stringBuilder.toString()
    }
}