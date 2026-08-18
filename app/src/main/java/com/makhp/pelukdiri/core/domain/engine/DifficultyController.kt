package com.makhp.pelukdiri.core.domain.engine

import com.makhp.pelukdiri.core.domain.model.ControlConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class DifficultyController @Inject constructor(
    private val config: ControlConfig,
) {
    /**
     * Calculates the difficulty target and next actual difficulty level.
     *
     * @param deviation Deviation signal D in [0,1].
     * @param performance Performance score P in [0,1].
     * @param sensitivity Sensitivity modifier Q in [0,1].
     * @param currentLevel Current difficulty level [1,5].
     * @param insufficientEvidence True if performance history is insufficient.
     * @return DifficultyResult.
     */
    fun calculate(
        deviation: Double,
        performance: Double,
        sensitivity: Double,
        currentLevel: Int,
        insufficientEvidence: Boolean,
    ): DifficultyResult {
        // C_D = D * P * (1 + lambda_D * Q)
        val controlSignal = deviation * performance * (1.0 + (config.lambdaDifficulty * sensitivity))
        
        // Normalize
        val normalizedSignal = controlSignal.coerceIn(0.0, 1.0)
        
        // difficultyTarget = 1 + 4 * C_D_norm
        val target = 1.0 + (4.0 * normalizedSignal)
        
        // Stabilization: clamp change to +/- 1
        val roundedTarget = target.roundToInt().coerceIn(1, 5)
        var nextLevel = when {
            roundedTarget > currentLevel + config.maxDifficultyChangePerUpdate -> 
                currentLevel + config.maxDifficultyChangePerUpdate
            roundedTarget < currentLevel - config.maxDifficultyChangePerUpdate -> 
                currentLevel - config.maxDifficultyChangePerUpdate
            else -> roundedTarget
        }.coerceIn(1, 5)

        // Rule: insufficientPerformanceEvidence -> nextDifficulty must not exceed currentDifficulty
        if (insufficientEvidence && nextLevel > currentLevel) {
            nextLevel = currentLevel
        }
        
        return DifficultyResult(
            controlSignal = controlSignal,
            normalizedSignal = normalizedSignal,
            target = target,
            nextLevel = nextLevel
        )
    }

    data class DifficultyResult(
        val controlSignal: Double,
        val normalizedSignal: Double,
        val target: Double,
        val nextLevel: Int
    )
}
