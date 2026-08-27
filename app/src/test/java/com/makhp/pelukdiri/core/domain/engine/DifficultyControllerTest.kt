package com.makhp.pelukdiri.core.domain.engine

import com.makhp.pelukdiri.core.domain.model.ControlConfig
import com.makhp.pelukdiri.core.domain.model.DeviationConfig
import com.makhp.pelukdiri.core.domain.model.DifficultyHistoryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DifficultyControllerTest {

    private val config = ControlConfig(
        lambdaDifficulty = 0.5,
        maxDifficultyChangePerUpdate = 1,
        difficultyDecreaseEvidenceWindow = 3,
    )
    private val controller = DifficultyController(config)

    @Test
    fun `calculate - base mapping`() {
        // D=0.25, P=1.0, Q=0.0 -> C_D = 0.25 -> Target = 1 + 4*0.25 = 2.0
        val result = controller.calculate(0.25, 1.0, 0.0, 1, false)
        assertEquals(2.0, result.target, 0.001)
        assertEquals(2, result.nextLevel)
    }

    @Test
    fun `calculate - stabilization prevents large jumps`() {
        // D=1.0, P=1.0, Q=1.0 -> C_D = 1 * 1 * (1 + 0.5) = 1.5 -> Normalized = 1.0 -> Target = 5.0
        val result = controller.calculate(1.0, 1.0, 1.0, 1, false)
        assertEquals(5.0, result.target, 0.001)
        assertEquals(2, result.nextLevel) // 1 + 1 = 2
    }

    @Test
    fun `calculate - stabilization prevents free fall`() {
        // D=0.0, P=0.0, Q=0.0 -> C_D = 0 -> Target = 1.0
        val result = controller.calculate(
            0.0, 0.0, 0.0, 5, false, consecutiveFailures = 3
        )
        assertEquals(1.0, result.target, 0.001)
        assertEquals(4, result.nextLevel) // 5 - 1 = 4
    }

    @Test
    fun `calculate - sensitivity increases aggressiveness`() {
        // D=0.5, P=1.0, Q=0.0 -> C_D = 0.5 -> Target = 3.0
        val res1 = controller.calculate(0.5, 1.0, 0.0, 3, false)
        // D=0.5, P=1.0, Q=1.0 -> C_D = 0.5 * 1.5 = 0.75 -> Target = 4.0
        val res2 = controller.calculate(0.5, 1.0, 1.0, 3, false)
        
        assertTrue(res2.target > res1.target)
    }

    @Test
    fun `perfect performance at deviation zero point five stays at least level three`() {
        val candidate = DifficultyController(ControlConfig.CANDIDATE_3)
        val bright = candidate.calculate(0.5, 1.0, 0.0, 3, false)
        val dark = candidate.calculate(0.5, 1.0, 1.0, 3, false)

        assertEquals(3.0, bright.target, 0.001)
        assertEquals(3.4, dark.target, 0.001)
        assertTrue(bright.nextLevel >= 3)
        assertTrue(dark.nextLevel >= 3)
    }

    @Test
    fun `difficulty decreases only after three consecutive failures`() {
        val afterOne = controller.calculate(
            0.0, 0.0, 0.0, 3, false,
            consecutiveFailures = 1, latestResponseFailed = true,
        )
        val afterTwo = controller.calculate(
            0.0, 0.0, 0.0, 3, false,
            consecutiveFailures = 2, latestResponseFailed = true,
        )
        val afterThree = controller.calculate(
            0.0, 0.0, 0.0, 3, false,
            consecutiveFailures = 3, latestResponseFailed = true,
        )

        assertEquals(3, afterOne.nextLevel)
        assertEquals(3, afterTwo.nextLevel)
        assertEquals(2, afterThree.nextLevel)
    }

    @Test
    fun `candidate three exits level one after two valid successes`() {
        val candidate = DifficultyController(ControlConfig.CANDIDATE_3)

        val afterOne = candidate.calculate(
            deviation = 0.0,
            performance = 0.5,
            sensitivity = 0.0,
            currentLevel = 1,
            insufficientEvidence = true,
            latestResponseSucceeded = true,
            consecutiveSuccesses = 1,
        )
        val afterTwo = candidate.calculate(
            deviation = 0.0,
            performance = 0.5,
            sensitivity = 0.0,
            currentLevel = 1,
            insufficientEvidence = true,
            latestResponseSucceeded = true,
            consecutiveSuccesses = 2,
        )

        assertEquals(1.0, afterTwo.target, 0.001)
        assertEquals(1, afterOne.nextLevel)
        assertEquals(2, afterTwo.nextLevel)
    }

    @Test
    fun `candidate three keeps level two when formula target is one`() {
        val candidate = DifficultyController(ControlConfig.CANDIDATE_3)

        val result = candidate.calculate(
            deviation = 0.0,
            performance = 0.0,
            sensitivity = 0.0,
            currentLevel = 2,
            insufficientEvidence = false,
        )

        assertEquals(1.0, result.target, 0.001)
        assertEquals(2, result.nextLevel)
    }

    @Test
    fun `candidate three aggressive deviation points map to expected targets`() {
        val deviation = DeviationEngine(DeviationConfig.CANDIDATE_3)
        val candidate = DifficultyController(ControlConfig.CANDIDATE_3)

        val midpoint = candidate.calculate(
            deviation.anchoredLogistic(2.0), 1.0, 0.0, 2, false,
        )
        val elevated = candidate.calculate(
            deviation.anchoredLogistic(5.0), 1.0, 0.0, 2, false,
        )
        val high = candidate.calculate(
            deviation.anchoredLogistic(10.0), 1.0, 0.0, 3, false,
        )

        assertEquals(1.9024, midpoint.target, 0.0001)
        assertEquals(2, midpoint.nextLevel)
        assertEquals(3.2093, elevated.target, 0.0001)
        assertEquals(3, elevated.nextLevel)
        assertEquals(4.4847, high.target, 0.0001)
        assertEquals(4, high.nextLevel)
    }

    @Test
    fun `candidate three allows level one after three failures`() {
        val candidate = DifficultyController(ControlConfig.CANDIDATE_3)

        val result = candidate.calculate(
            deviation = 0.0,
            performance = 0.0,
            sensitivity = 0.0,
            currentLevel = 2,
            insufficientEvidence = true,
            consecutiveFailures = 3,
            latestResponseFailed = true,
        )

        assertEquals(1, result.nextLevel)
    }

    @Test
    fun `candidate three ordinary decrease requires two failures`() {
        val candidate = DifficultyController(ControlConfig.CANDIDATE_3)
        val correct = candidate.calculate(
            0.0, 0.0, 0.0, 3, false,
            latestResponseSucceeded = true,
        )
        val afterOneFailure = candidate.calculate(
            0.0, 0.0, 0.0, 3, true,
            consecutiveFailures = 1,
            latestResponseFailed = true,
        )
        val afterTwoFailures = candidate.calculate(
            0.0, 0.0, 0.0, 3, true,
            consecutiveFailures = 2,
            latestResponseFailed = true,
        )

        assertEquals(3, correct.nextLevel)
        assertEquals(3, afterOneFailure.nextLevel)
        assertEquals(2, afterTwoFailures.nextLevel)
    }

    @Test
    fun `correct response follows the tuned target when it proposes a decrease`() {
        val result = controller.calculate(
            0.1, 1.0, 0.0, 3, false, latestResponseFailed = false
        )

        assertEquals(2, result.nextLevel)
    }

    @Test
    fun `calculate - low deviation prevents aggressive escalation regardless of performance`() {
        // Low D = 0.05, High P = 1.0, High Q = 1.0
        // C_D = 0.05 * 1.0 * (1 + 0.5 * 1.0) = 0.05 * 1.5 = 0.075
        // Target = 1 + 4 * 0.075 = 1.3
        val result = controller.calculate(0.05, 1.0, 1.0, 1, false)
        assertEquals(1.3, result.target, 0.001)
        assertEquals(1, result.nextLevel)
    }

    @Test
    fun `calculate - insufficient performance evidence blocks escalation`() {
        // D=0.9, P=0.5 (neutral), Q=1.0, current=2
        // C_D = 0.9 * 0.5 * 1.5 = 0.675
        // Target = 1 + 4 * 0.675 = 3.7 -> Rounded 4
        // Stabilization would allow 3 (2+1)
        // Guard must block it to 2
        val result = controller.calculate(0.9, 0.5, 1.0, 2, true)
        assertTrue(result.nextLevel <= 2)
        assertEquals(2, result.nextLevel)
    }

    @Test
    fun `calculate - reversal blocks the prior direction for three valid completions`() {
        val immediate = controller.calculate(0.5, 1.0, 0.0, 2, false, history(2, 3, 2))
        val afterTwo = controller.calculate(0.5, 1.0, 0.0, 2, false, history(2, 2, 2, 3, 2))
        val afterThree = controller.calculate(0.5, 1.0, 0.0, 2, false, history(2, 2, 2, 2, 3, 2))

        assertEquals(2, immediate.nextLevel)
        assertEquals(2, afterTwo.nextLevel)
        assertEquals(3, afterThree.nextLevel)
    }

    @Test
    fun `calculate - reversal guard is symmetric and permits the new direction`() {
        val blockedDown = controller.calculate(
            0.0, 0.0, 0.0, 3, false, history(3, 2, 3), consecutiveFailures = 3
        )
        val continuedDown = controller.calculate(
            0.0, 0.0, 0.0, 2, false, history(2, 3, 2), consecutiveFailures = 3
        )

        assertEquals(3, blockedDown.nextLevel)
        assertEquals(1, continuedDown.nextLevel)
    }

    @Test
    fun `calculate - invalid interventions do not age the reversal guard`() {
        val twoValidAfterReversal = listOf(
            DifficultyHistoryEntry(2, true),
            DifficultyHistoryEntry(2, false),
            DifficultyHistoryEntry(2, true),
            DifficultyHistoryEntry(2, false),
            DifficultyHistoryEntry(2, true),
            DifficultyHistoryEntry(3, true),
            DifficultyHistoryEntry(2, true),
        )
        val blocked = controller.calculate(0.5, 1.0, 0.0, 2, false, twoValidAfterReversal)
        val allowed = controller.calculate(
            0.5,
            1.0,
            0.0,
            2,
            false,
            listOf(DifficultyHistoryEntry(2, true)) + twoValidAfterReversal,
        )

        assertEquals(2, blocked.nextLevel)
        assertEquals(3, allowed.nextLevel)
    }

    private fun history(vararg levels: Int) = levels.map {
        DifficultyHistoryEntry(it, isValidResponse = true)
    }
}
