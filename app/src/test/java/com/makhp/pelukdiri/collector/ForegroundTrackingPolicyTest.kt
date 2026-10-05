package com.makhp.pelukdiri.collector

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundTrackingPolicyTest {
    private val monitored = setOf("com.google.android.youtube")

    @Test fun `same monitored package restarts a missing evaluator`() {
        assertTrue(
            ForegroundTrackingPolicy.shouldRestart(
                "com.google.android.youtube", "com.google.android.youtube", monitored, false
            )
        )
    }

    @Test fun `active evaluator is not duplicated`() {
        assertFalse(
            ForegroundTrackingPolicy.shouldRestart(
                "com.google.android.youtube", "com.google.android.youtube", monitored, true
            )
        )
    }

    @Test fun `unmonitored same package does not start evaluator`() {
        assertFalse(ForegroundTrackingPolicy.shouldRestart("example.app", "example.app", monitored, false))
    }

    @Test fun `removed foreground package is no longer tracked`() {
        assertFalse(
            ForegroundTrackingPolicy.shouldTrack(
                "com.google.android.youtube",
                "com.makhp.pelukdiri",
                emptySet(),
                emptySet(),
            )
        )
    }

    @Test fun `next delay uses one clock snapshot and nearest deadline`() {
        assertEquals(2_000L, ForegroundTrackingPolicy.nextDelay(10_000L, 15_000L, 12_000L))
        assertEquals(1_000L, ForegroundTrackingPolicy.nextDelay(12_000L, 15_000L, 12_000L))
    }

    @Test fun `both app variants are excluded from their own interventions`() {
        assertFalse(
            ForegroundTrackingPolicy.shouldTrack(
                "com.makhp.pelukdiri",
                "com.makhp.pelukdiri",
                setOf("com.makhp.pelukdiri"),
                emptySet(),
            )
        )
        assertFalse(
            ForegroundTrackingPolicy.shouldTrack(
                "com.makhp.pelukdiri.debug",
                "com.makhp.pelukdiri.debug",
                setOf("com.makhp.pelukdiri.debug"),
                emptySet(),
            )
        )
    }
}
