package com.makhp.pelukdiri.core.domain.engine

import com.makhp.pelukdiri.core.domain.model.ControlConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FrequencyController @Inject constructor(
    private val config: ControlConfig,
) {
    /**
     * Calculates the intervention interval in minutes.
     *
     * @param deviation Deviation signal D in [0,1].
     * @param sensitivity Sensitivity modifier Q in [0,1].
     * @return Calculated interval in minutes.
     */
    fun calculate(
        deviation: Double,
        sensitivity: Double,
    ): FrequencyResult {
        // C_F = D + lambda_F * Q * (1 - D)
        val controlSignal = deviation + (config.lambdaFrequency * sensitivity * (1.0 - deviation))
        
        // Normalize
        val normalizedSignal = controlSignal.coerceIn(0.0, 1.0)
        
        // intervalMinutes = 30 - 27 * C_F_norm
        // Range: 3 to 30
        val interval = 30.0 - 27.0 * normalizedSignal
        
        val finalInterval = interval.coerceIn(config.minFrequencyMinutes, config.maxFrequencyMinutes)
        
        return FrequencyResult(
            controlSignal = controlSignal,
            normalizedSignal = normalizedSignal,
            intervalMinutes = finalInterval
        )
    }

    data class FrequencyResult(
        val controlSignal: Double,
        val normalizedSignal: Double,
        val intervalMinutes: Double
    )
}
