package com.makhp.pelukdiri.core.domain.model

import com.makhp.pelukdiri.di.EngineModule
import org.junit.Assert.assertEquals
import org.junit.Test

class PolicyConfigTest {
    @Test
    fun `production uses candidate 3`() {
        assertEquals(ControlConfig.CANDIDATE_3, EngineModule.provideControlConfig())
        assertEquals(DeviationConfig.CANDIDATE_3, EngineModule.provideDeviationConfig())
        assertEquals("v0.6-candidate-3", ControlConfig.POLICY_VERSION)
    }

    @Test
    fun `candidate and legacy presets retain approved constants`() {
        assertEquals(ControlConfig(), ControlConfig.CANDIDATE_1)
        assertEquals(DeviationConfig(), DeviationConfig.CANDIDATE_1)

        assertEquals(0.5, ControlConfig.LEGACY_DEFAULT.lambdaDifficulty, 0.0)
        assertEquals(0.5, ControlConfig.LEGACY_DEFAULT.lambdaFrequency, 0.0)
        assertEquals(0.75, DeviationConfig.LEGACY_DEFAULT.k, 0.0)
        assertEquals(2.0, DeviationConfig.LEGACY_DEFAULT.s0, 0.0)
        assertEquals(0.0, DeviationConfig.LEGACY_DEFAULT.minimumMadMinutes, 0.0)
        assertEquals(0.0, DeviationConfig.LEGACY_DEFAULT.minimumMadFractionOfBaseline, 0.0)

        assertEquals(0.0, ControlConfig.CANDIDATE_3.lambdaDifficulty, 0.0)
        assertEquals(1.0, ControlConfig.CANDIDATE_3.lambdaFrequency, 0.0)
        assertEquals(0.1, DeviationConfig.CANDIDATE_3.k, 0.0)
        assertEquals(0.5, DeviationConfig.CANDIDATE_3.minimumMadFractionOfBaseline, 0.0)

        listOf(
            ControlConfig.LEGACY_DEFAULT,
            ControlConfig.CANDIDATE_1,
            ControlConfig.CANDIDATE_3,
        ).forEach { config ->
            assertEquals(3, config.reversalGuardInterventions)
            assertEquals(3.0, config.minFrequencyMinutes, 0.0)
            assertEquals(30.0, config.maxFrequencyMinutes, 0.0)
        }
    }
}
