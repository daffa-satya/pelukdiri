package com.makhp.pelukdiri.core.domain.engine

import com.makhp.pelukdiri.core.domain.model.ControlConfig
import com.makhp.pelukdiri.core.domain.model.DeviationConfig
import com.makhp.pelukdiri.core.domain.model.DifficultyHistoryEntry
import com.makhp.pelukdiri.core.domain.model.PerformanceMetrics
import java.util.Random
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max

/**
 * Deterministic offline tuning evidence, not real-user validation.
 *
 * The ensemble uses generated usage profiles, seeded outcome streams, a truncated
 * normal user-ability offset (SD 0.3), alternating math/pattern challenges, and an
 * 8% resolved non-response rate excluded from performance evidence. It does not
 * model learning, fatigue, abandonment duration, sensor noise, or social context.
 */
class SyntheticPolicyTuningTest {
    @Test
    fun `candidate 1 stays inside ensemble bands without clustered reversals`() {
        val previous = ensemble(DeviationConfig.LEGACY_DEFAULT, ControlConfig.LEGACY_DEFAULT)
        val candidate1 = ensemble(DeviationConfig.CANDIDATE_1, ControlConfig.CANDIDATE_1)
        val previousSuccess = previous.meanOf { it.successRate }
        val candidateSuccess = candidate1.meanOf { it.successRate }

        println("previous=${previous.summary()}")
        println("candidate1=${candidate1.summary()}")
        assertTrue(abs(candidateSuccess - CANDIDATE_1_TARGET_SUCCESS) < abs(previousSuccess - CANDIDATE_1_TARGET_SUCCESS))
        assertTrue(candidateSuccess in 0.67..0.74)
        assertTrue(candidate1.all { it.successRate in 0.62..0.79 })
        assertTrue(candidate1.meanOf { it.mathSuccessRate } in 0.65..0.77)
        assertTrue(candidate1.meanOf { it.patternSuccessRate } in 0.63..0.75)
        assertTrue(candidate1.meanOf { it.averageIntervalMinutes } in 15.0..22.0)
        assertTrue(candidate1.all { it.minimumIntervalMinutes >= 3.0 })
        assertTrue(candidate1.all { it.maximumIntervalMinutes <= 30.0 })
        assertTrue(candidate1.all { it.immediateReversalRate < 0.1 })
        assertFalse(candidate1.any { it.hasClusteredReversals })
        assertTrue(candidate1.all { it.maximumDifficultyMove <= 1 })
    }

    @Test
    fun `candidate 2 reports the ability frontier for 85 percent success`() {
        val traces = SEEDS.map(::buildTrace)
        val currentCeiling = traces.map { easiestSuccessRate(it, abilityShift = 0.0) }.average()
        val hypotheticalLevelZero = traces.map {
            easiestSuccessRate(it, abilityShift = 0.0, difficulty = 0)
        }.average()
        val requiredAbilityShift = (0..40)
            .map { it * ABILITY_SWEEP_STEP }
            .first { shift ->
                traces.map { easiestSuccessRate(it, shift) }.average() >= CANDIDATE_2_TARGET_SUCCESS
            }

        println(
            "candidate2Target=$CANDIDATE_2_TARGET_SUCCESS " +
                "level1Ceiling=$currentCeiling requiredBaseAbilityShift=$requiredAbilityShift " +
                "hypotheticalLevel0=$hypotheticalLevelZero"
        )
        assertTrue(currentCeiling < CANDIDATE_2_TARGET_SUCCESS)
        assertTrue(requiredAbilityShift in 0.5..1.0)
        assertTrue(hypotheticalLevelZero >= CANDIDATE_2_TARGET_SUCCESS)
    }

    @Test
    fun `candidate 3 aggressive policy preserves safety bounds`() {
        val candidate3 = ensemble(DeviationConfig.CANDIDATE_3, ControlConfig.CANDIDATE_3)

        println("candidate3=${candidate3.summary()}")
        assertTrue(candidate3.meanOf { it.successRate } in CANDIDATE_3_MINIMUM_SUCCESS..CANDIDATE_3_MAXIMUM_SUCCESS)
        assertTrue(candidate3.meanOf { it.averageIntervalMinutes } in 13.0..15.0)
        assertTrue(candidate3.all { it.minimumIntervalMinutes >= 3.0 })
        assertTrue(candidate3.all { it.maximumIntervalMinutes <= 30.0 })
        assertTrue(candidate3.all { it.maximumDifficultyMove <= 1 })
        assertTrue(candidate3.all { it.immediateReversalRate < 0.1 })
        assertFalse(candidate3.any { it.hasClusteredReversals })
    }

    @Test
    fun `resolved non-response is excluded without collapsing frequency`() {
        val withNonResponse = ensemble(
            DeviationConfig.CANDIDATE_3,
            ControlConfig.CANDIDATE_3,
            NON_RESPONSE_RATE,
        )
        val allAnswered = ensemble(
            DeviationConfig.CANDIDATE_3,
            ControlConfig.CANDIDATE_3,
            nonResponseRate = 0.0,
        )

        assertTrue(withNonResponse.meanOf { it.responseRate } in 0.89..0.95)
        assertTrue(
            abs(
                withNonResponse.meanOf { it.averageIntervalMinutes } -
                    allAnswered.meanOf { it.averageIntervalMinutes }
            ) < 1e-9
        )
        assertTrue(withNonResponse.all { it.minimumIntervalMinutes >= 3.0 })
    }

    private fun ensemble(
        deviationConfig: DeviationConfig,
        controlConfig: ControlConfig,
        nonResponseRate: Double = NON_RESPONSE_RATE,
    ) = SEEDS.map { seed ->
        simulate(deviationConfig, controlConfig, buildTrace(seed), nonResponseRate)
    }

    private fun simulate(
        deviationConfig: DeviationConfig,
        controlConfig: ControlConfig,
        trace: List<SyntheticStep>,
        nonResponseRate: Double,
    ): Metrics {
        val deviationEngine = DeviationEngine(deviationConfig)
        val controlEngine = ControlEngine(
            controlConfig,
            SensitivityCalculator(controlConfig),
            PerformanceCalculator(controlConfig),
            DifficultyController(controlConfig),
            FrequencyController(controlConfig),
        )
        val logs = mutableListOf<SyntheticLog>()
        val difficultyHistory = mutableListOf<DifficultyHistoryEntry>()
        val intervals = mutableListOf<Double>()
        val moves = mutableListOf<Int>()
        val reversals = mutableListOf<Int>()
        var difficulty = 2
        var validResponses = 0
        var mathResponses = 0
        var patternResponses = 0
        var mathSuccesses = 0
        var patternSuccesses = 0
        fun recordDifficulty(isValidResponse: Boolean) {
            difficultyHistory.add(0, DifficultyHistoryEntry(difficulty, isValidResponse))
            if (difficultyHistory.size > PERFORMANCE_HISTORY_LIMIT) difficultyHistory.removeLast()
        }

        trace.forEachIndexed { index, step ->
            val deviation = requireNotNull(
                deviationEngine.calculate(step.usage, step.profile.history).deviation
            )
            val run = logs.takeWhile { it.difficulty == difficulty }
                .filter { it.challengeType == step.challengeType }
            val lastPerformance = run.firstOrNull()?.let {
                PerformanceMetrics(it.responseTimeMs, it.isSuccess, it.difficulty)
            }
            val performanceHistory = run.drop(1)
                .take(controlConfig.performanceEvidenceWindow)
                .takeWhile { it.isSuccess }
                .map { it.responseTimeMs }
            val consecutiveFailures = run
                .takeWhile { !it.isSuccess }
                .take(controlConfig.difficultyDecreaseEvidenceWindow)
                .count()
            val result = controlEngine.calculateNextIntervention(
                deviation = deviation,
                lastPerformance = lastPerformance,
                performanceHistory = performanceHistory,
                lux = SyntheticChallengeModel.luxFor(step.sensitivity),
                bedtime = null,
                wakeTime = null,
                currentLevel = difficulty,
                timestampMs = index.toLong(),
                difficultyHistory = difficultyHistory,
                consecutiveFailures = consecutiveFailures,
            )

            val move = result.nextDifficulty - difficulty
            moves += move
            if (index > 0 && move != 0 && moves[index - 1] != 0 && move != moves[index - 1]) {
                reversals += index
            }
            difficulty = result.nextDifficulty
            intervals += result.intervalMinutes

            if (step.nonResponseSample < nonResponseRate) {
                recordDifficulty(isValidResponse = false)
                return@forEachIndexed
            }

            validResponses++
            recordDifficulty(isValidResponse = true)
            if (step.challengeType == InterventionChallengeType.MATH) mathResponses++ else patternResponses++
            val isSuccess = step.outcomeSample < SyntheticChallengeModel.successProbability(
                step.challengeType,
                difficulty,
                step.profile.abilityOffset,
                step.sensitivity,
                deviation,
            )
            if (isSuccess) {
                if (step.challengeType == InterventionChallengeType.MATH) mathSuccesses++ else patternSuccesses++
            }
            val responseTimeMs = if (isSuccess) {
                ((900 + difficulty * 450) * (1.0 + 0.3 * step.responseVariation)).toLong()
            } else {
                ((1_200 + difficulty * 600) * (1.0 + 0.3 * step.responseVariation)).toLong()
            }
            logs.add(0, SyntheticLog(difficulty, step.challengeType, isSuccess, responseTimeMs))
            if (logs.size > 100) logs.removeLast()
        }

        return Metrics(
            successRate = (mathSuccesses + patternSuccesses) / validResponses.toDouble(),
            mathSuccessRate = mathSuccesses / mathResponses.toDouble(),
            patternSuccessRate = patternSuccesses / patternResponses.toDouble(),
            responseRate = validResponses / trace.size.toDouble(),
            averageIntervalMinutes = intervals.average(),
            minimumIntervalMinutes = intervals.min(),
            maximumIntervalMinutes = intervals.max(),
            immediateReversalRate = reversals.size / (trace.size - 1.0),
            hasClusteredReversals = reversals.zipWithNext().any { (first, second) -> second - first <= 3 },
            maximumDifficultyMove = moves.maxOf { abs(it) },
        )
    }

    private fun easiestSuccessRate(
        trace: List<SyntheticStep>,
        abilityShift: Double,
        difficulty: Int = 1,
    ): Double {
        var responses = 0
        var successes = 0
        trace.forEach { step ->
            if (step.nonResponseSample < NON_RESPONSE_RATE) return@forEach
            responses++
            val probability = SyntheticChallengeModel.successProbability(
                step.challengeType,
                difficulty,
                step.profile.abilityOffset + abilityShift,
                step.sensitivity,
                deviation = 0.0,
            )
            if (step.outcomeSample < probability) successes++
        }
        return successes / responses.toDouble()
    }

    private fun buildTrace(seed: Long): List<SyntheticStep> {
        val random = Random(seed)
        val profiles = generateProfiles(seed)
        return List(SAMPLE_SIZE) { index ->
            val profile = profiles[index % profiles.size]
            SyntheticStep(
                profile = profile,
                usage = profile.usages[(index / profiles.size) % profile.usages.size],
                sensitivity = SENSITIVITY_VALUES[random.nextInt(SENSITIVITY_VALUES.size)],
                challengeType = if (index % 2 == 0) InterventionChallengeType.MATH else InterventionChallengeType.PATTERN,
                outcomeSample = random.nextDouble(),
                responseVariation = random.nextDouble(),
                nonResponseSample = random.nextDouble(),
            )
        }
    }

    private fun generateProfiles(seed: Long): List<SyntheticProfile> = PROFILE_SPECS.flatMapIndexed { specIndex, spec ->
        List(PROFILES_PER_CATEGORY) { instance ->
            val random = Random(seed + specIndex * 101L + instance * 17L)
            val history = List(14) { day ->
                max(1.0, spec.mean + spec.trendPerDay * (day - 6.5) + random.nextGaussian() * spec.spread)
            }
            SyntheticProfile(
                history = history,
                usages = CURRENT_USAGE_FACTORS.map { factor ->
                    max(1.0, spec.mean * factor + random.nextGaussian() * spec.spread)
                },
                abilityOffset = (random.nextGaussian() * ABILITY_STANDARD_DEVIATION)
                    .coerceIn(-MAX_ABILITY_OFFSET, MAX_ABILITY_OFFSET),
            )
        }
    }

    private fun List<Metrics>.meanOf(selector: (Metrics) -> Double) = map(selector).average()

    private fun List<Metrics>.summary() = "success=${meanOf { it.successRate }}, " +
        "math=${meanOf { it.mathSuccessRate }}, pattern=${meanOf { it.patternSuccessRate }}, " +
        "response=${meanOf { it.responseRate }}, interval=${meanOf { it.averageIntervalMinutes }}, " +
        "reversal=${meanOf { it.immediateReversalRate }}, " +
        "clusteredSeeds=${count { it.hasClusteredReversals }}/${size}"

    private data class ProfileSpec(
        val mean: Double,
        val spread: Double,
        val trendPerDay: Double = 0.0,
    )

    private data class SyntheticProfile(
        val history: List<Double>,
        val usages: List<Double>,
        val abilityOffset: Double,
    )

    private data class SyntheticStep(
        val profile: SyntheticProfile,
        val usage: Double,
        val sensitivity: Double,
        val challengeType: InterventionChallengeType,
        val outcomeSample: Double,
        val responseVariation: Double,
        val nonResponseSample: Double,
    )

    private data class SyntheticLog(
        val difficulty: Int,
        val challengeType: InterventionChallengeType,
        val isSuccess: Boolean,
        val responseTimeMs: Long,
    )

    private data class Metrics(
        val successRate: Double,
        val mathSuccessRate: Double,
        val patternSuccessRate: Double,
        val responseRate: Double,
        val averageIntervalMinutes: Double,
        val minimumIntervalMinutes: Double,
        val maximumIntervalMinutes: Double,
        val immediateReversalRate: Double,
        val hasClusteredReversals: Boolean,
        val maximumDifficultyMove: Int,
    )

    private companion object {
        const val SAMPLE_SIZE = 4_000
        const val PERFORMANCE_HISTORY_LIMIT = 32
        const val PROFILES_PER_CATEGORY = 3
        const val ABILITY_STANDARD_DEVIATION = 0.3
        const val MAX_ABILITY_OFFSET = 0.6
        const val NON_RESPONSE_RATE = 0.08
        const val ABILITY_SWEEP_STEP = 0.05
        const val CANDIDATE_1_TARGET_SUCCESS = 0.7
        const val CANDIDATE_2_TARGET_SUCCESS = 0.85
        const val CANDIDATE_3_MINIMUM_SUCCESS = 0.46
        const val CANDIDATE_3_MAXIMUM_SUCCESS = 0.51
        val SEEDS = listOf(11L, 29L, 47L, 71L, 101L, 137L, 173L, 211L, 257L, 307L)
        val SENSITIVITY_VALUES = listOf(0.0, 0.25, 0.5, 0.75, 1.0)
        val CURRENT_USAGE_FACTORS = listOf(1.0, 1.15, 1.5, 2.0, 3.0)
        val PROFILE_SPECS = listOf(
            ProfileSpec(mean = 120.0, spread = 4.0),
            ProfileSpec(mean = 120.0, spread = 20.0),
            ProfileSpec(mean = 180.0, spread = 75.0),
            ProfileSpec(mean = 30.0, spread = 5.0),
            ProfileSpec(mean = 480.0, spread = 45.0),
            ProfileSpec(mean = 180.0, spread = 35.0, trendPerDay = -8.0),
        )
    }
}
