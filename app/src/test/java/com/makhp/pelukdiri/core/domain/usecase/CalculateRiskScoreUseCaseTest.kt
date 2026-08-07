package com.makhp.pelukdiri.core.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateRiskScoreUseCaseTest {
    private val useCase = CalculateRiskScoreUseCase()

    @Test
    fun `returns level 1 with zero penalty when risk below 0_8`() {
        val result = useCase(
            screenTimeMinutes = 20.0,
            launchFrequency = 2.0,
            ambientLightLux = 100f
        )

        assertEquals(1, result.level)
        assertEquals(0, result.penaltyMinutes)
        assertEquals(60, result.calculatedLimitMinutes)
    }

    @Test
    fun `maps boundaries to expected tiers and penalties`() {
        val level2 = useCase(
            screenTimeMinutes = 120.0,
            launchFrequency = 0.0,
            ambientLightLux = 100f
        )
        assertEquals(2, level2.level)
        assertEquals(5, level2.penaltyMinutes)

        val level3 = useCase(
            screenTimeMinutes = 120.0,
            launchFrequency = 13.0,
            ambientLightLux = 100f
        )
        assertEquals(3, level3.level)
        assertEquals(10, level3.penaltyMinutes)

        val level4 = useCase(
            screenTimeMinutes = 180.0,
            launchFrequency = 10.0,
            ambientLightLux = 100f
        )
        assertEquals(4, level4.level)
        assertEquals(15, level4.penaltyMinutes)

        val level5 = useCase(
            screenTimeMinutes = 240.0,
            launchFrequency = 10.0,
            ambientLightLux = 100f
        )
        assertEquals(5, level5.level)
        assertEquals(20, level5.penaltyMinutes)
    }

    @Test
    fun `enforces minimum calculated limit at 15 minutes`() {
        val result = useCase(
            screenTimeMinutes = 600.0,
            launchFrequency = 200.0,
            ambientLightLux = 1f,
            baselineLimitMinutes = 30.0
        )

        assertEquals(5, result.level)
        assertEquals(20, result.penaltyMinutes)
        assertEquals(15, result.calculatedLimitMinutes)
    }
}
