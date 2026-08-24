package com.makhp.pelukdiri.core.domain.engine

import com.makhp.pelukdiri.core.domain.model.ControlConfig
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.exp
import kotlin.math.ln

class SyntheticModelPropertiesTest {
    @Test
    fun `success probability decreases with difficulty and deviation`() {
        val byDifficulty = (1..5).map { difficulty ->
            SyntheticChallengeModel.successProbability(
                InterventionChallengeType.MATH,
                difficulty,
                abilityOffset = 0.0,
                sensitivity = 0.5,
                deviation = 0.5,
            )
        }
        val byDeviation = (0..4).map { step ->
            SyntheticChallengeModel.successProbability(
                InterventionChallengeType.MATH,
                difficulty = 2,
                abilityOffset = 0.0,
                sensitivity = 0.5,
                deviation = step / 4.0,
            )
        }

        assertTrue(byDifficulty.zipWithNext().all { (lower, higher) -> lower > higher })
        assertTrue(byDeviation.zipWithNext().all { (lower, higher) -> lower > higher })
    }

    @Test
    fun `frequency remains inside configured bounds across the input grid`() {
        val config = ControlConfig.CANDIDATE_3
        val controller = FrequencyController(config)

        (-4..8).map { it / 4.0 }.forEach { deviation ->
            (-4..8).map { it / 4.0 }.forEach { sensitivity ->
                assertTrue(
                    controller.calculate(deviation, sensitivity).intervalMinutes in
                        config.minFrequencyMinutes..config.maxFrequencyMinutes
                )
            }
        }
    }
}

internal object SyntheticChallengeModel {
    fun successProbability(
        challengeType: InterventionChallengeType,
        difficulty: Int,
        abilityOffset: Double,
        sensitivity: Double,
        deviation: Double,
    ): Double {
        val baseAbility = when (challengeType) {
            InterventionChallengeType.MATH -> 2.05
            InterventionChallengeType.PATTERN -> 1.85
            InterventionChallengeType.AUTO -> error("Synthetic selection must be explicit")
        }
        val ability = baseAbility + abilityOffset - 0.2 * sensitivity - 0.1 * deviation
        return 1.0 / (1.0 + exp(1.15 * (difficulty - ability)))
    }

    fun luxFor(sensitivity: Double): Float {
        val logDark = ln(11.0)
        val logBright = ln(501.0)
        return (exp(logDark + (1.0 - sensitivity) * (logBright - logDark)) - 1.0).toFloat()
    }
}
