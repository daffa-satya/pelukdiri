package com.makhp.pelukdiri.core.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class InterventionChallengeSelectorTest {
    private val selector = InterventionChallengeSelector()

    @Test fun `first challenge is math and subsequent challenge alternates`() {
        assertEquals(InterventionChallengeType.MATH, selector.select(null))
        assertEquals(
            InterventionChallengeType.PATTERN,
            selector.select(InterventionChallengeType.MATH),
        )
        assertEquals(
            InterventionChallengeType.MATH,
            selector.select(InterventionChallengeType.PATTERN),
        )
    }
}
