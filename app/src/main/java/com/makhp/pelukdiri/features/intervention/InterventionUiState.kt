package com.makhp.pelukdiri.features.intervention

import androidx.compose.runtime.Immutable
import com.makhp.pelukdiri.core.domain.model.MathQuestion
import com.makhp.pelukdiri.core.domain.model.RiskAssessmentResult

@Immutable
sealed interface InterventionUiState {
    data object Idle : InterventionUiState
    data object Loading : InterventionUiState

    data class QuestionActive(
        val question: MathQuestion,
        val assessment: RiskAssessmentResult,
        val answerInput: String = "",
        val remainingBypasses: Int = 5,
        val bypassDenied: Boolean = false
    ) : InterventionUiState

    data class CorrectAnswer(
        val question: MathQuestion,
        val assessment: RiskAssessmentResult,
        val responseTimeMs: Long
    ) : InterventionUiState

    data class IncorrectAnswer(
        val question: MathQuestion,
        val assessment: RiskAssessmentResult,
        val enteredAnswer: String,
        val responseTimeMs: Long,
        val remainingBypasses: Int = 5
    ) : InterventionUiState

    data class MaxPenalized(
        val question: MathQuestion,
        val assessment: RiskAssessmentResult,
        val answerInput: String = "",
        val remainingBypasses: Int = 5,
        val bypassDenied: Boolean = false
    ) : InterventionUiState
}
