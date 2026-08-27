package com.makhp.pelukdiri.core.domain.model

import com.makhp.pelukdiri.di.EngineModule
import org.junit.Assert.assertEquals
import org.junit.Test

class PolicyConfigTest {
    @Test
    fun `production uses candidate 3`() {
        assertEquals(ControlConfig.CANDIDATE_3, EngineModule.provideControlConfig())
        assertEquals(DeviationConfig.CANDIDATE_3, EngineModule.provideDeviationConfig())
        assertEquals("v1.6-two-success-recovery", ControlConfig.POLICY_VERSION)
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

        assertEquals(0.2, ControlConfig.CANDIDATE_3.lambdaDifficulty, 0.0)
        assertEquals(1.0, ControlConfig.CANDIDATE_3.lambdaFrequency, 0.0)
        assertEquals(2, ControlConfig.CANDIDATE_3.performanceEvidenceWindow)
        assertEquals(3, ControlConfig.CANDIDATE_3.difficultyDecreaseEvidenceWindow)
        assertEquals(2, ControlConfig.CANDIDATE_3.ordinaryDecreaseFailureWindow)
        assertEquals(2, ControlConfig.CANDIDATE_3.recoverySuccessWindow)
        assertEquals(2, ControlConfig.CANDIDATE_3.normalMinimumDifficulty)
        assertEquals(true, ControlConfig.CANDIDATE_3.useAdaptiveLimitFrequencyFloor)
        assertEquals(0.3, DeviationConfig.CANDIDATE_3.k, 0.0)
        assertEquals(2.0, DeviationConfig.CANDIDATE_3.s0, 0.0)
        assertEquals(0.5, DeviationConfig.CANDIDATE_3.minimumMadFractionOfBaseline, 0.0)

        listOf(
            ControlConfig.LEGACY_DEFAULT,
            ControlConfig.CANDIDATE_1,
        ).forEach { config ->
            assertEquals(1, config.normalMinimumDifficulty)
            assertEquals(false, config.useAdaptiveLimitFrequencyFloor)
            assertEquals(0, config.ordinaryDecreaseFailureWindow)
            assertEquals(1, config.recoverySuccessWindow)
        }
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
