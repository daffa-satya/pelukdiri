package com.makhp.pelukdiri.core.domain.usecase

import com.makhp.pelukdiri.collector.AppUsageCollector
import com.makhp.pelukdiri.collector.UsageEventCollector
import com.makhp.pelukdiri.core.domain.InterventionLockManager
import com.makhp.pelukdiri.core.domain.engine.ControlEngine
import com.makhp.pelukdiri.core.domain.engine.DeviationEngine
import com.makhp.pelukdiri.core.domain.engine.InterventionChallengeSelector
import com.makhp.pelukdiri.core.domain.engine.InterventionChallengeType
import com.makhp.pelukdiri.core.domain.model.AppUsage
import com.makhp.pelukdiri.core.domain.model.ControlMode
import com.makhp.pelukdiri.core.domain.model.ControlConfig
import com.makhp.pelukdiri.core.domain.model.ControlResult
import com.makhp.pelukdiri.core.domain.model.DeviationResult
import com.makhp.pelukdiri.core.domain.model.DeviationStatus
import com.makhp.pelukdiri.core.domain.model.DifficultyHistoryEntry
import com.makhp.pelukdiri.core.domain.model.InterventionDecision
import com.makhp.pelukdiri.core.domain.model.InterventionDecisionAudit
import com.makhp.pelukdiri.core.domain.model.InterventionDecisionReason
import com.makhp.pelukdiri.core.domain.model.InterventionLog
import com.makhp.pelukdiri.core.domain.model.HistoricalConfig
import com.makhp.pelukdiri.core.domain.repository.InterventionLogRepository
import com.makhp.pelukdiri.core.domain.repository.InterventionDecisionRepository
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EvaluateInterventionEligibilityUseCaseTest {

    private val targetPackage = "com.example.target"

    private lateinit var useCase: EvaluateInterventionEligibilityUseCase
    private val usageEventCollector: UsageEventCollector = mockk()
    private val userPreferencesRepository: UserPreferencesRepository = mockk()
    private val getAdaptiveHistoryUseCase: GetAdaptiveHistoryUseCase = mockk()
    private val deviationEngine: DeviationEngine = mockk()
    private val controlEngine: ControlEngine = mockk()
    private val interventionLogRepository: InterventionLogRepository = mockk()
    private val interventionDecisionRepository: InterventionDecisionRepository = mockk()
    private val challengeSelector: InterventionChallengeSelector = mockk()
    private val appUsageCollector: AppUsageCollector = mockk()
    private val lockManager = InterventionLockManager()

    @Before
    fun setup() {
        coEvery { appUsageCollector.getCurrentAmbientLightLux() } returns 100f
        every { userPreferencesRepository.currentDifficulty } returns flowOf(2)
        coEvery { userPreferencesRepository.setNextEligibleInterventionAt(any()) } returns Unit
        coEvery { interventionDecisionRepository.insert(any()) } returns Unit
        every { challengeSelector.select() } returns InterventionChallengeType.MATH
        useCase = EvaluateInterventionEligibilityUseCase(
            usageEventCollector,
            userPreferencesRepository,
            getAdaptiveHistoryUseCase,
            deviationEngine,
            controlEngine,
            interventionLogRepository,
            interventionDecisionRepository,
            challengeSelector,
            appUsageCollector,
            lockManager,
            ControlConfig.CANDIDATE_3,
        )
        every { appUsageCollector.getCurrentAmbientLightLux() } returns 100f
    }

    @Test
    fun `returns shouldTrigger false when lock is held`() = runBlocking {
        // GIVEN: Lock is acquired
        lockManager.acquireLock()

        // WHEN: Invoke use case
        val result: InterventionDecision = useCase("com.example.app")

        // THEN: shouldTrigger is false
        assertFalse(result.shouldTrigger)
        coVerify {
            interventionDecisionRepository.insert(match {
                it.reason == InterventionDecisionReason.ACTIVE_LOCK && !it.shouldTrigger
            })
        }
    }

    @Test
    fun `authoritative usage is not inflated by a second copy of the active session`() = runBlocking {
        stubEligibleEvaluation(
            usage = listOf(
                AppUsage(targetPackage, "Target", 10 * 60_000L, 0L),
                AppUsage("com.example.other", "Other", 5 * 60_000L, 0L)
            )
        )

        val result = useCase(targetPackage)

        assertEquals(10.0, result.monitoredUsageMinutes, 0.0001)
        assertEquals(15.0, result.totalUsageMinutes, 0.0001)
        assertTrue(result.shouldTrigger)
        verify { deviationEngine.calculate(10.0, List(HistoricalConfig.HISTORY_SAMPLE_DAYS) { 10.0 }) }
    }

    @Test
    fun `triggered decision persists complete explainability telemetry`() = runBlocking {
        val captured = slot<InterventionDecisionAudit>()
        coEvery { interventionDecisionRepository.insert(capture(captured)) } returns Unit
        stubEligibleEvaluation()

        useCase(targetPackage)

        val audit = captured.captured
        assertEquals(InterventionDecisionReason.TRIGGERED, audit.reason)
        assertEquals(10.0, audit.baselineMedianMinutes!!, 0.0001)
        assertEquals(1.0, audit.madMinutes!!, 0.0001)
        assertEquals(0.2, audit.deviation!!, 0.0001)
        assertEquals(0.5, audit.performance!!, 0.0001)
        assertEquals(0.1, audit.difficultyControlSignal!!, 0.0001)
        assertEquals(24.6, audit.proposedIntervalMinutes!!, 0.0001)
        assertEquals(InterventionChallengeType.MATH, audit.challengeType)
        assertTrue(audit.shouldTrigger)
    }

    @Test
    fun `first monitored evaluation schedules an intervention instead of launching immediately`() = runBlocking {
        stubEligibleEvaluation(nextEligibleAt = 0L)

        val result = useCase(targetPackage)

        assertFalse(result.shouldTrigger)
        coVerify(exactly = 1) { userPreferencesRepository.setNextEligibleInterventionAt(1_000L) }
        coVerify {
            interventionDecisionRepository.insert(match {
                it.reason == InterventionDecisionReason.INTERVAL_SCHEDULED &&
                    it.nextEligibleAt == 1_000L && !it.shouldTrigger
            })
        }
    }

    @Test
    fun `expired interval triggers even when deviation is below old threshold`() = runBlocking {
        stubEligibleEvaluation(nextEligibleAt = 1L)
        every { deviationEngine.calculate(any(), any()) } returns DeviationResult(
            deviation = 0.0,
            baseline = 10.0,
            mad = 1.0,
            signal = 0.0,
            relativeDeviation = 0.0,
            relativeMagnitude = 0.0,
            status = DeviationStatus.Success,
        )

        val result = useCase(targetPackage)

        assertTrue(result.shouldTrigger)
        coVerify(exactly = 0) { userPreferencesRepository.setNextEligibleInterventionAt(any()) }
        coVerify {
            interventionDecisionRepository.insert(match {
                it.reason == InterventionDecisionReason.TRIGGERED &&
                    it.deviation == 0.0 && it.shouldTrigger
            })
        }
    }

    @Test
    fun `expired interval triggers with insufficient history and null deviation`() = runBlocking {
        stubEligibleEvaluation(nextEligibleAt = 1L)
        coEvery { getAdaptiveHistoryUseCase() } returns emptyList()
        every { deviationEngine.calculate(any(), emptyList()) } returns DeviationResult(
            deviation = null,
            baseline = null,
            mad = null,
            signal = null,
            relativeDeviation = null,
            relativeMagnitude = null,
            status = DeviationStatus.InsufficientHistory,
        )

        val result = useCase(targetPackage)

        assertTrue(result.shouldTrigger)
        coVerify {
            interventionDecisionRepository.insert(match {
                it.reason == InterventionDecisionReason.TRIGGERED &&
                    it.historyCount == 0 && it.deviation == null && it.shouldTrigger
            })
        }
    }

    @Test
    fun `cooldown decision is audited without running engines`() = runBlocking {
        every { usageEventCollector.getUsageForDay(any()) } returns
            listOf(AppUsage(targetPackage, "Target", 60_000L, 0L))
        every { userPreferencesRepository.monitoredPackages } returns flowOf(setOf(targetPackage))
        every { userPreferencesRepository.nextEligibleInterventionAt } returns flowOf(Long.MAX_VALUE)
        every { userPreferencesRepository.emergencyBypassUntil } returns flowOf(0L)

        val result = useCase(targetPackage)

        assertFalse(result.shouldTrigger)
        verify(exactly = 0) { deviationEngine.calculate(any(), any()) }
        coVerify {
            interventionDecisionRepository.insert(match {
                it.reason == InterventionDecisionReason.COOLDOWN_ACTIVE &&
                    it.nextEligibleAt == Long.MAX_VALUE && it.deviation == null
            })
        }
    }

    @Test
    fun `evaluation failure is audited and remains visible to caller`() = runBlocking {
        every { usageEventCollector.getUsageForDay(any()) } throws IllegalStateException("collector failed")

        val thrown = runCatching { useCase(targetPackage) }.exceptionOrNull()

        assertTrue(thrown is IllegalStateException)
        coVerify {
            interventionDecisionRepository.insert(match {
                it.reason == InterventionDecisionReason.EVALUATION_ERROR &&
                    it.errorType == "IllegalStateException" && !it.shouldTrigger
            })
        }
    }

    @Test
    fun `evaluation cancellation is rethrown without an error audit`() = runBlocking {
        every { usageEventCollector.getUsageForDay(any()) } throws CancellationException("stopped")

        val thrown = runCatching { useCase(targetPackage) }.exceptionOrNull()

        assertTrue(thrown is CancellationException)
        coVerify(exactly = 0) { interventionDecisionRepository.insert(any()) }
    }

    @Test
    fun `performance uses same difficulty excludes bypasses and omits current response from baseline`() = runBlocking {
        val latest = performanceLog(id = 10L, responseTimeMs = 500L)
        val bypass = performanceLog(id = 9L, responseTimeMs = 0L, isBypassed = true)
        val previous = (1L..2L).map { id -> performanceLog(id = id, responseTimeMs = 1_000L) }
        stubEligibleEvaluation(recentLogs = listOf(latest, bypass) + previous)

        useCase(targetPackage)

        coVerify { interventionLogRepository.getRecentLogs(32) }
        verify {
            controlEngine.calculateNextIntervention(
                any(),
                match { it.difficulty == 2 && it.responseTimeMs == 500L },
                match { it == List(2) { 1_000L } },
                any(),
                any(),
                any(),
                2,
                any(),
                any(),
                match {
                    it.map(DifficultyHistoryEntry::difficulty) == List(4) { 2 } &&
                        it.map(DifficultyHistoryEntry::isValidResponse) ==
                        listOf(true, false, true, true)
                },
                0,
            )
        }
    }

    @Test
    fun `performance failure resets consecutive upward evidence`() = runBlocking {
        val latest = performanceLog(id = 10L, responseTimeMs = 500L)
        val recentSuccess = performanceLog(id = 9L, responseTimeMs = 1_000L)
        val failure = performanceLog(id = 8L, responseTimeMs = 1_200L, isSuccess = false)
        val olderSuccesses = (1L..5L).map { id -> performanceLog(id, 900L) }.reversed()
        stubEligibleEvaluation(recentLogs = listOf(latest, recentSuccess, failure) + olderSuccesses)

        useCase(targetPackage)

        verify {
            controlEngine.calculateNextIntervention(
                any(),
                match { it.isSuccess && it.responseTimeMs == 500L },
                match { it == listOf(1_000L) },
                any(), any(), any(), 2, any(), any(), any(), 0,
            )
        }
    }

    @Test
    fun `performance ignores stale successes from before a difficulty transition`() = runBlocking {
        val staleLevelTwo = (1L..6L).map { id -> performanceLog(id, 1_000L) }
        val latestLevelThree = performanceLog(
            id = 20L,
            responseTimeMs = 2_000L,
            difficulty = 3,
            isSuccess = false,
        )
        stubEligibleEvaluation(recentLogs = listOf(latestLevelThree) + staleLevelTwo.reversed())

        useCase(targetPackage)

        verify {
            controlEngine.calculateNextIntervention(
                any(), null, emptyList(), any(), any(), any(), 2, any(), any(),
                match {
                    it.map(DifficultyHistoryEntry::difficulty) == listOf(3, 2, 2, 2, 2, 2, 2) &&
                        it.all(DifficultyHistoryEntry::isValidResponse)
                },
                0,
            )
        }
    }

    @Test
    fun `pattern and math performance histories are isolated`() = runBlocking {
        every { challengeSelector.select() } returns InterventionChallengeType.PATTERN
        val logs = listOf(
            performanceLog(3L, 500L, challengeType = InterventionChallengeType.PATTERN),
            performanceLog(2L, 100L, challengeType = InterventionChallengeType.MATH),
            performanceLog(1L, 1_000L, challengeType = InterventionChallengeType.PATTERN),
        )
        stubEligibleEvaluation(recentLogs = logs)

        val result = useCase(targetPackage)

        assertEquals(InterventionChallengeType.PATTERN, result.challengeType)
        verify {
            controlEngine.calculateNextIntervention(
                any(),
                match { it.responseTimeMs == 500L },
                match { it == listOf(1_000L) },
                any(), any(), any(), 2, any(), any(),
                match {
                    it.map(DifficultyHistoryEntry::difficulty) == listOf(2, 2, 2) &&
                        it.all(DifficultyHistoryEntry::isValidResponse)
                },
                0,
            )
        }
    }

    @Test
    fun `three same-type failures are required before difficulty may decrease`() = runBlocking {
        val failures = (1L..3L).map { id ->
            performanceLog(id, 1_000L, isSuccess = false)
        }.reversed()
        stubEligibleEvaluation(recentLogs = failures)

        useCase(targetPackage)

        verify {
            controlEngine.calculateNextIntervention(
                any(), match { !it.isSuccess }, emptyList(), any(), any(), any(),
                2, any(), any(), any(), 3,
            )
        }
    }

    @Test
    fun `unmonitored package cannot trigger the engine`() = runBlocking {
        every { usageEventCollector.getUsageForDay(any()) } returns emptyList()
        every { userPreferencesRepository.monitoredPackages } returns flowOf(emptySet())

        val result = useCase("com.example.unmonitored")

        assertFalse(result.shouldTrigger)
        verify(exactly = 0) { deviationEngine.calculate(any(), any()) }
    }

    private fun stubEligibleEvaluation(
        usage: List<AppUsage> = listOf(AppUsage(targetPackage, "Target", 10 * 60_000L, 0L)),
        recentLogs: List<InterventionLog> = emptyList(),
        nextEligibleAt: Long = 1L,
    ) {
        every { usageEventCollector.getUsageForDay(any()) } returns usage
        every { userPreferencesRepository.monitoredPackages } returns flowOf(setOf(targetPackage))
        every { userPreferencesRepository.nextEligibleInterventionAt } returns flowOf(nextEligibleAt)
        every { userPreferencesRepository.emergencyBypassUntil } returns flowOf(0L)
        every { userPreferencesRepository.currentDifficulty } returns flowOf(2)
        every { userPreferencesRepository.bedtime } returns flowOf(null)
        every { userPreferencesRepository.wakeTime } returns flowOf(null)
        coEvery { getAdaptiveHistoryUseCase() } returns
            List(HistoricalConfig.HISTORY_SAMPLE_DAYS) { 10.0 }
        every { deviationEngine.calculate(any(), any()) } returns DeviationResult(
            deviation = 0.2,
            baseline = 10.0,
            mad = 1.0,
            signal = 1.0,
            relativeDeviation = 1.0,
            relativeMagnitude = 0.1,
            status = DeviationStatus.Success
        )
        coEvery { interventionLogRepository.getRecentLogs(32) } returns recentLogs
        every {
            controlEngine.calculateNextIntervention(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        } returns controlResult()
    }

    private fun performanceLog(
        id: Long,
        responseTimeMs: Long,
        difficulty: Int = 2,
        isSuccess: Boolean = true,
        challengeType: InterventionChallengeType = InterventionChallengeType.MATH,
        isBypassed: Boolean = false,
    ) = InterventionLog(
        id = id,
        timestamp = id,
        deviation = 0.2,
        difficultyControlSignal = 0.5,
        difficultyLevel = difficulty,
        responseTimeMs = responseTimeMs,
        isSuccess = isSuccess,
        isBypassed = isBypassed,
        penaltyAppliedMinutes = 0,
        challengeType = challengeType,
    )

    private fun controlResult() = ControlResult(
        deviation = 0.2,
        performance = 0.5,
        qLux = 0.0,
        qTime = 0.0,
        sensitivity = 0.0,
        difficultyControl = 0.1,
        normalizedDifficultyControl = 0.1,
        difficultyTarget = 1.4,
        currentDifficulty = 2,
        nextDifficulty = 2,
        frequencyControl = 0.2,
        normalizedFrequencyControl = 0.2,
        intervalMinutes = 24.6,
        nextEligibleInterventionAt = 1_000L,
        mode = ControlMode.PERSONALIZED
    )
}
