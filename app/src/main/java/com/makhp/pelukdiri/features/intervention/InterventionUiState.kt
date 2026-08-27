package com.makhp.pelukdiri.features.intervention

import androidx.compose.runtime.Immutable
import com.makhp.pelukdiri.core.domain.model.MathQuestion
import com.makhp.pelukdiri.core.domain.model.PatternQuestion
import com.makhp.pelukdiri.core.domain.model.PatternShape
import com.makhp.pelukdiri.core.domain.model.RiskAssessmentResult

@Immutable
sealed interface InterventionUiState {
    data object Idle : InterventionUiState
    data object Loading : InterventionUiState

    data class Error(
        val operation: FailedInterventionOperation
    ) : InterventionUiState

    data class QuestionActive(
        val question: MathQuestion,
        val assessment: RiskAssessmentResult,
        val answerInput: String = "",
        val remainingBypasses: Int = 5,
        val bypassDenied: Boolean = false
    ) : InterventionUiState

    data class PatternActive(
        val question: PatternQuestion,
        val assessment: RiskAssessmentResult,
        val answerInput: List<PatternShape> = emptyList(),
        val isPlaying: Boolean = true,
        val playbackIndex: Int? = null,
        val replaysRemaining: Int = 1,
        val remainingBypasses: Int = 5,
        val bypassDenied: Boolean = false,
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

    data class PatternCorrectAnswer(
        val question: PatternQuestion,
        val assessment: RiskAssessmentResult,
        val responseTimeMs: Long,
    ) : InterventionUiState

    data class PatternIncorrectAnswer(
        val question: PatternQuestion,
        val assessment: RiskAssessmentResult,
        val enteredSequence: List<PatternShape>,
        val responseTimeMs: Long,
        val remainingBypasses: Int = 5,
    ) : InterventionUiState

    data class MaxPenalized(
        val question: MathQuestion,
        val assessment: RiskAssessmentResult,
        val answerInput: String = "",
        val remainingBypasses: Int = 5,
        val bypassDenied: Boolean = false
    ) : InterventionUiState
}

enum class FailedInterventionOperation {
    START,
    RESTORE,
    SUBMIT_ANSWER,
    RETRY_CHALLENGE,
    EMERGENCY_BYPASS,
    COMPLETE,
}
