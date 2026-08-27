package com.makhp.pelukdiri.core.domain.engine

import com.makhp.pelukdiri.core.domain.model.ControlConfig
import com.makhp.pelukdiri.core.domain.model.ControlMode
import com.makhp.pelukdiri.core.domain.model.ControlResult
import com.makhp.pelukdiri.core.domain.model.DifficultyHistoryEntry
import com.makhp.pelukdiri.core.domain.model.PerformanceMetrics
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ControlEngine @Inject constructor(
    private val config: ControlConfig,
    private val sensitivityCalculator: SensitivityCalculator,
    private val performanceCalculator: PerformanceCalculator,
    private val difficultyController: DifficultyController,
    private val frequencyController: FrequencyController,
) {
    /**
     * Orchestrates the control logic to determine the next difficulty and interval.
     */
    fun calculateNextIntervention(
        deviation: Double?,
        lastPerformance: PerformanceMetrics?,
        performanceHistory: List<Long>,
        lux: Float?,
        bedtime: LocalTime?,
        wakeTime: LocalTime?,
        currentLevel: Int,
        currentTime: LocalTime = LocalTime.now(),
        timestampMs: Long = System.currentTimeMillis(),
        difficultyHistory: List<DifficultyHistoryEntry> = emptyList(),
        consecutiveFailures: Int = 0,
        adaptiveLimitProgress: Double? = null,
    ): ControlResult {
        val consecutiveSuccesses = if (lastPerformance?.isSuccess == true) {
            1 + performanceHistory.size
        } else {
            0
        }
        // 1. Fallback for Deviation
        if (deviation == null) {
            return safeDefault(
                currentLevel,
                timestampMs,
                difficultyHistory,
                consecutiveFailures,
                lastPerformance?.isSuccess == false,
                lastPerformance?.isSuccess == true,
                consecutiveSuccesses,
            )
        }

        // 2. Sensitivity
        val q = sensitivityCalculator.calculate(lux, bedtime, wakeTime, currentTime)
        
        // Explainability metrics
        val qLux = sensitivityCalculator.calculate(lux, null, null, currentTime)
        val qTime = sensitivityCalculator.calculate(null, bedtime, wakeTime, currentTime)

        // 3. Performance
        val p = if (lastPerformance != null) {
            performanceCalculator.calculate(lastPerformance, performanceHistory)
        } else {
            0.5 // Neutral performance
        }

        val insufficientEvidence = lastPerformance?.isSuccess != true ||
            performanceHistory.size < config.performanceEvidenceWindow
        val mode = if (insufficientEvidence) {
            ControlMode.INSUFFICIENT_HISTORY
        } else {
            ControlMode.PERSONALIZED
        }

        // 4. Difficulty
        val diffResult = difficultyController.calculate(
            deviation,
            p,
            q,
            currentLevel,
            insufficientEvidence,
            difficultyHistory,
            consecutiveFailures,
            lastPerformance?.isSuccess == false,
            lastPerformance?.isSuccess == true,
            consecutiveSuccesses,
        )

        // 5. Frequency
        val freqResult = frequencyController.calculate(deviation, q, adaptiveLimitProgress)

        val nextEligibleAt = timestampMs + (freqResult.intervalMinutes * 60 * 1000).toLong()

        return ControlResult(
            deviation = deviation,
            performance = p,
            qLux = qLux,
            qTime = qTime,
            sensitivity = q,
            difficultyControl = diffResult.controlSignal,
            normalizedDifficultyControl = diffResult.normalizedSignal,
            difficultyTarget = diffResult.target,
            currentDifficulty = currentLevel,
            nextDifficulty = diffResult.nextLevel,
            frequencyControl = freqResult.controlSignal,
            normalizedFrequencyControl = freqResult.normalizedSignal,
            intervalMinutes = freqResult.intervalMinutes,
            nextEligibleInterventionAt = nextEligibleAt,
            mode = mode
        )
    }

    private fun safeDefault(
        currentLevel: Int,
        timestampMs: Long,
        difficultyHistory: List<DifficultyHistoryEntry>,
        consecutiveFailures: Int,
        latestResponseFailed: Boolean,
        latestResponseSucceeded: Boolean,
        consecutiveSuccesses: Int,
    ): ControlResult {
        val nextEligibleAt = timestampMs + (config.defaultFrequencyMinutes * 60 * 1000).toLong()
        var nextDifficulty = difficultyController.applyDecreaseEvidencePolicy(
            currentLevel,
            config.defaultDifficulty,
            consecutiveFailures,
            latestResponseFailed,
        )
        nextDifficulty = difficultyController.applyReversalGuard(
            currentLevel, nextDifficulty, difficultyHistory
        )
        nextDifficulty = difficultyController.applyMinimumDifficultyPolicy(
            currentLevel,
            nextDifficulty,
            consecutiveFailures,
            latestResponseFailed,
            latestResponseSucceeded,
            consecutiveSuccesses,
        )
        return ControlResult(
            deviation = null,
            performance = 0.5,
            qLux = 0.0,
            qTime = 0.0,
            sensitivity = 0.0,
            difficultyControl = 0.0,
            normalizedDifficultyControl = 0.0,
            difficultyTarget = config.defaultDifficulty.toDouble(),
            currentDifficulty = currentLevel,
            nextDifficulty = nextDifficulty,
            frequencyControl = 0.0,
            normalizedFrequencyControl = 0.0,
            intervalMinutes = config.defaultFrequencyMinutes,
            nextEligibleInterventionAt = nextEligibleAt,
            mode = ControlMode.SAFE_DEFAULT
        )
    }
}
