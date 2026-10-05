package com.makhp.pelukdiri.core.data.repository

import com.makhp.pelukdiri.core.domain.model.AggressivenessLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class UserPreferencesRepositoryImplTest {

    @Test
    fun `aggressiveness parser preserves known values and recovers unknown values`() {
        AggressivenessLevel.entries.forEach { level ->
            assertEquals(level, parseAggressivenessLevel(level.name))
        }
        assertEquals(AggressivenessLevel.BALANCED, parseAggressivenessLevel(null))
        assertEquals(AggressivenessLevel.BALANCED, parseAggressivenessLevel("LEGACY_OR_CORRUPT"))
    }
}
