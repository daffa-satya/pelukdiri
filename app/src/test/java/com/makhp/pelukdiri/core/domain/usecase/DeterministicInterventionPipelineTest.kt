package com.makhp.pelukdiri.core.domain.usecase

import com.makhp.pelukdiri.collector.AppUsageCollector
import com.makhp.pelukdiri.collector.UsageEvent
import com.makhp.pelukdiri.collector.UsageEventCollector
import com.makhp.pelukdiri.collector.UsageEventReconstructor
import com.makhp.pelukdiri.core.domain.InterventionLockManager
import com.makhp.pelukdiri.core.domain.engine.CognitiveQuestionGenerator
import com.makhp.pelukdiri.core.domain.engine.ControlEngine
import com.makhp.pelukdiri.core.domain.engine.DeviationEngine
import com.makhp.pelukdiri.core.domain.engine.DifficultyController
import com.makhp.pelukdiri.core.domain.engine.FrequencyController
import com.makhp.pelukdiri.core.domain.engine.InterventionChallengeSelector
import com.makhp.pelukdiri.core.domain.engine.InterventionChallengeType
import com.makhp.pelukdiri.core.domain.engine.PatternQuestionGenerator
import com.makhp.pelukdiri.core.domain.engine.PerformanceCalculator
import com.makhp.pelukdiri.core.domain.engine.SensitivityCalculator
import com.makhp.pelukdiri.core.domain.model.AppUsage
import com.makhp.pelukdiri.core.domain.model.ControlConfig
import com.makhp.pelukdiri.core.domain.model.DailySummary
import com.makhp.pelukdiri.core.domain.model.DeviationConfig
import com.makhp.pelukdiri.core.domain.model.InterventionDecisionAudit
import com.makhp.pelukdiri.core.domain.model.InterventionDecisionReason
import com.makhp.pelukdiri.core.domain.model.InterventionLog
import com.makhp.pelukdiri.core.domain.model.MathQuestion
import com.makhp.pelukdiri.core.domain.repository.AdaptiveLimitRepository
import com.makhp.pelukdiri.core.domain.repository.InterventionDecisionRepository
import com.makhp.pelukdiri.core.domain.repository.InterventionLogRepository
import com.makhp.pelukdiri.core.domain.repository.UsageRepository
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import com.makhp.pelukdiri.core.domain.time.TimeProvider
import com.makhp.pelukdiri.features.intervention.ActiveInterventionSession
import com.makhp.pelukdiri.features.intervention.InterventionUiState
import com.makhp.pelukdiri.features.intervention.InterventionViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class DeterministicInterventionPipelineTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `usage event reaches decision launch overlay bypass persistence at exact boundary`() = runTest {
        val targetPackage = "com.example.target"
        val time = MutableTimeProvider(1_800_000_000_000L)
        val reconstructor = UsageEventReconstructor()
        val rangeStart = time.nowMillis() - 2 * 60 * 60_000L
        val sessions = reconstructor.reconstructSessions(
            events = listOf(
                UsageEvent(targetPackage, time.nowMillis() - 90 * 60_000L, UsageEventReconstructor.ACTIVITY_RESUMED),
                UsageEvent(targetPackage, time.nowMillis(), UsageEventReconstructor.ACTIVITY_PAUSED),
            ),
            queryEnd = time.nowMillis(),
        )
        val usage = reconstructor.aggregateUsage(sessions, rangeStart, time.nowMillis()).map {
            AppUsage(it.key, it.key, it.value.duration, it.value.lastTimestamp)
        }
        assertEquals(90 * 60_000L, usage.single().usageDurationMillis)

        val nextEligible = MutableStateFlow(0L)
        val bypassUntil = MutableStateFlow(0L)
        val difficulty = MutableStateFlow(2)
        val activeSession = MutableStateFlow<String?>(null)
        val preferences = mockk<UserPreferencesRepository>()
        every { preferences.monitoredPackages } returns flowOf(setOf(targetPackage))
        every { preferences.nextEligibleInterventionAt } returns nextEligible
        every { preferences.emergencyBypassUntil } returns bypassUntil
        every { preferences.currentDifficulty } returns difficulty
        every { preferences.activeInterventionSession } returns activeSession
        every { preferences.bedtime } returns flowOf(null)
        every { preferences.wakeTime } returns flowOf(null)
        coEvery { preferences.setNextEligibleInterventionAt(any()) } answers {
            nextEligible.value = firstArg()
        }
        coEvery { preferences.setEmergencyBypassUntil(any()) } answers {
            bypassUntil.value = firstArg()
        }
        coEvery { preferences.setCurrentDifficulty(any()) } answers {
            difficulty.value = firstArg()
        }
        coEvery { preferences.setActiveInterventionSession(any()) } answers {
            activeSession.value = firstArg()
        }

        val usageRepository = mockk<UsageRepository>()
        val history = (1L..7L).map { daysAgo ->
            DailySummary(
                date = time.today().minusDays(daysAgo),
                totalScreenTimeMillis = 60 * 60_000L,
                totalScreenOnMillis = 60 * 60_000L,
                monitoredUsageMillis = 60 * 60_000L,
                unlockCount = 1,
                mostUsedApp = targetPackage,
            )
        }
        every { usageRepository.getUsageHistory(any(), any()) } returns flowOf(history)

        val usageCollector = mockk<UsageEventCollector>()
        every { usageCollector.getUsageForDay(any()) } returns usage
        val appUsageCollector = mockk<AppUsageCollector>()
        every { appUsageCollector.getCurrentAmbientLightLux() } returns 25f

        val decisions = mutableListOf<InterventionDecisionAudit>()
        val decisionRepository = mockk<InterventionDecisionRepository>()
        coEvery { decisionRepository.insert(any()) } answers {
            decisions += firstArg<InterventionDecisionAudit>()
        }
        val logs = mutableListOf<InterventionLog>()
        val logRepository = mockk<InterventionLogRepository>()
        coEvery { logRepository.getRecentLogs(any()) } returns emptyList()
        coEvery { logRepository.getBypassCountForDay(any(), any()) } answers {
            logs.count { it.isBypassed }
        }
        coEvery { logRepository.insertBypassIfQuotaAvailable(any(), any(), any(), any()) } answers {
            val limit = arg<Int>(3)
            if (logs.count { it.isBypassed } >= limit) null else {
                logs += firstArg<InterventionLog>()
                limit - logs.count { it.isBypassed }
            }
        }

        val config = ControlConfig.CANDIDATE_3
        val controlEngine = ControlEngine(
            config,
            SensitivityCalculator(config),
            PerformanceCalculator(config),
            DifficultyController(config),
            FrequencyController(config),
        )
        val lockManager = InterventionLockManager()
        val eligibility = EvaluateInterventionEligibilityUseCase(
            usageCollector,
            preferences,
            GetAdaptiveHistoryUseCase(usageRepository, time),
            DeviationEngine(DeviationConfig.CANDIDATE_3),
            controlEngine,
            logRepository,
            decisionRepository,
            InterventionChallengeSelector { true },
            appUsageCollector,
            lockManager,
            config,
            time,
        )

        val scheduled = eligibility(targetPackage)
        assertFalse(scheduled.shouldTrigger)
        assertEquals(InterventionDecisionReason.INTERVAL_SCHEDULED, decisions.single().reason)

        time.now = nextEligible.value
        val triggered = eligibility(targetPackage)
        assertTrue(triggered.shouldTrigger)
        assertEquals(2, decisions.size)
        assertEquals(InterventionDecisionReason.TRIGGERED, decisions.last().reason)
        assertEquals(time.nowMillis(), decisions.last().timestamp)
        assertEquals(InterventionChallengeType.MATH, decisions.last().challengeType)

        val control = requireNotNull(triggered.controlResult)
        var overlayLaunches = 0
        val launchResult = AttemptInterventionLaunchUseCase(lockManager, preferences)(control) {
            overlayLaunches++
            true
        }
        assertEquals(InterventionLaunchResult.LAUNCHED, launchResult)
        assertEquals(1, overlayLaunches)
        assertTrue(lockManager.isLocked.value)
        assertEquals(control.nextEligibleInterventionAt, nextEligible.value)
        assertEquals(control.nextDifficulty, difficulty.value)

        val cognitiveGenerator = mockk<CognitiveQuestionGenerator>()
        coEvery { cognitiveGenerator.generateQuestion(any()) } returns MathQuestion("1 + 1", 2, 1)
        val patternGenerator = mockk<PatternQuestionGenerator>()
        val adaptiveLimitRepository = mockk<AdaptiveLimitRepository>()
        coEvery { adaptiveLimitRepository.getLimitForDate(any()) } returns null
        val bypass = PerformEmergencyBypassUseCase(logRepository, preferences, time)
        val session = ActiveInterventionSession(preferences, time, lockManager)
        val viewModel = InterventionViewModel(
            cognitiveGenerator,
            patternGenerator,
            InterventionChallengeSelector { true },
            logRepository,
            adaptiveLimitRepository,
            preferences,
            bypass,
            lockManager,
            session,
            time,
        )
        viewModel.startIntervention(
            monitoredUsageMinutes = triggered.monitoredUsageMinutes,
            intervalMinutesAtLaunch = 1.0,
            ambientLightLuxAtLaunch = triggered.ambientLux,
            deviation = requireNotNull(control.deviation),
            difficultyControlSignal = control.normalizedDifficultyControl,
            difficulty = control.nextDifficulty,
            challengeType = triggered.challengeType,
        )
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is InterventionUiState.QuestionActive)

        time.now += 5_000L
        viewModel.emergencyBypass()
        advanceUntilIdle()

        assertEquals(InterventionUiState.Idle, viewModel.uiState.value)
        assertFalse(lockManager.isLocked.value)
        assertEquals(1, logs.size)
        assertTrue(logs.single().isBypassed)
        assertEquals(InterventionChallengeType.MATH, logs.single().challengeType)
        assertEquals(time.nowMillis(), logs.single().timestamp)
        assertEquals(time.nowMillis() + 180_000L, bypassUntil.value)
        assertEquals(bypassUntil.value, nextEligible.value)
        assertEquals(2, decisions.size)
    }

    @Test
    fun `failed launch releases ownership and commits no control state`() = runTest {
        val nextEligible = MutableStateFlow(11L)
        val difficulty = MutableStateFlow(2)
        val preferences = mockk<UserPreferencesRepository>()
        coEvery { preferences.setNextEligibleInterventionAt(any()) } answers {
            nextEligible.value = firstArg()
        }
        coEvery { preferences.setCurrentDifficulty(any()) } answers {
            difficulty.value = firstArg()
        }
        val lockManager = InterventionLockManager()
        val config = ControlConfig.CANDIDATE_3
        val control = ControlEngine(
            config,
            SensitivityCalculator(config),
            PerformanceCalculator(config),
            DifficultyController(config),
            FrequencyController(config),
        ).calculateNextIntervention(
            deviation = 1.0,
            lastPerformance = null,
            performanceHistory = emptyList(),
            lux = 25f,
            bedtime = null,
            wakeTime = null,
            currentLevel = 2,
            timestampMs = 1_000L,
        )

        val result = AttemptInterventionLaunchUseCase(lockManager, preferences)(control) { false }

        assertEquals(InterventionLaunchResult.FAILED, result)
        assertFalse(lockManager.isLocked.value)
        assertEquals(11L, nextEligible.value)
        assertEquals(2, difficulty.value)
    }

    private class MutableTimeProvider(
        var now: Long,
    ) : TimeProvider {
        override fun nowMillis(): Long = now
        override fun zoneId(): ZoneId = ZoneId.of("Asia/Jakarta")
    }
}
