package com.makhp.pelukdiri.features.intervention

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makhp.pelukdiri.core.domain.engine.CognitiveQuestionGenerator
import com.makhp.pelukdiri.core.domain.model.DailyAdaptiveLimit
import com.makhp.pelukdiri.core.domain.model.InterventionLog
import com.makhp.pelukdiri.core.domain.model.RiskAssessmentResult
import com.makhp.pelukdiri.core.domain.repository.AdaptiveLimitRepository
import com.makhp.pelukdiri.core.domain.repository.InterventionLogRepository
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import com.makhp.pelukdiri.core.domain.usecase.BypassResult
import com.makhp.pelukdiri.core.domain.usecase.PerformEmergencyBypassUseCase
import com.makhp.pelukdiri.core.domain.time.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt
import javax.inject.Inject

@HiltViewModel
class InterventionViewModel @Inject constructor(
    private val cognitiveQuestionGenerator: CognitiveQuestionGenerator,
    private val interventionLogRepository: InterventionLogRepository,
    private val adaptiveLimitRepository: AdaptiveLimitRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val performEmergencyBypassUseCase: PerformEmergencyBypassUseCase,
    private val lockManager: com.makhp.pelukdiri.core.domain.InterventionLockManager,
    private val activeInterventionSession: ActiveInterventionSession,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<InterventionUiState>(InterventionUiState.Idle)
    val uiState: StateFlow<InterventionUiState> = _uiState.asStateFlow()

    private var questionJob: kotlinx.coroutines.Job? = null
    private var questionStartTimeMs: Long = 0L
    private var currentMonitoredUsageMinutes: Double = 0.0
    private var currentDeviation: Double = 0.0
    private var currentDifficultyControlSignal: Double = 0.0
    private var currentDifficulty: Int = 2
    private var currentLaunchFrequency: Int = 0
    private var currentAmbientLightLux: Float = 0f
    private var isBypassProcessing: Boolean = false
    private var sessionCreatedAtMs: Long = 0L

    fun startIntervention(
        monitoredUsageMinutes: Double,
        launchFrequency: Int,
        ambientLightLux: Float,
        deviation: Double,
        difficultyControlSignal: Double,
        difficulty: Int
    ) {
        // Cancel any pending generation to ensure state integrity
        questionJob?.cancel()

        currentMonitoredUsageMinutes = monitoredUsageMinutes
        currentDeviation = deviation
        currentDifficultyControlSignal = difficultyControlSignal
        currentDifficulty = difficulty
        currentLaunchFrequency = launchFrequency
        currentAmbientLightLux = ambientLightLux
        sessionCreatedAtMs = timeProvider.nowMillis()
        publishState(InterventionUiState.Loading)

        questionJob = viewModelScope.launch {
            val question = cognitiveQuestionGenerator.generateQuestion(difficulty)
            val dateString = timeProvider.today().toString()
            val adaptiveLimit = adaptiveLimitRepository.getLimitForDate(dateString)

            val assessment = RiskAssessmentResult(
                // RiskAssessmentResult is a legacy UI contract; this value is the
                // normalized difficulty-control signal in the v0.1 control flow.
                riskScore = difficultyControlSignal,
                level = difficulty,
                penaltyMinutes = 0, // Penalty is not explicitly defined in v0.1 trig logic
                calculatedLimitMinutes = adaptiveLimit?.calculatedLimitMinutes ?: 0
            )

            val remaining = getRemainingBypasses()
            questionStartTimeMs = timeProvider.nowMillis()
            publishState(InterventionUiState.QuestionActive(
                question = question,
                assessment = assessment,
                remainingBypasses = remaining
            ))
        }
    }

    fun restoreActiveIntervention(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val snapshot = activeInterventionSession.restore()
            if (snapshot == null) {
                onResult(false)
                return@launch
            }

        currentMonitoredUsageMinutes = snapshot.monitoredUsageMinutes
        currentLaunchFrequency = snapshot.launchFrequency
        currentAmbientLightLux = snapshot.ambientLightLux
        currentDeviation = snapshot.deviation
        currentDifficultyControlSignal = snapshot.difficultyControlSignal
        currentDifficulty = snapshot.difficulty
        questionStartTimeMs = snapshot.questionStartTimeMs
        sessionCreatedAtMs = snapshot.createdAtMs

        if (snapshot.uiState is InterventionUiState.Loading) {
            startIntervention(
                monitoredUsageMinutes = snapshot.monitoredUsageMinutes,
                launchFrequency = snapshot.launchFrequency,
                ambientLightLux = snapshot.ambientLightLux,
                deviation = snapshot.deviation,
                difficultyControlSignal = snapshot.difficultyControlSignal,
                difficulty = snapshot.difficulty
            )
        } else {
            _uiState.value = snapshot.uiState
        }
            onResult(true)
        }
    }

    fun onAnswerChanged(rawInput: String) {
        val state = _uiState.value
        val question = when (state) {
            is InterventionUiState.QuestionActive -> state.question
            is InterventionUiState.MaxPenalized -> state.question
            else -> return
        }
        val maxDigits = question.correctAnswer
            .toString()
            .count { it.isDigit() }
            .coerceAtLeast(1)
        val sanitizedInput = rawInput
            .filter { it.isDigit() }
            .take(maxDigits)

        publishState(when (state) {
            is InterventionUiState.QuestionActive -> state.copy(answerInput = sanitizedInput)
            is InterventionUiState.MaxPenalized -> state.copy(answerInput = sanitizedInput)
            else -> state
        })
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

        val responseTime = timeProvider.nowMillis() - questionStartTimeMs
        val isSuccess = enteredAnswer == question.correctAnswer

        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    interventionLogRepository.insertLog(
                        InterventionLog(
                            timestamp = timeProvider.nowMillis(),
                            deviation = currentDeviation,
                            difficultyControlSignal = currentDifficultyControlSignal,
                            difficultyLevel = currentDifficulty,
                            responseTimeMs = responseTime,
                            isSuccess = isSuccess,
                            penaltyAppliedMinutes = assessment.penaltyMinutes
                        )
                    )

                    val dateString = timeProvider.today().toString()
                    val existingLimit = adaptiveLimitRepository.getLimitForDate(dateString)
                    val updatedActualScreenTime = max(
                        existingLimit?.actualScreenTimeMinutes ?: 0,
                        currentMonitoredUsageMinutes.roundToInt()
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

            publishState(if (isSuccess) {
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
            })
        }
    }

    fun emergencyBypass() {
        if (isBypassProcessing) return

        val currentState = _uiState.value
        val assessment = when (currentState) {
            is InterventionUiState.QuestionActive -> currentState.assessment
            is InterventionUiState.MaxPenalized -> currentState.assessment
            is InterventionUiState.IncorrectAnswer -> currentState.assessment
            else -> return
        }

        val responseTime = timeProvider.nowMillis() - questionStartTimeMs
        isBypassProcessing = true

        viewModelScope.launch {
            try {
                val result = performEmergencyBypassUseCase(
                    deviation = currentDeviation,
                    difficultyControlSignal = currentDifficultyControlSignal,
                    difficulty = currentDifficulty,
                    penaltyMinutes = assessment.penaltyMinutes,
                    responseTimeMs = responseTime
                )

                when (result) {
                    is BypassResult.Success -> {
                        publishState(InterventionUiState.Idle) // Triggers onDismiss
                        lockManager.releaseLock()
                    }
                    BypassResult.Exhausted -> {
                        publishState(when (val state = _uiState.value) {
                            is InterventionUiState.QuestionActive -> state.copy(bypassDenied = true, remainingBypasses = 0)
                            is InterventionUiState.MaxPenalized -> state.copy(bypassDenied = true, remainingBypasses = 0)
                            is InterventionUiState.IncorrectAnswer -> state.copy(remainingBypasses = 0)
                            else -> state
                        })
                    }
                }
            } finally {
                isBypassProcessing = false
            }
        }
    }

    private suspend fun getRemainingBypasses(): Int {
        val today = timeProvider.today()
        val startOfDay = today.atStartOfDay(timeProvider.zoneId()).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(timeProvider.zoneId()).toInstant().toEpochMilli()
        val count = interventionLogRepository.getBypassCountForDay(startOfDay, endOfDay)
        return maxOf(0, 5 - count)
    }

    fun resetToIdle() {
        publishState(InterventionUiState.Idle)
        lockManager.releaseLock()
    }

    private fun publishState(state: InterventionUiState) {
        _uiState.value = state
        if (state is InterventionUiState.Idle) {
            viewModelScope.launch { activeInterventionSession.clear() }
            return
        }

        val now = timeProvider.nowMillis()
        if (sessionCreatedAtMs == 0L) sessionCreatedAtMs = now
        val snapshot = ActiveInterventionSnapshot(
                uiState = state,
                monitoredUsageMinutes = currentMonitoredUsageMinutes,
                launchFrequency = currentLaunchFrequency,
                ambientLightLux = currentAmbientLightLux,
                deviation = currentDeviation,
                difficultyControlSignal = currentDifficultyControlSignal,
                difficulty = currentDifficulty,
                questionStartTimeMs = questionStartTimeMs,
                createdAtMs = sessionCreatedAtMs,
                expiresAtMs = sessionCreatedAtMs + ActiveInterventionSession.TTL_MS
        )
        viewModelScope.launch { activeInterventionSession.save(snapshot) }
    }
}
