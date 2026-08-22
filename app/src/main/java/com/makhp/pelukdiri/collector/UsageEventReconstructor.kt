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
    val type: Int
)

@Singleton
class UsageEventReconstructor @Inject constructor() {

    companion object {
        const val ACTIVITY_RESUMED = 1
        const val ACTIVITY_PAUSED = 2
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
        ignoredPackages: Set<String> = emptySet(),
    ): Long = sessions.asSequence()
        .filter { it.packageName !in ignoredPackages }
        .maxOfOrNull { session ->
            (min(session.endTime, rangeEnd) - max(session.startTime, rangeStart)).coerceAtLeast(0L)
        } ?: 0L

    fun aggregateHourlyUsage(
        sessions: List<UsageSession>,
        rangeStart: Long,
        rangeEnd: Long,
        zoneId: ZoneId,
        ignoredPackages: Set<String> = emptySet(),
    ): List<Long> {
        val hourlyUsage = LongArray(24)
        sessions.asSequence()
            .filter { it.packageName !in ignoredPackages }
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
