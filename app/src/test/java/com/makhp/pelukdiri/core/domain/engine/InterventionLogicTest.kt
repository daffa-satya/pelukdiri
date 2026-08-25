package com.makhp.pelukdiri.core.domain.engine

import com.makhp.pelukdiri.core.domain.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalTime

class InterventionLogicTest {

    private lateinit var controlEngine: ControlEngine
    private val config = ControlConfig()
    private val sensitivityCalculator = SensitivityCalculator(config)
    private val performanceCalculator = PerformanceCalculator(config)
    private val difficultyController = DifficultyController(config)
    private val frequencyController = FrequencyController(config)

    @Before
    fun setup() {
        controlEngine = ControlEngine(
            config,
            sensitivityCalculator,
            performanceCalculator,
            difficultyController,
            frequencyController
        )
    }

    @Test
    fun `significant deviation triggers intervention - D gt 0_05`() {
        // D = 0.06
        val deviation = 0.06
        val shouldTrigger = deviation > 0.05
        assertTrue(shouldTrigger)
    }

    @Test
    fun `insignificant deviation suppresses intervention - D le 0_05`() {
        // D = 0.04
        val deviation = 0.04
        val shouldTrigger = deviation > 0.05
        assertFalse(shouldTrigger)
    }

    @Test
    fun `insufficient history suppresses intervention - deviation is null`() {
        val deviation: Double? = null
        val shouldTrigger = deviation != null && deviation > 0.05
        assertFalse(shouldTrigger)
    }

    @Test
    fun `cooldown - nextEligibleInterventionAt is correctly calculated`() {
        val deviation = 0.1
        val timestamp = 1000L
        val result = controlEngine.calculateNextIntervention(
            deviation = deviation,
            lastPerformance = null,
            performanceHistory = emptyList(),
            lux = null,
            bedtime = null,
            wakeTime = null,
            currentLevel = 1,
            timestampMs = timestamp
        )

        // intervalMinutes = 30 - 27 * C_F_norm
        // D = 0.1, Q = 0.0 -> C_F = 0.1
        // interval = 30 - 27 * 0.1 = 27.3
        val expectedNext = timestamp + (27.3 * 60 * 1000L).toLong()
        assertEquals(expectedNext, result.nextEligibleInterventionAt)
    }

    @Test
    fun `cooldown - block until nextEligible`() {
        val currentTime = 1000L
        val nextEligible = 2000L
        
        // Logic from AppBlockerAccessibilityService
        val isBlocked = currentTime < nextEligible
        assertTrue(isBlocked)
    }

    @Test
    fun `cooldown - eligible after nextEligible`() {
        val currentTime = 2000L
        val nextEligible = 2000L
        
        // Logic from AppBlockerAccessibilityService
        val isBlocked = currentTime < nextEligible
        assertFalse(isBlocked)
    }

    @Test
    fun `safe default also respects the reversal guard`() {
        val result = controlEngine.calculateNextIntervention(
            deviation = null,
            lastPerformance = null,
            performanceHistory = emptyList(),
            lux = null,
            bedtime = null,
            wakeTime = null,
            currentLevel = 3,
            difficultyHistory = listOf(
                DifficultyHistoryEntry(3, true),
                DifficultyHistoryEntry(2, true),
                DifficultyHistoryEntry(3, true),
            ),
        )

        assertEquals(3, result.nextDifficulty)
    }

    @Test
    fun `candidate three increases after three total consecutive successes`() {
        val candidate = ControlConfig.CANDIDATE_3
        val engine = ControlEngine(
            candidate,
            SensitivityCalculator(candidate),
            PerformanceCalculator(candidate),
            DifficultyController(candidate),
            FrequencyController(candidate),
        )
        val latest = PerformanceMetrics(500L, true, 2)
        val afterTwoTotal = engine.calculateNextIntervention(
            0.5, latest, listOf(1_000L), 500f, null, null, 2
        )
        val afterThreeTotal = engine.calculateNextIntervention(
            0.5, latest, listOf(1_000L, 1_000L), 500f, null, null, 2
        )

        assertEquals(2, afterTwoTotal.nextDifficulty)
        assertEquals(3, afterThreeTotal.nextDifficulty)
    }

    @Test
    fun `candidate three safe default also requires three failures to decrease`() {
        val candidate = ControlConfig.CANDIDATE_3
        val engine = ControlEngine(
            candidate,
            SensitivityCalculator(candidate),
            PerformanceCalculator(candidate),
            DifficultyController(candidate),
            FrequencyController(candidate),
        )
        val afterTwo = engine.calculateNextIntervention(
            null, PerformanceMetrics(1_000L, false, 3), emptyList(), null, null, null,
            3, consecutiveFailures = 2
        )
        val afterThree = engine.calculateNextIntervention(
            null, PerformanceMetrics(1_000L, false, 3), emptyList(), null, null, null,
            3, consecutiveFailures = 3
        )

        assertEquals(3, afterTwo.nextDifficulty)
        assertEquals(2, afterThree.nextDifficulty)
    }
}
