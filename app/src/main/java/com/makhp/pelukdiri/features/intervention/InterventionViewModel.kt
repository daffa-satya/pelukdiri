package com.makhp.pelukdiri.features.intervention

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makhp.pelukdiri.core.domain.engine.CognitiveQuestionGenerator
import com.makhp.pelukdiri.core.domain.engine.CooldownAnchor
import com.makhp.pelukdiri.core.domain.engine.InterventionChallengeSelector
import com.makhp.pelukdiri.core.domain.engine.InterventionChallengeType
import com.makhp.pelukdiri.core.domain.engine.PatternQuestionGenerator
import com.makhp.pelukdiri.core.domain.model.DailyAdaptiveLimit
import com.makhp.pelukdiri.core.domain.model.InterventionLog
import com.makhp.pelukdiri.core.domain.model.PatternShape
import com.makhp.pelukdiri.core.domain.model.RiskAssessmentResult
import com.makhp.pelukdiri.core.domain.repository.AdaptiveLimitRepository
import com.makhp.pelukdiri.core.domain.repository.InterventionLogRepository
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import com.makhp.pelukdiri.core.domain.usecase.BypassResult
import com.makhp.pelukdiri.core.domain.usecase.PerformEmergencyBypassUseCase
import com.makhp.pelukdiri.core.domain.time.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt
import javax.inject.Inject

@HiltViewModel
class InterventionViewModel @Inject constructor(
    private val cognitiveQuestionGenerator: CognitiveQuestionGenerator,
    private val patternQuestionGenerator: PatternQuestionGenerator,
    private val challengeSelector: InterventionChallengeSelector,
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
    private var patternPlaybackJob: Job? = null
    private var questionStartTimeMs: Long = 0L
    private var currentMonitoredUsageMinutes: Double = 0.0
    private var currentDeviation: Double = 0.0
    private var currentDifficultyControlSignal: Double = 0.0
    private var currentDifficulty: Int = 2
    private var currentIntervalMinutesAtLaunch: Double = 0.0
    private var currentAmbientLightLuxAtLaunch: Float = 0f
    private var currentChallengeType = InterventionChallengeType.MATH
    private var isBypassProcessing: Boolean = false
    private var isBypassCommitted: Boolean = false
    private var isAnswerProcessing: Boolean = false
    private var isCompletionProcessing: Boolean = false
    private var pendingCompletionEligibleAtMs: Long? = null
    private var sessionCreatedAtMs: Long = 0L
    private var stateBeforeError: InterventionUiState? = null

    fun startIntervention(
        monitoredUsageMinutes: Double,
        intervalMinutesAtLaunch: Double,
        ambientLightLuxAtLaunch: Float,
        deviation: Double,
        difficultyControlSignal: Double,
        difficulty: Int,
        challengeType: InterventionChallengeType = InterventionChallengeType.AUTO,
    ) {
        // Cancel any pending generation to ensure state integrity
        questionJob?.cancel()
        patternPlaybackJob?.cancel()

        currentMonitoredUsageMinutes = monitoredUsageMinutes
        currentDeviation = deviation
        currentDifficultyControlSignal = difficultyControlSignal
        currentDifficulty = difficulty
        currentIntervalMinutesAtLaunch = intervalMinutesAtLaunch
        currentAmbientLightLuxAtLaunch = ambientLightLuxAtLaunch
        currentChallengeType = challengeType.takeUnless { it == InterventionChallengeType.AUTO }
            ?: challengeSelector.select()
        sessionCreatedAtMs = timeProvider.nowMillis()
        isBypassCommitted = false
        pendingCompletionEligibleAtMs = null
        stateBeforeError = null
        publishState(InterventionUiState.Loading)

        questionJob = viewModelScope.launch {
            try {
                val dateString = timeProvider.today().toString()
                val adaptiveLimit = adaptiveLimitRepository.getLimitForDate(dateString)

                val assessment = RiskAssessmentResult(
                    // RiskAssessmentResult is a legacy UI contract; this value is the
                    // normalized difficulty-control signal in the v0.1 control flow.
                    riskScore = difficultyControlSignal,
                    level = difficulty,
                    penaltyMinutes = 0,
                    calculatedLimitMinutes = adaptiveLimit?.calculatedLimitMinutes ?: 0
                )

                val remaining = getRemainingBypasses()
                questionStartTimeMs = timeProvider.nowMillis()
                if (currentChallengeType == InterventionChallengeType.PATTERN) {
                    startPatternPlayback(
                        InterventionUiState.PatternActive(
                            question = patternQuestionGenerator.generateQuestion(difficulty),
                            assessment = assessment,
                            remainingBypasses = remaining,
                        ),
                        resetResponseTimer = true,
                    )
                } else {
                    publishState(
                        InterventionUiState.QuestionActive(
                            question = cognitiveQuestionGenerator.generateQuestion(difficulty),
                            assessment = assessment,
                            remainingBypasses = remaining,
                        )
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                showOperationError(FailedInterventionOperation.START)
            }
        }
    }

    fun restoreActiveIntervention(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val snapshot = activeInterventionSession.restore()
                if (snapshot == null) {
                    onResult(false)
                    return@launch
                }

                currentMonitoredUsageMinutes = snapshot.monitoredUsageMinutes
                currentIntervalMinutesAtLaunch = snapshot.intervalMinutesAtLaunch
                currentAmbientLightLuxAtLaunch = snapshot.ambientLightLuxAtLaunch
                currentDeviation = snapshot.deviation
                currentDifficultyControlSignal = snapshot.difficultyControlSignal
                currentDifficulty = snapshot.difficulty
                questionStartTimeMs = snapshot.questionStartTimeMs
                sessionCreatedAtMs = snapshot.createdAtMs
                currentChallengeType = when (snapshot.uiState) {
                    is InterventionUiState.PatternActive,
                    is InterventionUiState.PatternCorrectAnswer,
                    is InterventionUiState.PatternIncorrectAnswer -> InterventionChallengeType.PATTERN
                    else -> InterventionChallengeType.MATH
                }

                when (val state = snapshot.uiState) {
                    InterventionUiState.Loading -> startIntervention(
                        monitoredUsageMinutes = snapshot.monitoredUsageMinutes,
                        intervalMinutesAtLaunch = snapshot.intervalMinutesAtLaunch,
                        ambientLightLuxAtLaunch = snapshot.ambientLightLuxAtLaunch,
                        deviation = snapshot.deviation,
                        difficultyControlSignal = snapshot.difficultyControlSignal,
                        difficulty = snapshot.difficulty,
                        challengeType = currentChallengeType,
                    )
                    is InterventionUiState.PatternActive -> {
                        if (state.isPlaying) {
                            startPatternPlayback(state, resetResponseTimer = state.replaysRemaining > 0)
                        } else {
                            _uiState.value = state
                        }
                    }
                    else -> _uiState.value = state
                }
                onResult(true)
            } catch (_: Exception) {
                showOperationError(FailedInterventionOperation.RESTORE)
                onResult(true)
            }
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

    fun onPatternSelected(shape: PatternShape) {
        val state = _uiState.value as? InterventionUiState.PatternActive ?: return
        if (state.isPlaying || isAnswerProcessing || state.answerInput.size >= state.question.sequence.size) return

        val updated = state.copy(answerInput = state.answerInput + shape)
        publishState(updated)
        if (updated.answerInput.size == updated.question.sequence.size) {
            submitPatternAnswer(updated)
        }
    }

    fun replayPattern() {
        val state = _uiState.value as? InterventionUiState.PatternActive ?: return
        if (!isAnswerProcessing && !state.isPlaying) {
            startPatternPlayback(
                // Keep zero as a persisted marker that this is a replay, while
                // allowing any number of subsequent replays.
                state.copy(answerInput = emptyList(), replaysRemaining = 0),
                resetResponseTimer = false,
            )
        }
    }

    private fun startPatternPlayback(
        state: InterventionUiState.PatternActive,
        resetResponseTimer: Boolean,
    ) {
        patternPlaybackJob?.cancel()
        val resetState = state.copy(answerInput = emptyList(), isPlaying = true, playbackIndex = null)
        publishState(resetState)
        patternPlaybackJob = viewModelScope.launch {
            delay(PATTERN_PREPARATION_MS)
            resetState.question.sequence.indices.forEach { index ->
                publishState(resetState.copy(playbackIndex = index))
                delay(PATTERN_HIGHLIGHT_MS)
                publishState(resetState.copy(playbackIndex = null))
                delay(PATTERN_GAP_MS)
            }
            if (resetResponseTimer) questionStartTimeMs = timeProvider.nowMillis()
            publishState(resetState.copy(isPlaying = false, playbackIndex = null))
        }
    }

    private fun submitPatternAnswer(state: InterventionUiState.PatternActive) {
        if (isAnswerProcessing) return
        val responseTime = (timeProvider.nowMillis() - questionStartTimeMs).coerceAtLeast(0L)
        val isSuccess = state.answerInput == state.question.sequence
        isAnswerProcessing = true
        viewModelScope.launch {
            try {
                persistOutcome(isSuccess, state.assessment, responseTime)
                publishState(
                    if (isSuccess) {
                        InterventionUiState.PatternCorrectAnswer(
                            state.question, state.assessment, responseTime
                        )
                    } else {
                        InterventionUiState.PatternIncorrectAnswer(
                            state.question,
                            state.assessment,
                            state.answerInput,
                            responseTime,
                            getRemainingBypasses(),
                        )
                    }
                )
            } catch (_: Exception) {
                showOperationError(FailedInterventionOperation.SUBMIT_ANSWER)
            } finally {
                isAnswerProcessing = false
            }
        }
    }

    fun submitAnswer() {
        if (isAnswerProcessing) return

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

        isAnswerProcessing = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
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

                    // Keep the append-only action log last. The adaptive-limit upsert is
                    // idempotent, so retrying an earlier failure cannot duplicate an answer.
                    interventionLogRepository.insertLog(
                        InterventionLog(
                            timestamp = timeProvider.nowMillis(),
                            deviation = currentDeviation,
                            difficultyControlSignal = currentDifficultyControlSignal,
                            difficultyLevel = currentDifficulty,
                            responseTimeMs = responseTime,
                            isSuccess = isSuccess,
                            penaltyAppliedMinutes = assessment.penaltyMinutes,
                            challengeType = currentChallengeType,
                        )
                    )
                }

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
            } catch (_: Exception) {
                showOperationError(FailedInterventionOperation.SUBMIT_ANSWER)
            } finally {
                isAnswerProcessing = false
            }
        }
    }

    private suspend fun persistOutcome(
        isSuccess: Boolean,
        assessment: RiskAssessmentResult,
        responseTime: Long,
    ) = withContext(Dispatchers.IO) {
        val dateString = timeProvider.today().toString()
        val existingLimit = adaptiveLimitRepository.getLimitForDate(dateString)
        adaptiveLimitRepository.insertOrUpdateLimit(
            DailyAdaptiveLimit(
                dateString = dateString,
                calculatedLimitMinutes = assessment.calculatedLimitMinutes,
                actualScreenTimeMinutes = max(
                    existingLimit?.actualScreenTimeMinutes ?: 0,
                    currentMonitoredUsageMinutes.roundToInt(),
                ),
                reclaimedTimeMinutes = assessment.penaltyMinutes,
            )
        )
        interventionLogRepository.insertLog(
            InterventionLog(
                timestamp = timeProvider.nowMillis(),
                deviation = currentDeviation,
                difficultyControlSignal = currentDifficultyControlSignal,
                difficultyLevel = currentDifficulty,
                responseTimeMs = responseTime,
                isSuccess = isSuccess,
                penaltyAppliedMinutes = assessment.penaltyMinutes,
                challengeType = currentChallengeType,
            )
        )
    }

    fun emergencyBypass() {
        if (isBypassProcessing) return

        val currentState = _uiState.value
        val assessment = when (currentState) {
            is InterventionUiState.QuestionActive -> currentState.assessment
            is InterventionUiState.MaxPenalized -> currentState.assessment
            is InterventionUiState.IncorrectAnswer -> currentState.assessment
            is InterventionUiState.PatternActive -> currentState.assessment
            is InterventionUiState.PatternIncorrectAnswer -> currentState.assessment
            else -> return
        }

        val responseTime = timeProvider.nowMillis() - questionStartTimeMs
        isBypassProcessing = true

        viewModelScope.launch {
            try {
                if (isBypassCommitted) {
                    finishSuccessfulBypass()
                    return@launch
                }

                val result = performEmergencyBypassUseCase(
                        deviation = currentDeviation,
                        difficultyControlSignal = currentDifficultyControlSignal,
                        difficulty = currentDifficulty,
                        penaltyMinutes = assessment.penaltyMinutes,
                        responseTimeMs = responseTime,
                        challengeType = currentChallengeType,
                    )

                when (result) {
                    is BypassResult.Success -> {
                        isBypassCommitted = true
                        finishSuccessfulBypass()
                    }
                    BypassResult.Exhausted -> {
                        publishState(when (val state = _uiState.value) {
                            is InterventionUiState.QuestionActive -> state.copy(bypassDenied = true, remainingBypasses = 0)
                            is InterventionUiState.MaxPenalized -> state.copy(bypassDenied = true, remainingBypasses = 0)
                            is InterventionUiState.IncorrectAnswer -> state.copy(remainingBypasses = 0)
                            is InterventionUiState.PatternActive -> state.copy(bypassDenied = true, remainingBypasses = 0)
                            is InterventionUiState.PatternIncorrectAnswer -> state.copy(remainingBypasses = 0)
                            else -> state
                        })
                    }
                }
            } catch (_: Exception) {
                showOperationError(FailedInterventionOperation.EMERGENCY_BYPASS)
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
        if (isCompletionProcessing) return
        isCompletionProcessing = true
        viewModelScope.launch {
            try {
                val eligibleAt = pendingCompletionEligibleAtMs ?: run {
                    val originallyEligibleAt = userPreferencesRepository.nextEligibleInterventionAt.first()
                    CooldownAnchor.afterCompletion(
                        sessionCreatedAtMs = sessionCreatedAtMs,
                        originallyEligibleAtMs = originallyEligibleAt,
                        completedAtMs = timeProvider.nowMillis(),
                    ).also { pendingCompletionEligibleAtMs = it }
                }
                eligibleAt?.let { userPreferencesRepository.setNextEligibleInterventionAt(it) }
                activeInterventionSession.clear()
                publishState(InterventionUiState.Idle)
                lockManager.releaseLock()
            } catch (_: Exception) {
                showOperationError(FailedInterventionOperation.COMPLETE)
            } finally {
                isCompletionProcessing = false
            }
        }
    }

    fun retryLastOperation() {
        val errorState = _uiState.value as? InterventionUiState.Error ?: return
        val previousState = stateBeforeError
        if (previousState != null) _uiState.value = previousState

        when (errorState.operation) {
            FailedInterventionOperation.START -> startIntervention(
                currentMonitoredUsageMinutes,
                currentIntervalMinutesAtLaunch,
                currentAmbientLightLuxAtLaunch,
                currentDeviation,
                currentDifficultyControlSignal,
                currentDifficulty,
                currentChallengeType,
            )
            FailedInterventionOperation.RESTORE -> restoreActiveIntervention { }
            FailedInterventionOperation.SUBMIT_ANSWER -> when (val state = _uiState.value) {
                is InterventionUiState.PatternActive -> submitPatternAnswer(state)
                else -> submitAnswer()
            }
            FailedInterventionOperation.EMERGENCY_BYPASS -> emergencyBypass()
            FailedInterventionOperation.COMPLETE -> resetToIdle()
        }
    }

    private suspend fun finishSuccessfulBypass() {
        activeInterventionSession.clear()
        publishState(InterventionUiState.Idle)
        lockManager.releaseLock()
    }

    private fun showOperationError(operation: FailedInterventionOperation) {
        Log.e(TAG, "Intervention operation failed")
        if (_uiState.value !is InterventionUiState.Error) {
            stateBeforeError = _uiState.value
        }
        _uiState.value = InterventionUiState.Error(operation)
    }

    private fun publishState(state: InterventionUiState) {
        _uiState.value = state
        if (state is InterventionUiState.Idle) {
            return
        }

        val now = timeProvider.nowMillis()
        if (sessionCreatedAtMs == 0L) sessionCreatedAtMs = now
        val snapshot = ActiveInterventionSnapshot(
                uiState = state,
                monitoredUsageMinutes = currentMonitoredUsageMinutes,
                intervalMinutesAtLaunch = currentIntervalMinutesAtLaunch,
                ambientLightLuxAtLaunch = currentAmbientLightLuxAtLaunch,
                deviation = currentDeviation,
                difficultyControlSignal = currentDifficultyControlSignal,
                difficulty = currentDifficulty,
                questionStartTimeMs = questionStartTimeMs,
                createdAtMs = sessionCreatedAtMs,
                expiresAtMs = sessionCreatedAtMs + ActiveInterventionSession.TTL_MS
        )
        viewModelScope.launch {
            try {
                activeInterventionSession.save(snapshot)
            } catch (_: Exception) {
                Log.e(TAG, "Unable to persist active intervention snapshot")
            }
        }
    }

    internal companion object {
        const val TAG = "InterventionViewModel"
        const val PATTERN_PREPARATION_MS = 1_000L
        const val PATTERN_HIGHLIGHT_MS = 500L
        const val PATTERN_GAP_MS = 100L
    }
}
