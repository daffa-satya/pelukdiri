package com.makhp.pelukdiri.collector

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageDayBoundsTest {
    @Test
    fun `local day boundaries preserve normal and daylight-saving durations`() {
        assertDurationHours(LocalDate.of(2026, 9, 13), "Asia/Jakarta", 24)
        assertDurationHours(LocalDate.of(2026, 3, 8), "America/New_York", 23)
        assertDurationHours(LocalDate.of(2026, 11, 1), "America/New_York", 25)
    }

    @Test
    fun `query end is capped to the requested local day`() {
        val bounds = usageDayBounds(LocalDate.of(2026, 9, 13), ZoneId.of("Asia/Jakarta"))

        assertEquals(bounds.startMillis, bounds.queryEnd(bounds.startMillis - 1L))
        assertEquals(bounds.startMillis + 1L, bounds.queryEnd(bounds.startMillis + 1L))
        assertEquals(bounds.endExclusiveMillis, bounds.queryEnd(bounds.endExclusiveMillis + 1L))
    }

    private fun assertDurationHours(date: LocalDate, zoneId: String, expectedHours: Long) {
        val bounds = usageDayBounds(date, ZoneId.of(zoneId))
        assertEquals(expectedHours * 60L * 60L * 1_000L, bounds.endExclusiveMillis - bounds.startMillis)
    }
}
