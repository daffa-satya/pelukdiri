package com.makhp.pelukdiri.core.domain.engine

import com.makhp.pelukdiri.core.domain.model.AdaptiveLimitConfig
import com.makhp.pelukdiri.core.domain.model.AdaptiveLimitResult
import com.makhp.pelukdiri.core.domain.model.DeviationResult
import com.makhp.pelukdiri.core.domain.model.DeviationStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class AdaptiveLimitGenerator @Inject constructor(
    private val config: AdaptiveLimitConfig
) {

    /**
     * Generates the initial personalized limit for a day.
     * Formula v0.1: L = B + beta * M
     * where B = baseline (median), M = MAD, beta = config.beta (default 1.0)
     */
    fun generateInitialLimit(deviationResult: DeviationResult): AdaptiveLimitResult {
        return when (deviationResult.status) {
            DeviationStatus.Success -> {
                val baseline = deviationResult.baseline ?: return AdaptiveLimitResult.InsufficientHistory
                val mad = deviationResult.mad ?: 0.0
                
                val rawLimit = baseline + (config.beta * mad)
                AdaptiveLimitResult.Personalized(rawLimit.roundToInt())
            }
            DeviationStatus.InsufficientHistory -> {
                AdaptiveLimitResult.InsufficientHistory
            }
        }
    }
}
