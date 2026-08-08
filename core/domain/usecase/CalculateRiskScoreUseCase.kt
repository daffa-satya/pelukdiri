package com.makhp.pelukdiri.core.domain.usecase

import com.makhp.pelukdiri.core.domain.model.RiskAssessmentResult
import javax.inject.Inject
import kotlin.math.max

class CalculateRiskScoreUseCase @Inject constructor() {

    operator fun invoke(
        screenTimeMinutes: Double,
        launchFrequency: Double,
        ambientLightLux: Float,
        baselineLimitMinutes: Double = 60.0,
        frequencyBaseline: Double = 10.0
    ): RiskAssessmentResult {
        require(baselineLimitMinutes > 0.0) { "baselineLimitMinutes must be > 0" }
        require(frequencyBaseline > 0.0) { "frequencyBaseline must be > 0" }

        val sanitizedScreenTime = max(screenTimeMinutes, 0.0)
        val sanitizedFrequency = max(launchFrequency, 0.0)
        val isDark = if (ambientLightLux < 20.0f) 1.0 else 0.0

        val riskScore = (0.4 * (sanitizedScreenTime / baselineLimitMinutes)) +
            (0.4 * (sanitizedFrequency / frequencyBaseline)) +
            (0.2 * isDark)

        val level = when {
            riskScore < 0.8 -> 1
            riskScore < 1.2 -> 2
            riskScore < 1.6 -> 3
            riskScore < 2.0 -> 4
            else -> 5
        }

        val penaltyMinutes = when (level) {
            1 -> 0
            2 -> 5
            3 -> 10
            4 -> 15
            else -> 20
        }

        val calculatedLimitMinutes = max(baselineLimitMinutes.toInt() - penaltyMinutes, 15)

        return RiskAssessmentResult(
            riskScore = riskScore,
            level = level,
            penaltyMinutes = penaltyMinutes,
            calculatedLimitMinutes = calculatedLimitMinutes
        )
    }
}
