package com.makhp.pelukdiri.ui

import com.makhp.pelukdiri.features.analytics.AnalyticsPeriod
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class AllAppsRouteTest {
    @Test
    fun `all apps route preserves selected date and period`() {
        assertEquals(
            "all_apps/2026-08-10/WEEKLY",
            Screen.AllApps.route(LocalDate.of(2026, 8, 10), AnalyticsPeriod.WEEKLY),
        )
    }
}
