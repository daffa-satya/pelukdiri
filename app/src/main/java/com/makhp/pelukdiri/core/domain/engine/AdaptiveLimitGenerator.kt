package com.makhp.pelukdiri.core.domain.engine

import com.makhp.pelukdiri.core.domain.model.AdaptiveLimitResult
import com.makhp.pelukdiri.core.domain.model.DeviationResult
import com.makhp.pelukdiri.core.domain.model.DeviationStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class AdaptiveLimitGenerator @Inject constructor() {

    fun generateInitialLimit(deviationResult: DeviationResult): AdaptiveLimitResult {
        return when (deviationResult.status) {
            DeviationStatus.Success -> {
                val baseline = deviationResult.baseline ?: return AdaptiveLimitResult.InsufficientHistory
                AdaptiveLimitResult.Personalized(baseline.roundToInt())
            }
            DeviationStatus.InsufficientHistory -> {
                AdaptiveLimitResult.InsufficientHistory
            }
        }
    }
}
