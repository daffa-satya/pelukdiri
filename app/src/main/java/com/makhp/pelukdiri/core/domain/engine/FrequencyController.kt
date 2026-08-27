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
        adaptiveLimitProgress: Double? = null,
    ): FrequencyResult {
        // C_F = D + lambda_F * Q * (1 - D)
        val controlSignal = deviation + (config.lambdaFrequency * sensitivity * (1.0 - deviation))
        
        // Normalize
        val normalizedSignal = controlSignal.coerceIn(0.0, 1.0)
        
        // Map C_F_norm from the configured maximum interval down to the minimum.
        val intervalRange = config.maxFrequencyMinutes - config.minFrequencyMinutes
        val interval = config.maxFrequencyMinutes - intervalRange * normalizedSignal
        
        val progressFloor = when {
            !config.useAdaptiveLimitFrequencyFloor || adaptiveLimitProgress == null ->
                config.minFrequencyMinutes
            adaptiveLimitProgress < LOW_PROGRESS_THRESHOLD -> LOW_PROGRESS_INTERVAL_MINUTES
            adaptiveLimitProgress < HIGH_PROGRESS_THRESHOLD -> MEDIUM_PROGRESS_INTERVAL_MINUTES
            else -> config.minFrequencyMinutes
        }.coerceIn(config.minFrequencyMinutes, config.maxFrequencyMinutes)
        val finalInterval = interval
            .coerceIn(config.minFrequencyMinutes, config.maxFrequencyMinutes)
            .coerceAtLeast(progressFloor)
        
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

    private companion object {
        const val LOW_PROGRESS_THRESHOLD = 0.5
        const val HIGH_PROGRESS_THRESHOLD = 0.8
        const val LOW_PROGRESS_INTERVAL_MINUTES = 15.0
        const val MEDIUM_PROGRESS_INTERVAL_MINUTES = 10.0
    }
}
