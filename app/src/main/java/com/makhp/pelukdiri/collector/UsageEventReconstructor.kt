package com.makhp.pelukdiri.collector

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

data class UsageSession(
    val packageName: String,
    val startTime: Long,
    val endTime: Long,
)

data class UsageEvent(
    val packageName: String,
    val timestamp: Long,
    val type: Int,
    val className: String? = null,
)

@Singleton
class UsageEventReconstructor @Inject constructor() {

    companion object {
        const val ACTIVITY_RESUMED = 1
        const val ACTIVITY_PAUSED = 2
        const val ACTIVITY_STOPPED = 23
        const val SCREEN_NON_INTERACTIVE = 16
        private const val TRANSIENT_GAP_THRESHOLD_MS = 500L
    }

    fun reconstructSessions(
        events: List<UsageEvent>,
        queryEnd: Long,
        initialPackage: String? = null,
        initialStartTime: Long = -1L
    ): List<UsageSession> {
        val sortedEvents = events.sortedBy { it.timestamp }
        val sessions = mutableListOf<UsageSession>()
        
        var currentPackage: String? = initialPackage
        var sessionStartTime: Long = if (initialPackage != null) initialStartTime else -1L

        for (i in sortedEvents.indices) {
            val event = sortedEvents[i]
            
            when (event.type) {
                ACTIVITY_RESUMED -> {
                    if (currentPackage != null && currentPackage != event.packageName) {
                        // Close previous session
                        sessions.add(UsageSession(currentPackage, sessionStartTime, event.timestamp))
                        currentPackage = event.packageName
                        sessionStartTime = event.timestamp
                    } else if (currentPackage == null) {
                        currentPackage = event.packageName
                        sessionStartTime = event.timestamp
                    }
                }
                ACTIVITY_PAUSED -> {
                    if (currentPackage == event.packageName) {
                        // Peek at next event
                        var isTransient = false
                        if (i + 1 < sortedEvents.size) {
                            val nextEvent = sortedEvents[i + 1]
                            if (nextEvent.type == ACTIVITY_RESUMED &&
                                nextEvent.packageName == event.packageName &&
                                (nextEvent.timestamp - event.timestamp) < TRANSIENT_GAP_THRESHOLD_MS) {
                                isTransient = true
                            }
                        }
                        
                        if (!isTransient) {
                            sessions.add(UsageSession(currentPackage!!, sessionStartTime, event.timestamp))
                            currentPackage = null
                            sessionStartTime = -1L
                        }
                    }
                }
                // ACTIVITY_STOPPED belongs to one Activity. A newer Activity from the same
                // package may already be resumed, so it must not close the package session.
                ACTIVITY_STOPPED -> Unit
                SCREEN_NON_INTERACTIVE -> {
                    if (currentPackage != null) {
                        sessions.add(UsageSession(currentPackage!!, sessionStartTime, event.timestamp))
                        currentPackage = null
                        sessionStartTime = -1L
                    }
                }
            }
        }

        // Close last active session at queryEnd
        currentPackage?.let {
            sessions.add(UsageSession(it, sessionStartTime, queryEnd))
        }

        return sessions
    }

    fun aggregateUsage(
        sessions: List<UsageSession>,
        rangeStart: Long,
        rangeEnd: Long
    ): Map<String, PackageUsageStats> {
        val result = mutableMapOf<String, PackageUsageStats>()
        
        for (session in sessions) {
            val actualStart = max(session.startTime, rangeStart)
            val actualEnd = min(session.endTime, rangeEnd)
            
            if (actualStart < actualEnd) {
                val duration = actualEnd - actualStart
                val current = result.getOrPut(session.packageName) { PackageUsageStats(0L, 0L) }
                result[session.packageName] = PackageUsageStats(
                    duration = current.duration + duration,
                    lastTimestamp = max(current.lastTimestamp, actualEnd)
                )
            }
        }
        
        return result
    }

    fun longestSessionDuration(
        sessions: List<UsageSession>,
        rangeStart: Long,
        rangeEnd: Long,
    ): Long = sessions.asSequence()
        .maxOfOrNull { session ->
            (min(session.endTime, rangeEnd) - max(session.startTime, rangeStart)).coerceAtLeast(0L)
        } ?: 0L

    fun appInsights(
        events: List<UsageEvent>,
        sessions: List<UsageSession>,
        rangeStart: Long,
        rangeEnd: Long,
        interstitialPackages: Set<String> = emptySet(),
    ): Map<String, AppUsageInsight> {
        val launches = countForegroundStarts(events, rangeStart, rangeEnd, interstitialPackages)

        return (launches.keys + sessions.map { it.packageName }).associateWith { packageName ->
            val peakSequence = longestUsageSequence(
                sessions = sessions,
                packageName = packageName,
                rangeStart = rangeStart,
                rangeEnd = rangeEnd,
                interstitialPackages = interstitialPackages,
            )
            AppUsageInsight(
                launchCount = launches[packageName] ?: 0,
                longestSessionStartMillis = peakSequence?.startTime,
                longestSessionEndMillis = peakSequence?.endTime,
                longestSequenceDurationMillis = peakSequence?.durationMillis,
            )
        }
    }

    private fun longestUsageSequence(
        sessions: List<UsageSession>,
        packageName: String,
        rangeStart: Long,
        rangeEnd: Long,
        interstitialPackages: Set<String>,
    ): UsageSequence? {
        var current: UsageSequence? = null
        var longest: UsageSequence? = null
        var onlyInterventionBetweenTargets = false

        fun finish() {
            val sequence = current ?: return
            if (longest == null || sequence.durationMillis > longest!!.durationMillis) longest = sequence
            current = null
        }

        sessions.sortedBy { it.startTime }.forEach { session ->
            if (session.packageName == packageName) {
                val start = max(session.startTime, rangeStart)
                val end = min(session.endTime, rangeEnd)
                if (start < end) {
                    val existing = current
                    if (existing != null && !onlyInterventionBetweenTargets) finish()
                    current = if (current == null) {
                        UsageSequence(start, end, end - start)
                    } else {
                        current!!.copy(
                            endTime = max(current!!.endTime, end),
                            durationMillis = current!!.durationMillis + (end - start),
                        )
                    }
                    onlyInterventionBetweenTargets = false
                }
            } else {
                if (session.packageName in interstitialPackages && current != null) {
                    onlyInterventionBetweenTargets = true
                } else {
                    finish()
                    onlyInterventionBetweenTargets = false
                }
            }
        }
        finish()
        return longest
    }

    private data class UsageSequence(
        val startTime: Long,
        val endTime: Long,
        val durationMillis: Long,
    )

    /**
     * Counts app openings as foreground-session starts, not raw ACTIVITY_RESUMED events.
     * A package can emit several resumes while its own Activities are being replaced or
     * restored; those are one opening until the package actually leaves the foreground.
     */
    fun countForegroundStarts(
        events: List<UsageEvent>,
        rangeStart: Long,
        rangeEnd: Long,
        interstitialPackages: Set<String> = emptySet(),
    ): Map<String, Int> {
        val sortedEvents = events.sortedBy { it.timestamp }
        val counts = mutableMapOf<String, Int>()
        val activeActivities = mutableSetOf<String>()
        var transientPackage: String? = null
        var transientUntil = Long.MIN_VALUE
        var screenOffPackages = emptySet<String>()
        var lastForegroundPackage: String? = null
        var suspendedByInterstitialPackage: String? = null

        for (index in sortedEvents.indices) {
            val event = sortedEvents[index]
            when (event.type) {
                ACTIVITY_RESUMED -> {
                    val activityKey = activityKey(event)
                    val packageWasForeground = activeActivities.any {
                        it.substringBefore('\u0000') == event.packageName
                    } || (transientPackage == event.packageName && event.timestamp < transientUntil)
                    val wasScreenOffInSamePackage = event.packageName in screenOffPackages &&
                        event.packageName in interstitialPackages
                    val isInterstitial = event.packageName in interstitialPackages &&
                        event.className?.contains("InterventionActivity") == true
                    val isReturnFromInterstitial = suspendedByInterstitialPackage == event.packageName
                    if (!packageWasForeground && !wasScreenOffInSamePackage &&
                        !(isInterstitial && lastForegroundPackage != null) &&
                        !isReturnFromInterstitial
                    ) {
                        if (event.timestamp in rangeStart until rangeEnd) {
                            counts[event.packageName] = (counts[event.packageName] ?: 0) + 1
                        }
                    }
                    if (isInterstitial && !packageWasForeground && lastForegroundPackage != null) {
                        suspendedByInterstitialPackage = lastForegroundPackage
                    } else if (!isInterstitial && isReturnFromInterstitial) {
                        suspendedByInterstitialPackage = null
                    }
                    // UsageStats may omit the old package's pause on OEM builds. A resumed
                    // top-level Activity still establishes the foreground package boundary;
                    // retain other Activities only when they belong to this same package.
                    activeActivities.removeAll {
                        it.substringBefore('\u0000') != event.packageName
                    }
                    activeActivities += activityKey
                    if (!isInterstitial) lastForegroundPackage = event.packageName
                    transientPackage = null
                    if (event.packageName !in screenOffPackages) screenOffPackages = emptySet()
                }
                ACTIVITY_PAUSED, ACTIVITY_STOPPED -> {
                    val activityKey = activityKey(event)
                    if (event.type == ACTIVITY_PAUSED) {
                        val next = sortedEvents.getOrNull(index + 1)
                        val isTransient = next?.type == ACTIVITY_RESUMED &&
                            next.packageName == event.packageName &&
                            next.timestamp - event.timestamp < TRANSIENT_GAP_THRESHOLD_MS
                        if (isTransient) {
                            activeActivities -= activityKey
                        } else {
                            activeActivities.removeAll {
                                it.substringBefore('\u0000') == event.packageName
                            }
                        }
                        if (isTransient) {
                            transientPackage = event.packageName
                            transientUntil = event.timestamp + TRANSIENT_GAP_THRESHOLD_MS
                        }
                    } else {
                        activeActivities -= activityKey
                        if (transientPackage == event.packageName) transientPackage = null
                    }
                }
                SCREEN_NON_INTERACTIVE -> {
                    screenOffPackages = activeActivities
                        .map { it.substringBefore('\u0000') }
                        .toSet() + listOfNotNull(lastForegroundPackage)
                    activeActivities.clear()
                    transientPackage = null
                }
            }
        }
        return counts
    }

    private fun activityKey(event: UsageEvent): String =
        event.packageName + '\u0000' + (event.className ?: "<unknown>")

    fun aggregateHourlyUsage(
        sessions: List<UsageSession>,
        rangeStart: Long,
        rangeEnd: Long,
        zoneId: ZoneId,
    ): List<Long> {
        val hourlyUsage = LongArray(24)
        sessions.asSequence()
            .forEach { session ->
                var cursor = max(session.startTime, rangeStart)
                val sessionEnd = min(session.endTime, rangeEnd)
                while (cursor < sessionEnd) {
                    val currentHour = Instant.ofEpochMilli(cursor).atZone(zoneId)
                    val nextHour = currentHour.truncatedTo(ChronoUnit.HOURS).plusHours(1)
                        .toInstant().toEpochMilli()
                    val segmentEnd = min(sessionEnd, nextHour)
                    hourlyUsage[currentHour.hour] += segmentEnd - cursor
                    cursor = segmentEnd
                }
            }
        return hourlyUsage.toList()
    }

    data class PackageUsageStats(val duration: Long, val lastTimestamp: Long)
}
