package com.makhp.pelukdiri.core.domain.engine

import com.makhp.pelukdiri.core.domain.model.AdaptiveLimitConfig
import com.makhp.pelukdiri.core.domain.model.AdaptiveLimitResult
import com.makhp.pelukdiri.core.domain.model.DeviationResult
import com.makhp.pelukdiri.core.domain.model.DeviationStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveLimitGeneratorTest {

    private val config = AdaptiveLimitConfig(beta = 1.0)
    private val generator = AdaptiveLimitGenerator(config)

    @Test
    fun `generateInitialLimit returns Personalized with baseline plus MAD when status is Success`() {
        val deviationResult = DeviationResult(
            deviation = 0.5,
            baseline = 120.5,
            mad = 10.0,
            signal = 1.0,
            relativeDeviation = 1.0,
            relativeMagnitude = 0.1,
            status = DeviationStatus.Success
        )

        // Formula: 120.5 + 1.0 * 10.0 = 130.5 -> rounds to 131
        val result = generator.generateInitialLimit(deviationResult)

        assert(result is AdaptiveLimitResult.Personalized)
        assertEquals(131, (result as AdaptiveLimitResult.Personalized).limitMinutes)
    }

    @Test
    fun `generateInitialLimit handles beta multiplier correctly`() {
        val generatorWithBeta = AdaptiveLimitGenerator(AdaptiveLimitConfig(beta = 2.0))
        val deviationResult = DeviationResult(
            deviation = 0.5,
            baseline = 100.0,
            mad = 10.0,
            signal = 1.0,
            relativeDeviation = 1.0,
            relativeMagnitude = 0.1,
            status = DeviationStatus.Success
        )

        // Formula: 100.0 + 2.0 * 10.0 = 120.0
        val result = generatorWithBeta.generateInitialLimit(deviationResult)

        assert(result is AdaptiveLimitResult.Personalized)
        assertEquals(120, (result as AdaptiveLimitResult.Personalized).limitMinutes)
    }

    @Test
    fun `generateInitialLimit handles MAD of zero`() {
        val deviationResult = DeviationResult(
            deviation = 0.0,
            baseline = 100.0,
            mad = 0.0,
            signal = 0.0,
            relativeDeviation = 0.0,
            relativeMagnitude = 0.0,
            status = DeviationStatus.Success
        )

        // Formula: 100.0 + 1.0 * 0.0 = 100.0
        val result = generator.generateInitialLimit(deviationResult)

        assert(result is AdaptiveLimitResult.Personalized)
        assertEquals(100, (result as AdaptiveLimitResult.Personalized).limitMinutes)
    }

    @Test
    fun `generateInitialLimit returns InsufficientHistory when status is InsufficientHistory`() {
        val deviationResult = DeviationResult(
            deviation = null,
            baseline = null,
            mad = null,
            signal = null,
            relativeDeviation = null,
            relativeMagnitude = null,
            status = DeviationStatus.InsufficientHistory
        )

        val result = generator.generateInitialLimit(deviationResult)

        assert(result is AdaptiveLimitResult.InsufficientHistory)
    }
}
