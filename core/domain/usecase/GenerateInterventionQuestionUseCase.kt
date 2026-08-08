package com.makhp.pelukdiri.core.domain.usecase

import com.makhp.pelukdiri.core.domain.engine.CognitiveQuestionGenerator
import com.makhp.pelukdiri.core.domain.model.MathQuestion
import com.makhp.pelukdiri.core.domain.model.RiskAssessmentResult
import javax.inject.Inject

class GenerateInterventionQuestionUseCase @Inject constructor(
    private val calculateRiskScoreUseCase: CalculateRiskScoreUseCase,
    private val cognitiveQuestionGenerator: CognitiveQuestionGenerator
) {
    operator fun invoke(
        screenTimeMinutes: Double,
        launchFrequency: Double,
        ambientLightLux: Float,
        baselineLimitMinutes: Double = 60.0,
        frequencyBaseline: Double = 10.0
    ): Pair<RiskAssessmentResult, MathQuestion> {
        val assessment = calculateRiskScoreUseCase(
            screenTimeMinutes = screenTimeMinutes,
            launchFrequency = launchFrequency,
            ambientLightLux = ambientLightLux,
            baselineLimitMinutes = baselineLimitMinutes,
            frequencyBaseline = frequencyBaseline
        )
        val question = cognitiveQuestionGenerator.generateQuestion(assessment.level)
        return assessment to question
    }
}
