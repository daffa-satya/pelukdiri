package com.makhp.pelukdiri.features.intervention

import org.junit.Assert.assertEquals
import org.junit.Test

class InterventionResponseTimeFormatTest {
    @Test
    fun `response milliseconds are formatted as decimal seconds`() {
        assertEquals("3.5", formatResponseTimeSeconds(3_500L))
        assertEquals("0.0", formatResponseTimeSeconds(0L))
    }
}
