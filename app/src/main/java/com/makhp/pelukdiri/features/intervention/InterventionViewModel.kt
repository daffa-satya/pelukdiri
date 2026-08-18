package com.makhp.pelukdiri.features.intervention

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makhp.pelukdiri.core.domain.model.DailyAdaptiveLimit
import com.makhp.pelukdiri.core.domain.model.InterventionLog
import com.makhp.pelukdiri.core.domain.repository.AdaptiveLimitRepository
import com.makhp.pelukdiri.core.domain.repository.InterventionLogRepository
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import com.makhp.pelukdiri.core.domain.usecase.GenerateInterventionQuestionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.max
import kotlin.math.roundToInt
import javax.inject.Inject

@HiltViewModel
class InterventionViewModel @Inject constructor(
    private val generateInterventionQuestionUseCase: GenerateInterventionQuestionUseCase,
    private val interventionLogRepository: InterventionLogRepository,
    private val adaptiveLimitRepository: AdaptiveLimitRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<InterventionUiState>(InterventionUiState.Idle)
    val uiState: StateFlow<InterventionUiState> = _uiState.asStateFlow()

    private var questionStartTimeMs: Long = 0L
    private var currentScreenTimeMinutes: Double = 0.0

    fun startIntervention(
        screenTimeMinutes: Double,
        launchFrequency: Int,
        ambientLightLux: Float,
        baselineLimitMinutes: Double = 60.0,
        frequencyBaseline: Double = 10.0
    ) {
        _uiState.value = InterventionUiState.Loading
        currentScreenTimeMinutes = screenTimeMinutes
        
        viewModelScope.launch {
            val (assessment, question) = generateInterventionQuestionUseCase()
            val remaining = getRemainingBypasses()
            questionStartTimeMs = System.currentTimeMillis()
            _uiState.value = InterventionUiState.QuestionActive(
                question = question,
                assessment = assessment,
                remainingBypasses = remaining
            )
        }
    }

    fun onAnswerChanged(rawInput: String) {
        val sanitizedInput = rawInput.filter { it.isDigit() }
        val state = _uiState.value
        _uiState.value = when (state) {
            is InterventionUiState.QuestionActive -> state.copy(answerInput = sanitizedInput)
            is InterventionUiState.MaxPenalized -> state.copy(answerInput = sanitizedInput)
            else -> state
        }
    }

    fun submitAnswer() {
        val currentState = _uiState.value
        val input = when (currentState) {
            is InterventionUiState.QuestionActive -> currentState.answerInput
            is InterventionUiState.MaxPenalized -> currentState.answerInput
            else -> return
        }

        val enteredAnswer = input.toIntOrNull() ?: return
        val question = when (currentState) {
            is InterventionUiState.QuestionActive -> currentState.question
            is InterventionUiState.MaxPenalized -> currentState.question
            else -> return
        }
        val assessment = when (currentState) {
            is InterventionUiState.QuestionActive -> currentState.assessment
            is InterventionUiState.MaxPenalized -> currentState.assessment
            else -> return
        }

        val responseTime = System.currentTimeMillis() - questionStartTimeMs
        val isSuccess = enteredAnswer == question.correctAnswer

        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    interventionLogRepository.insertLog(
                        InterventionLog(
                            timestamp = System.currentTimeMillis(),
                            riskScore = assessment.riskScore,
                            difficultyLevel = assessment.level,
                            responseTimeMs = responseTime,
                            isSuccess = isSuccess,
                            penaltyAppliedMinutes = assessment.penaltyMinutes
                        )
                    )

                    val dateString = LocalDate.now().toString()
                    val existingLimit = adaptiveLimitRepository.getLimitForDate(dateString)
                    val updatedActualScreenTime = max(
                        existingLimit?.actualScreenTimeMinutes ?: 0,
                        currentScreenTimeMinutes.roundToInt()
                    )
                    adaptiveLimitRepository.insertOrUpdateLimit(
                        DailyAdaptiveLimit(
                            dateString = dateString,
                            calculatedLimitMinutes = assessment.calculatedLimitMinutes,
                            actualScreenTimeMinutes = updatedActualScreenTime,
                            reclaimedTimeMinutes = assessment.penaltyMinutes
                        )
                    )
                }
            }.getOrThrow()

            _uiState.value = if (isSuccess) {
                InterventionUiState.CorrectAnswer(
                    question = question,
                    assessment = assessment,
                    responseTimeMs = responseTime
                )
            } else {
                InterventionUiState.IncorrectAnswer(
                    question = question,
                    assessment = assessment,
                    enteredAnswer = enteredAnswer.toString(),
                    responseTimeMs = responseTime,
                    remainingBypasses = getRemainingBypasses()
                )
            }
        }
    }

    fun emergencyBypass() {
        val currentState = _uiState.value
        val assessment = when (currentState) {
            is InterventionUiState.QuestionActive -> currentState.assessment
            is InterventionUiState.MaxPenalized -> currentState.assessment
            is InterventionUiState.IncorrectAnswer -> currentState.assessment
            else -> return
        }

        val responseTime = System.currentTimeMillis() - questionStartTimeMs

        viewModelScope.launch {
            val remaining = getRemainingBypasses()
            if (remaining <= 0) {
                _uiState.value = when (val state = _uiState.value) {
                    is InterventionUiState.QuestionActive -> state.copy(bypassDenied = true, remainingBypasses = 0)
                    is InterventionUiState.MaxPenalized -> state.copy(bypassDenied = true, remainingBypasses = 0)
                    is InterventionUiState.IncorrectAnswer -> state.copy(remainingBypasses = 0)
                    else -> state
                }
                return@launch
            }

            runCatching {
                withContext(Dispatchers.IO) {
                    // 1. Log as bypassed
                    interventionLogRepository.insertLog(
                        InterventionLog(
                            timestamp = System.currentTimeMillis(),
                            riskScore = assessment.riskScore,
                            difficultyLevel = assessment.level,
                            responseTimeMs = responseTime,
                            isSuccess = false,
                            isBypassed = true,
                            penaltyAppliedMinutes = assessment.penaltyMinutes
                        )
                    )

                    // 2. Set bypass guard for 3 minutes
                    userPreferencesRepository.setEmergencyBypassUntil(
                        System.currentTimeMillis() + 180_000L
                    )
                }
            }.getOrThrow()
            
            _uiState.value = InterventionUiState.Idle // This will trigger onDismiss in Activity
        }
    }

    private suspend fun getRemainingBypasses(): Int {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val startOfDay = now.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = now.toLocalDate().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val count = interventionLogRepository.getBypassCountForDay(startOfDay, endOfDay)
        return maxOf(0, 5 - count)
    }

    fun resetToIdle() {
        _uiState.value = InterventionUiState.Idle
    }
}
