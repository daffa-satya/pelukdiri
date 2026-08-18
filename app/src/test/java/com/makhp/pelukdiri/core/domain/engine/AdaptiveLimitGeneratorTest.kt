package com.makhp.pelukdiri.core.domain.engine

import com.makhp.pelukdiri.core.domain.model.AdaptiveLimitResult
import com.makhp.pelukdiri.core.domain.model.DeviationResult
import com.makhp.pelukdiri.core.domain.model.DeviationStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveLimitGeneratorTest {

    private val generator = AdaptiveLimitGenerator()

    @Test
    fun `generateInitialLimit returns Personalized when status is Success`() {
        val deviationResult = DeviationResult(
            deviation = 0.5,
            baseline = 120.5,
            mad = 10.0,
            signal = 1.0,
            relativeDeviation = 1.0,
            relativeMagnitude = 0.1,
            status = DeviationStatus.Success
        )

        val result = generator.generateInitialLimit(deviationResult)

        assert(result is AdaptiveLimitResult.Personalized)
        assertEquals(121, (result as AdaptiveLimitResult.Personalized).limitMinutes)
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
