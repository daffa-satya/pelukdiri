package com.makhp.pelukdiri.features.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class AllAppsProgressTest {

    @Test
    fun `app progress is its share of total screen time`() {
        assertEquals(0.5f, appUsageShare(30 * 60_000L, 60 * 60_000L))
        assertEquals(0f, appUsageShare(30 * 60_000L, 0L))
        assertEquals(1f, appUsageShare(90 * 60_000L, 60 * 60_000L))
    }
}
