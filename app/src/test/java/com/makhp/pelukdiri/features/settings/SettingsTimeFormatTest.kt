package com.makhp.pelukdiri.features.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsTimeFormatTest {
    @Test
    fun `formats stored 24 hour times with AM and PM`() {
        assertEquals("12:00 AM", formatTimeWithPeriod("00:00"))
        assertEquals("6:00 AM", formatTimeWithPeriod("06:00"))
        assertEquals("12:00 PM", formatTimeWithPeriod("12:00"))
        assertEquals("10:00 PM", formatTimeWithPeriod("22:00"))
    }
}
