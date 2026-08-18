package com.makhp.pelukdiri.core.domain.usecase

import com.makhp.pelukdiri.core.domain.engine.CognitiveQuestionGenerator
import com.makhp.pelukdiri.core.domain.model.MathQuestion
import com.makhp.pelukdiri.core.domain.model.RiskAssessmentResult
import com.makhp.pelukdiri.core.domain.repository.AdaptiveLimitRepository
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

class GenerateInterventionQuestionUseCase @Inject constructor(
    private val cognitiveQuestionGenerator: CognitiveQuestionGenerator,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val adaptiveLimitRepository: AdaptiveLimitRepository
) {
    suspend operator fun invoke(): Pair<RiskAssessmentResult, MathQuestion> {
        val level = userPreferencesRepository.currentDifficulty.first()
        val question = cognitiveQuestionGenerator.generateQuestion(level)

        val dateString = LocalDate.now().toString()
        val adaptiveLimit = adaptiveLimitRepository.getLimitForDate(dateString)
        
        // Compatibility Result for UI and Logging
        val assessment = RiskAssessmentResult(
            riskScore = 0.0,
            level = level,
            penaltyMinutes = 0,
            calculatedLimitMinutes = adaptiveLimit?.calculatedLimitMinutes ?: 0
        )
        return assessment to question
    }
}
