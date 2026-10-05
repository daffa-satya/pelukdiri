package com.makhp.pelukdiri.features.analytics

import com.makhp.pelukdiri.collector.AppUsageInsight
import com.makhp.pelukdiri.collector.UsageEventCollector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class AnalyticsComparisonTest {

    @Test
    fun `percentage comparison preserves direction and handles missing baseline`() {
        assertEquals(25, percentageChange(125, 100))
        assertEquals(-25, percentageChange(75, 100))
        assertNull(percentageChange(10, 0))
    }

    @Test
    fun `only today's daily graph calculates automatically`() {
        val today = LocalDate.of(2026, 8, 22)

        assertEquals(true, shouldCalculateGraphAutomatically(AnalyticsPeriod.DAILY, today, today))
        assertEquals(false, shouldCalculateGraphAutomatically(AnalyticsPeriod.DAILY, today.minusDays(1), today))
        assertEquals(false, shouldCalculateGraphAutomatically(AnalyticsPeriod.WEEKLY, today, today))
        assertEquals(false, shouldCalculateGraphAutomatically(AnalyticsPeriod.MONTHLY, today, today))
    }

    @Test
    fun `session metrics preserve longest insight and average only active days`() {
        val shorter = AppUsageInsight(launchCount = 2, 100L, 150L)
        val longer = AppUsageInsight(launchCount = 3, 200L, 280L)
        val metrics = aggregateSessionMetrics(
            listOf(
                UsageEventCollector.DailySessionMetrics(
                    appInsights = mapOf("app" to shorter),
                    longestSessionMillis = 50L,
                    hourlyUsage = List(24) { if (it == 0) 60L else 0L },
                ),
                UsageEventCollector.DailySessionMetrics(
                    appInsights = mapOf("app" to longer),
                    longestSessionMillis = 80L,
                    hourlyUsage = List(24) { if (it <= 1) 120L else 0L },
                ),
                UsageEventCollector.DailySessionMetrics(
                    appInsights = emptyMap(),
                    longestSessionMillis = 0L,
                    hourlyUsage = List(24) { 0L },
                ),
            )
        )

        assertEquals(5, metrics.appInsights.getValue("app").launchCount)
        assertEquals(200L, metrics.appInsights.getValue("app").longestSessionStartMillis)
        assertEquals(80L, metrics.longestSessionMillis)
        assertEquals(90L, metrics.hourlyUsage[0])
        assertEquals(60L, metrics.hourlyUsage[1])
    }

}
