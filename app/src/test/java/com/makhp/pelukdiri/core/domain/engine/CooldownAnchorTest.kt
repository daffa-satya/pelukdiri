package com.makhp.pelukdiri.core.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CooldownAnchorTest {
    @Test fun `preserves selected interval but starts it after completion`() {
        assertEquals(
            1_480_000L,
            CooldownAnchor.afterCompletion(
                sessionCreatedAtMs = 100_000L,
                originallyEligibleAtMs = 280_000L,
                completedAtMs = 1_300_000L,
            )
        )
    }

    @Test fun `does not create cooldown when launch never committed one`() {
        assertNull(CooldownAnchor.afterCompletion(100_000L, 0L, 200_000L))
    }
}
