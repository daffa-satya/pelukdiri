package com.makhp.pelukdiri.collector

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.makhp.pelukdiri.core.domain.model.AppUsage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class UsageEventCollector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appUsageCollector: AppUsageCollector,
    private val reconstructor: UsageEventReconstructor,
    private val screenReconstructor: ScreenInteractiveReconstructor
) {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val ignoredPackages = setOf(
        "com.miui.home", "com.android.launcher", "com.google.android.apps.nexuslauncher",
        "com.android.settings", "com.android.systemui", "app.olauncher", "com.makhp.pelukdiri"
    )

    /**
     * Reconstructs usage for a specific local date.
     * Uses a context window before the day start to ensure session continuity.
     */
    fun getUsageForDay(date: LocalDate): List<AppUsage> {
        val day = reconstructDay(date)
        val usageMap = reconstructor.aggregateUsage(day.sessions, day.startMillis, day.endMillis)

        return usageMap.filter { it.key !in ignoredPackages && it.value.duration > 0 }
            .map { (pkg, stats) ->
                AppUsage(
                    packageName = pkg,
                    appName = appUsageCollector.getAppName(pkg),
                    usageDurationMillis = stats.duration,
                    lastUsedTimestamp = stats.lastTimestamp
                )
            }
    }

    fun getLongestSessionForDay(date: LocalDate): Long {
        val day = reconstructDay(date)
        return reconstructor.longestSessionDuration(
            day.sessions,
            day.startMillis,
            day.endMillis,
            ignoredPackages,
        )
    }

    fun getHourlyUsageForDay(date: LocalDate): List<Long> {
        val day = reconstructDay(date)
        return reconstructor.aggregateHourlyUsage(
            sessions = day.sessions,
            rangeStart = day.startMillis,
            rangeEnd = day.endMillis,
            zoneId = ZoneId.systemDefault(),
            ignoredPackages = ignoredPackages,
        )
    }

    private fun reconstructDay(date: LocalDate): ReconstructedDay {
        val zoneId = ZoneId.systemDefault()
        val dayStart = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val dayEnd = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        val queryEnd = min(dayEnd, now)

        // 1. Establish state at dayStart by looking back up to 24 hours
        val contextStart = dayStart - (24 * 60 * 60 * 1000)
        val contextEvents = fetchEvents(contextStart, dayStart)
        val initialState = findStateAtTimestamp(contextEvents)

        // 2. Query today's events
        val events = fetchEvents(dayStart, queryEnd)

        // 3. Reconstruct
        val sessions = reconstructor.reconstructSessions(
            events = events,
            queryEnd = queryEnd,
            initialPackage = initialState?.packageName,
            initialStartTime = initialState?.timestamp ?: dayStart
        )
        return ReconstructedDay(sessions, dayStart, queryEnd)
    }

    private data class ReconstructedDay(
        val sessions: List<UsageSession>,
        val startMillis: Long,
        val endMillis: Long,
    )

    fun getScreenOnMillisForDay(date: LocalDate): Long {
        val zoneId = ZoneId.systemDefault()
        val dayStart = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val dayEnd = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        val queryEnd = min(dayEnd, now)

        // 1. Establish screen state at dayStart by looking back up to 24 hours
        val contextStart = dayStart - (24 * 60 * 60 * 1000)
        val contextEvents = fetchEvents(contextStart, dayStart)
        val initialStartTime = findScreenStateAtTimestamp(contextEvents)

        // 2. Query today's events
        val events = fetchEvents(dayStart, queryEnd)
        
        return screenReconstructor.calculateTotalScreenOn(events, dayStart, queryEnd, initialStartTime)
    }

    private fun findScreenStateAtTimestamp(events: List<UsageEvent>): Long? {
        var isInteractive = false
        var interactiveStartTime: Long? = null

        val sorted = events.sortedBy { it.timestamp }
        for (event in sorted) {
            when (event.type) {
                ScreenInteractiveReconstructor.SCREEN_INTERACTIVE -> {
                    if (!isInteractive) {
                        isInteractive = true
                        interactiveStartTime = event.timestamp
                    }
                }
                ScreenInteractiveReconstructor.SCREEN_NON_INTERACTIVE -> {
                    isInteractive = false
                    interactiveStartTime = null
                }
            }
        }
        return if (isInteractive) interactiveStartTime else null
    }

    private fun findStateAtTimestamp(events: List<UsageEvent>): UsageEvent? {
        // Find the latest ACTIVITY_RESUMED
        val lastResumed = events.lastOrNull { it.type == UsageEventReconstructor.ACTIVITY_RESUMED } ?: return null
        
        // Find if there's any PAUSE or SCREEN_OFF after it
        val closer = events.lastOrNull { 
            (it.type == UsageEventReconstructor.ACTIVITY_PAUSED || it.type == UsageEventReconstructor.SCREEN_NON_INTERACTIVE) &&
            it.timestamp >= lastResumed.timestamp 
        }
        
        return if (closer == null) lastResumed else null
    }

    private fun fetchEvents(startTime: Long, endTime: Long): List<UsageEvent> {
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val result = mutableListOf<UsageEvent>()
        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            result.add(UsageEvent(
                packageName = event.packageName,
                timestamp = event.timeStamp,
                type = event.eventType
            ))
        }
        return result
    }
}
