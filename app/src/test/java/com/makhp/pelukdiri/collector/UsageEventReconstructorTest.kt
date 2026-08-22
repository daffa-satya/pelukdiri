package com.makhp.pelukdiri.collector

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.ZoneId

class UsageEventReconstructorTest {

    private lateinit var reconstructor: UsageEventReconstructor
    private val pkgA = "com.example.appA"
    private val pkgB = "com.example.appB"

    @Before
    fun setup() {
        reconstructor = UsageEventReconstructor()
    }

    @Test
    fun `test single session`() {
        val events = listOf(
            UsageEvent(pkgA, 1000L, UsageEventReconstructor.ACTIVITY_RESUMED),
            UsageEvent(pkgA, 2000L, UsageEventReconstructor.ACTIVITY_PAUSED),
        )
        val sessions = reconstructor.reconstructSessions(events, 3000L)
        
        assertEquals(1, sessions.size)
        assertEquals(pkgA, sessions[0].packageName)
        assertEquals(1000L, sessions[0].startTime)
        assertEquals(2000L, sessions[0].endTime)
    }

    @Test
    fun `test multiple sessions`() {
        val events = listOf(
            UsageEvent(pkgA, 1000L, UsageEventReconstructor.ACTIVITY_RESUMED),
            UsageEvent(pkgA, 2000L, UsageEventReconstructor.ACTIVITY_PAUSED),
            UsageEvent(pkgB, 3000L, UsageEventReconstructor.ACTIVITY_RESUMED),
            UsageEvent(pkgB, 4000L, UsageEventReconstructor.ACTIVITY_PAUSED)
        )
        val sessions = reconstructor.reconstructSessions(events, 5000L)
        
        assertEquals(2, sessions.size)
        assertEquals(pkgA, sessions[0].packageName)
        assertEquals(pkgB, sessions[1].packageName)
    }

    @Test
    fun `test transient gap inside same package`() {
        // Gap is 400ms (< 500ms)
        val events = listOf(
            UsageEvent(pkgA, 1000L, UsageEventReconstructor.ACTIVITY_RESUMED),
            UsageEvent(pkgA, 2000L, UsageEventReconstructor.ACTIVITY_PAUSED),
            UsageEvent(pkgA, 2400L, UsageEventReconstructor.ACTIVITY_RESUMED),
            UsageEvent(pkgA, 3000L, UsageEventReconstructor.ACTIVITY_PAUSED)
        )
        val sessions = reconstructor.reconstructSessions(events, 4000L)
        
        // Should be treated as one session from 1000 to 3000
        assertEquals(1, sessions.size)
        assertEquals(1000L, sessions[0].startTime)
        assertEquals(3000L, sessions[0].endTime)
    }

    @Test
    fun `test app switch`() {
        val events = listOf(
            UsageEvent(pkgA, 1000L, UsageEventReconstructor.ACTIVITY_RESUMED),
            UsageEvent(pkgB, 2000L, UsageEventReconstructor.ACTIVITY_RESUMED),
            UsageEvent(pkgB, 3000L, UsageEventReconstructor.ACTIVITY_PAUSED)
        )
        val sessions = reconstructor.reconstructSessions(events, 4000L)
        
        assertEquals(2, sessions.size)
        assertEquals(pkgA, sessions[0].packageName)
        assertEquals(2000L, sessions[0].endTime) // Closed by pkgB resume
        assertEquals(pkgB, sessions[1].packageName)
        assertEquals(2000L, sessions[1].startTime)
    }

    @Test
    fun `test midnight crossing clipping`() {
        val sessions = listOf(
            UsageSession(pkgA, 90L, 110L) // Spans over 100L
        )
        // rangeStart = 100L
        val usage = reconstructor.aggregateUsage(sessions, 100L, 200L)
        
        assertEquals(10L, usage[pkgA]?.duration) // Only 100L to 110L
    }

    @Test
    fun `test screen lock`() {
        val events = listOf(
            UsageEvent(pkgA, 1000L, UsageEventReconstructor.ACTIVITY_RESUMED),
            UsageEvent(pkgA, 1500L, UsageEventReconstructor.SCREEN_NON_INTERACTIVE)
        )
        val sessions = reconstructor.reconstructSessions(events, 2000L)
        
        assertEquals(1, sessions.size)
        assertEquals(1500L, sessions[0].endTime)
    }

    @Test
    fun `test session already active at rangeStart`() {
        val sessions = listOf(
            UsageSession(pkgA, 500L, 1500L)
        )
        // rangeStart = 1000L
        val usage = reconstructor.aggregateUsage(sessions, 1000L, 2000L)
        
        assertEquals(500L, usage[pkgA]?.duration) // 1000 to 1500
    }

    @Test
    fun `test still active at queryEnd`() {
        val events = listOf(
            UsageEvent(pkgA, 1000L, UsageEventReconstructor.ACTIVITY_RESUMED)
        )
        val sessions = reconstructor.reconstructSessions(events, 2000L)
        
        assertEquals(1, sessions.size)
        assertEquals(2000L, sessions[0].endTime)
    }

    @Test
    fun `longest session clips range and ignores system packages`() {
        val sessions = listOf(
            UsageSession(pkgA, 50L, 250L),
            UsageSession(pkgB, 110L, 190L),
            UsageSession("com.android.systemui", 100L, 300L),
        )

        val longest = reconstructor.longestSessionDuration(
            sessions = sessions,
            rangeStart = 100L,
            rangeEnd = 300L,
            ignoredPackages = setOf("com.android.systemui"),
        )

        assertEquals(150L, longest)
    }

    @Test
    fun `hourly usage splits a session across hour boundaries`() {
        val hour = 60 * 60 * 1000L
        val sessions = listOf(UsageSession(pkgA, 30 * 60_000L, hour + 15 * 60_000L))

        val usage = reconstructor.aggregateHourlyUsage(
            sessions = sessions,
            rangeStart = 0L,
            rangeEnd = 2 * hour,
            zoneId = ZoneId.of("UTC"),
        )

        assertEquals(30 * 60_000L, usage[0])
        assertEquals(15 * 60_000L, usage[1])
    }
}
