package com.makhp.pelukdiri.features.intervention

import org.junit.Assert.assertEquals
import org.junit.Test

class InterventionResponseTimeFormatTest {
    @Test
    fun `response milliseconds are formatted as decimal seconds`() {
        assertEquals("3.5", formatResponseTimeSeconds(3_500L))
        assertEquals("0.0", formatResponseTimeSeconds(0L))
    }

    @Test
    fun `active timer is formatted as minutes and seconds`() {
        assertEquals("00.00", formatInterventionTimer(0L))
        assertEquals("01.05", formatInterventionTimer(65_999L))
        assertEquals("00.00", formatInterventionTimer(-1L))
    }

    @Test
    fun `pattern replay freezes timer while initial playback stays at zero`() {
        assertEquals(4_200L, nextInterventionTimerElapsed(4_200L, 9_000L, true, true))
        assertEquals(0L, nextInterventionTimerElapsed(4_200L, 9_000L, true, false))
        assertEquals(9_000L, nextInterventionTimerElapsed(4_200L, 9_000L, false, true))
    }
}
