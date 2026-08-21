package com.makhp.pelukdiri.core.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class InterventionChallengeSelectorTest {
    @Test fun `heads selects math`() {
        val selector = InterventionChallengeSelector { true }
        assertEquals(InterventionChallengeType.MATH, selector.select())
    }

    @Test fun `tails selects pattern`() {
        val selector = InterventionChallengeSelector { false }
        assertEquals(InterventionChallengeType.PATTERN, selector.select())
    }
}
