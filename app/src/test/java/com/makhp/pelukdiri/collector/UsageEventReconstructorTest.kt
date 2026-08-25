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
    fun `longest session clips range and includes system and PelukDiri packages`() {
        val sessions = listOf(
            UsageSession(pkgA, 50L, 250L),
            UsageSession(pkgB, 110L, 190L),
            UsageSession("com.android.settings", 100L, 300L),
            UsageSession("com.makhp.pelukdiri", 120L, 130L),
        )

        val longest = reconstructor.longestSessionDuration(
            sessions = sessions,
            rangeStart = 100L,
            rangeEnd = 300L,
        )

        assertEquals(200L, longest)
    }

    @Test
    fun `app insights count launches and report each package longest clipped session`() {
        val sessions = listOf(
            UsageSession(pkgA, 50L, 160L),
            UsageSession(pkgA, 180L, 260L),
            UsageSession(pkgB, 120L, 150L),
        )
        val events = listOf(
            UsageEvent(pkgA, 90L, UsageEventReconstructor.ACTIVITY_RESUMED),
            UsageEvent(pkgA, 190L, UsageEventReconstructor.ACTIVITY_RESUMED),
            UsageEvent(pkgB, 120L, UsageEventReconstructor.ACTIVITY_RESUMED),
            UsageEvent(pkgB, 250L, UsageEventReconstructor.ACTIVITY_RESUMED),
        )

        val insights = reconstructor.appInsights(events, sessions, rangeStart = 100L, rangeEnd = 200L)

        assertEquals(1, insights[pkgA]?.launchCount)
        assertEquals(100L, insights[pkgA]?.longestSessionStartMillis)
        assertEquals(160L, insights[pkgA]?.longestSessionEndMillis)
        assertEquals(60L, insights[pkgA]?.longestSessionDurationMillis)
        assertEquals(1, insights[pkgB]?.launchCount)
        assertEquals(120L, insights[pkgB]?.longestSessionStartMillis)
        assertEquals(150L, insights[pkgB]?.longestSessionEndMillis)
    }

    @Test
    fun `longest usage sequence joins target sessions separated by intervention overlay`() {
        val intervention = "com.makhp.pelukdiri.debug"
        val sessions = listOf(
            UsageSession(pkgA, 100L, 500L),
            UsageSession(intervention, 500L, 600L),
            UsageSession(pkgA, 600L, 900L),
            UsageSession(pkgB, 900L, 1_000L),
            UsageSession(pkgA, 1_000L, 1_100L),
        )
        val events = listOf(
            UsageEvent(pkgA, 100L, UsageEventReconstructor.ACTIVITY_RESUMED),
            UsageEvent(intervention, 500L, UsageEventReconstructor.ACTIVITY_RESUMED, "InterventionActivity"),
            UsageEvent(pkgA, 600L, UsageEventReconstructor.ACTIVITY_RESUMED),
            UsageEvent(pkgB, 900L, UsageEventReconstructor.ACTIVITY_RESUMED),
            UsageEvent(pkgA, 1_000L, UsageEventReconstructor.ACTIVITY_RESUMED),
        )

        val insights = reconstructor.appInsights(
            events = events,
            sessions = sessions,
            rangeStart = 0L,
            rangeEnd = 2_000L,
            interstitialPackages = setOf(intervention),
        )

        assertEquals(700L, insights[pkgA]?.longestSessionDurationMillis)
        assertEquals(100L, insights[pkgA]?.longestSessionStartMillis)
        assertEquals(900L, insights[pkgA]?.longestSessionEndMillis)
    }

    @Test
    fun `app opening count ignores repeated resumes inside one foreground session`() {
        val events = listOf(
            UsageEvent(pkgA, 100L, UsageEventReconstructor.ACTIVITY_RESUMED),
            UsageEvent(pkgA, 200L, UsageEventReconstructor.ACTIVITY_RESUMED),
            UsageEvent(pkgA, 300L, UsageEventReconstructor.ACTIVITY_PAUSED),
            UsageEvent(pkgA, 600L, UsageEventReconstructor.ACTIVITY_RESUMED),
            UsageEvent(pkgB, 700L, UsageEventReconstructor.ACTIVITY_RESUMED),
            UsageEvent(pkgB, 800L, UsageEventReconstructor.ACTIVITY_PAUSED),
            UsageEvent(pkgA, 900L, UsageEventReconstructor.ACTIVITY_RESUMED),
        )

        val counts = reconstructor.countForegroundStarts(events, 0L, 1_000L)

        assertEquals(2, counts[pkgA])
        assertEquals(1, counts[pkgB])
    }

    @Test
    fun `app opening count treats multiple activities in one package as one opening`() {
        val activityA = "MainActivity"
        val activityB = "DialogActivity"
        val events = listOf(
            UsageEvent(pkgA, 100L, UsageEventReconstructor.ACTIVITY_RESUMED, activityA),
            UsageEvent(pkgA, 200L, UsageEventReconstructor.ACTIVITY_PAUSED, activityA),
            UsageEvent(pkgA, 210L, UsageEventReconstructor.ACTIVITY_RESUMED, activityB),
            UsageEvent(pkgA, 400L, UsageEventReconstructor.ACTIVITY_STOPPED, activityA),
            UsageEvent(pkgA, 500L, UsageEventReconstructor.ACTIVITY_PAUSED, activityB),
            UsageEvent(pkgA, 600L, UsageEventReconstructor.ACTIVITY_STOPPED, activityB),
            UsageEvent(pkgA, 1_000L, UsageEventReconstructor.ACTIVITY_RESUMED, activityA),
        )

        val counts = reconstructor.countForegroundStarts(events, 0L, 2_000L)

        assertEquals(2, counts[pkgA])
    }

    @Test
    fun `screen off and unlock of the same app does not count another opening`() {
        val events = listOf(
            UsageEvent(pkgA, 100L, UsageEventReconstructor.ACTIVITY_RESUMED, "MainActivity"),
            UsageEvent(pkgA, 190L, UsageEventReconstructor.ACTIVITY_PAUSED, "MainActivity"),
            UsageEvent(pkgA, 200L, UsageEventReconstructor.SCREEN_NON_INTERACTIVE),
            UsageEvent(pkgA, 300L, UsageEventReconstructor.ACTIVITY_RESUMED, "MainActivity"),
            UsageEvent(pkgA, 310L, UsageEventReconstructor.ACTIVITY_RESUMED, "DialogActivity"),
            UsageEvent(pkgB, 500L, UsageEventReconstructor.ACTIVITY_RESUMED, "OtherActivity"),
            UsageEvent(pkgA, 600L, UsageEventReconstructor.ACTIVITY_RESUMED, "MainActivity"),
        )

        val counts = reconstructor.countForegroundStarts(
            events = events,
            rangeStart = 0L,
            rangeEnd = 1_000L,
            interstitialPackages = setOf(pkgA),
        )

        assertEquals(2, counts[pkgA])
        assertEquals(1, counts[pkgB])
    }

    @Test
    fun `intervention overlay does not count return to interrupted app as new opening`() {
        val pelukDiri = "com.makhp.pelukdiri.debug"
        val events = listOf(
            UsageEvent(pkgA, 100L, UsageEventReconstructor.ACTIVITY_RESUMED, "VideoActivity"),
            UsageEvent(pelukDiri, 200L, UsageEventReconstructor.ACTIVITY_RESUMED, "InterventionActivity"),
            UsageEvent(pelukDiri, 300L, UsageEventReconstructor.ACTIVITY_PAUSED, "InterventionActivity"),
            UsageEvent(pkgA, 400L, UsageEventReconstructor.ACTIVITY_RESUMED, "VideoActivity"),
        )

        val counts = reconstructor.countForegroundStarts(
            events = events,
            rangeStart = 0L,
            rangeEnd = 1_000L,
            interstitialPackages = setOf(pelukDiri),
        )

        assertEquals(1, counts[pkgA])
        assertEquals(null, counts[pelukDiri])
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
