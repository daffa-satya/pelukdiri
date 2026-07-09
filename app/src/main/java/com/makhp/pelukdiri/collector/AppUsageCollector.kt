package com.makhp.pelukdiri.collector

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import com.makhp.pelukdiri.core.domain.model.AppUsage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class AppUsageCollector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    // 1. Check permission logic belongs here
    fun isPermissionGranted(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    // 2. Data formatting logic belongs here
    fun fetchRecentEvents(startTime: Long, endTime: Long): List<AppUsage> {
        if (!isPermissionGranted()) return emptyList()

        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        if (stats.isNullOrEmpty()) return emptyList()

        return stats.filter { it.totalTimeInForeground > 0 }.map { usageStats ->
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