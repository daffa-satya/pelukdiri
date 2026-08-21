package com.makhp.pelukdiri.collector

import org.junit.Assert.assertFalse
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
}
