package com.makhp.pelukdiri.collector

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.makhp.pelukdiri.core.domain.model.AppUsage
import com.makhp.pelukdiri.core.domain.time.TimeProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageEventCollector @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val appUsageCollector: AppUsageCollector,
    private val reconstructor: UsageEventReconstructor,
    private val screenReconstructor: ScreenInteractiveReconstructor,
    private val timeProvider: TimeProvider,
) {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    data class DailyUsageAndScreenOn(
        val usageList: List<AppUsage>,
        val screenOnMillis: Long
    )

    data class DailySessionMetrics(
        val appInsights: Map<String, AppUsageInsight>,
        val longestSessionMillis: Long,
        val hourlyUsage: List<Long>,
    )

    /**
     * Reconstructs usage and screen-on time for a specific local date in a single event pass.
     */
    fun getUsageAndScreenOnForDay(date: LocalDate): DailyUsageAndScreenOn {
        val day = reconstructDay(date)
        val usageMap = reconstructor.aggregateUsage(day.sessions, day.startMillis, day.endMillis)

        val usageList = usageMap.filter { it.value.duration > 0 }
            .map { (pkg, stats) ->
                AppUsage(
                    packageName = pkg,
                    appName = appUsageCollector.getAppName(pkg),
                    usageDurationMillis = stats.duration,
                    lastUsedTimestamp = stats.lastTimestamp
                )
            }

        val initialStartTime = findScreenStateAtTimestamp(day.contextEvents)
        val screenOnMillis = screenReconstructor.calculateTotalScreenOn(day.events, day.startMillis, day.endMillis, initialStartTime)

        return DailyUsageAndScreenOn(usageList, screenOnMillis)
    }

    /**
     * Reconstructs usage for a specific local date.
     * Uses a context window before the day start to ensure session continuity.
     */
    fun getUsageForDay(date: LocalDate): List<AppUsage> {
        return getUsageAndScreenOnForDay(date).usageList
    }

    /** Reuses one UsageEvents reconstruction for all session-based analytics. */
    fun getSessionMetricsForDay(date: LocalDate): DailySessionMetrics {
        val day = reconstructDay(date)
        return DailySessionMetrics(
            appInsights = reconstructor.appInsights(
                events = day.events,
                sessions = day.sessions,
                rangeStart = day.startMillis,
                rangeEnd = day.endMillis,
                interstitialPackages = setOf(context.packageName),
            ),
            longestSessionMillis = reconstructor.longestSessionDuration(
                sessions = day.sessions,
                rangeStart = day.startMillis,
                rangeEnd = day.endMillis,
            ),
            hourlyUsage = reconstructor.aggregateHourlyUsage(
                sessions = day.sessions,
                rangeStart = day.startMillis,
                rangeEnd = day.endMillis,
                zoneId = day.zoneId,
            ),
        )
    }

    /**
     * Returns per-package details reconstructed from the same event stream as daily usage.
     * A launch is a foreground-session start in the requested local day; repeated resumes from
     * the same package are coalesced. The peak session is clipped to that day.
     */
    fun getAppInsightsForDay(date: LocalDate): Map<String, AppUsageInsight> {
        val day = reconstructDay(date)
        return reconstructor.appInsights(
            events = day.events,
            sessions = day.sessions,
            rangeStart = day.startMillis,
            rangeEnd = day.endMillis,
            interstitialPackages = setOf(context.packageName),
        )
    }

    private fun reconstructDay(date: LocalDate): ReconstructedDay {
        val zoneId = timeProvider.zoneId()
        val bounds = usageDayBounds(date, zoneId)
        val queryEnd = bounds.queryEnd(timeProvider.nowMillis())

        // 1. Establish state at dayStart by looking back up to 24 hours
        val contextStart = bounds.startMillis - (24 * 60 * 60 * 1000)
        val contextEvents = fetchEvents(contextStart, bounds.startMillis)
        val initialState = findStateAtTimestamp(contextEvents)

        // 2. Query today's events
        val events = fetchEvents(bounds.startMillis, queryEnd)

        // 3. Reconstruct
        val sessions = reconstructor.reconstructSessions(
            events = events,
            queryEnd = queryEnd,
            initialPackage = initialState?.packageName,
            initialStartTime = initialState?.timestamp ?: bounds.startMillis
        )
        return ReconstructedDay(
            sessions = sessions,
            events = events,
            contextEvents = contextEvents,
            startMillis = bounds.startMillis,
            endMillis = queryEnd,
            zoneId = zoneId,
        )
    }

    private data class ReconstructedDay(
        val sessions: List<UsageSession>,
        val events: List<UsageEvent>,
        val contextEvents: List<UsageEvent>,
        val startMillis: Long,
        val endMillis: Long,
        val zoneId: ZoneId,
    )

    fun getScreenOnMillisForDay(date: LocalDate): Long {
        val day = reconstructDay(date)
        val initialStartTime = findScreenStateAtTimestamp(day.contextEvents)
        return screenReconstructor.calculateTotalScreenOn(day.events, day.startMillis, day.endMillis, initialStartTime)
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
            (it.type == UsageEventReconstructor.ACTIVITY_PAUSED ||
                it.type == UsageEventReconstructor.ACTIVITY_STOPPED ||
                it.type == UsageEventReconstructor.SCREEN_NON_INTERACTIVE) &&
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
                type = event.eventType,
                className = event.className,
            ))
        }
        return result
    }
}
