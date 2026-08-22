package com.makhp.pelukdiri.features.analytics

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

}
