package com.makhp.pelukdiri.features.intervention

import com.makhp.pelukdiri.core.domain.InterventionLockManager
import com.makhp.pelukdiri.core.domain.engine.CognitiveQuestionGenerator
import com.makhp.pelukdiri.core.domain.model.MathQuestion
import com.makhp.pelukdiri.core.domain.repository.AdaptiveLimitRepository
import com.makhp.pelukdiri.core.domain.repository.InterventionLogRepository
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import com.makhp.pelukdiri.core.domain.usecase.PerformEmergencyBypassUseCase
import com.makhp.pelukdiri.core.domain.usecase.BypassResult
import com.makhp.pelukdiri.core.domain.time.TimeProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InterventionViewModelTest {

    private lateinit var viewModel: InterventionViewModel
    private val cognitiveQuestionGenerator: CognitiveQuestionGenerator = mockk()
    private val interventionLogRepository: InterventionLogRepository = mockk()
    private val adaptiveLimitRepository: AdaptiveLimitRepository = mockk()
    private val userPreferencesRepository: UserPreferencesRepository = mockk()
    private val performEmergencyBypassUseCase: PerformEmergencyBypassUseCase = mockk()
    private val lockManager = InterventionLockManager()
    private lateinit var activeInterventionSession: ActiveInterventionSession
    private val timeProvider = object : TimeProvider {
        override fun nowMillis() = 1_800_000_000_000L
        override fun zoneId() = java.time.ZoneId.of("Asia/Jakarta")
    }

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        coEvery { interventionLogRepository.getBypassCountForDay(any(), any()) } returns 0
        coEvery { userPreferencesRepository.setActiveInterventionSession(any()) } returns Unit
        coEvery { userPreferencesRepository.activeInterventionSession } returns flowOf(null)
        activeInterventionSession = ActiveInterventionSession(userPreferencesRepository, timeProvider, lockManager)

        viewModel = InterventionViewModel(
            cognitiveQuestionGenerator,
            interventionLogRepository,
            adaptiveLimitRepository,
            userPreferencesRepository,
            performEmergencyBypassUseCase,
            lockManager,
            activeInterventionSession,
            timeProvider
        )
    }

    @Test
    fun `startIntervention cancels previous job and resets state`() = runTest {
        // GIVEN: A pending question generation
        coEvery { cognitiveQuestionGenerator.generateQuestion(any()) } coAnswers {
            delay(1000) // Simulate long work
            MathQuestion("1+1", 2, 1)
        }
        coEvery { adaptiveLimitRepository.getLimitForDate(any()) } returns null

        // WHEN: Start first intervention
        viewModel.startIntervention(10.0, 1, 100f, 0.1, 0.5, 1)
        
        // AND: Start second intervention immediately with different data
        viewModel.startIntervention(20.0, 2, 200f, 0.2, 0.6, 2)
        
        advanceUntilIdle()

        // THEN: The state should reflect the SECOND call's data
        val finalState = viewModel.uiState.value
        assertTrue(finalState is InterventionUiState.QuestionActive)
        val activeState = finalState as InterventionUiState.QuestionActive
        assertEquals(0.6, activeState.assessment.riskScore, 0.001)
        assertEquals(2, activeState.assessment.level)
    }

    @Test
    fun `replacement ViewModel restores exact active question and input`() = runTest {
        val question = MathQuestion("43 + 47", 90, 1)
        coEvery { cognitiveQuestionGenerator.generateQuestion(1) } returns question
        coEvery { adaptiveLimitRepository.getLimitForDate(any()) } returns null

        viewModel.startIntervention(130.0, 4, 25f, 0.08, 0.07, 1)
        advanceUntilIdle()
        viewModel.onAnswerChanged("9")
        val originalState = viewModel.uiState.value

        val replacement = InterventionViewModel(
            cognitiveQuestionGenerator,
            interventionLogRepository,
            adaptiveLimitRepository,
            userPreferencesRepository,
            performEmergencyBypassUseCase,
            lockManager,
            activeInterventionSession,
            timeProvider
        )

        var restored = false
        replacement.restoreActiveIntervention { restored = it }
        advanceUntilIdle()
        assertTrue(restored)
        assertEquals(originalState, replacement.uiState.value)
    }

    @Test
    fun `answer input is capped to the generated answer digit count`() = runTest {
        coEvery { cognitiveQuestionGenerator.generateQuestion(1) } returns
            MathQuestion("43 + 47", 90, 1)
        coEvery { adaptiveLimitRepository.getLimitForDate(any()) } returns null

        viewModel.startIntervention(130.0, 4, 25f, 0.08, 0.07, 1)
        advanceUntilIdle()
        viewModel.onAnswerChanged("123456789")

        val state = viewModel.uiState.value as InterventionUiState.QuestionActive
        assertEquals("12", state.answerInput)
    }

    @Test
    fun `start failure shows retryable error and recovers`() = runTest {
        coEvery { cognitiveQuestionGenerator.generateQuestion(1) } throws
            IllegalStateException("generator unavailable") andThen
            MathQuestion("1+1", 2, 1)
        coEvery { adaptiveLimitRepository.getLimitForDate(any()) } returns null

        viewModel.startIntervention(10.0, 1, 100f, 0.1, 0.5, 1)
        advanceUntilIdle()

        val error = viewModel.uiState.value as InterventionUiState.Error
        assertEquals(FailedInterventionOperation.START, error.operation)

        viewModel.retryLastOperation()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is InterventionUiState.QuestionActive)
    }

    @Test
    fun `failed answer preserves state and retry does not create an extra successful insert`() = runTest {
        coEvery { cognitiveQuestionGenerator.generateQuestion(1) } returns MathQuestion("1+1", 2, 1)
        coEvery { adaptiveLimitRepository.getLimitForDate(any()) } returns null
        coEvery { adaptiveLimitRepository.insertOrUpdateLimit(any()) } returns Unit
        coEvery { interventionLogRepository.insertLog(any()) } throws
            IllegalStateException("database unavailable") andThen Unit

        viewModel.startIntervention(10.0, 1, 100f, 0.1, 0.5, 1)
        advanceUntilIdle()
        viewModel.onAnswerChanged("2")
        viewModel.submitAnswer()

        val error = viewModel.uiState.filterIsInstance<InterventionUiState.Error>().first()
        assertEquals(FailedInterventionOperation.SUBMIT_ANSWER, error.operation)

        viewModel.retryLastOperation()
        viewModel.uiState.filterIsInstance<InterventionUiState.CorrectAnswer>().first()
        coVerify(exactly = 2) { interventionLogRepository.insertLog(any()) }
    }

    @Test
    fun `failed bypass restores the same action and succeeds on retry`() = runTest {
        coEvery { cognitiveQuestionGenerator.generateQuestion(1) } returns MathQuestion("1+1", 2, 1)
        coEvery { adaptiveLimitRepository.getLimitForDate(any()) } returns null
        coEvery { performEmergencyBypassUseCase(any(), any(), any(), any(), any()) } throws
            IllegalStateException("preferences unavailable") andThen BypassResult.Success(4)

        viewModel.startIntervention(10.0, 1, 100f, 0.1, 0.5, 1)
        advanceUntilIdle()
        viewModel.emergencyBypass()
        advanceUntilIdle()

        val error = viewModel.uiState.value as InterventionUiState.Error
        assertEquals(FailedInterventionOperation.EMERGENCY_BYPASS, error.operation)

        viewModel.retryLastOperation()
        advanceUntilIdle()
        assertEquals(InterventionUiState.Idle, viewModel.uiState.value)
        coVerify(exactly = 2) {
            performEmergencyBypassUseCase(any(), any(), any(), any(), any())
        }
    }
}
