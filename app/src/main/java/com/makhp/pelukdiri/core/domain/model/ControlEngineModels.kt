package com.makhp.pelukdiri.core.domain.model

/**
 * Explainable output for Control Engine decisions.
 */
data class ControlResult(
    val deviation: Double?,
    val performance: Double,
    val qLux: Double,
    val qTime: Double,
    val sensitivity: Double,
    
    val difficultyControl: Double,
    val normalizedDifficultyControl: Double,
    val difficultyTarget: Double,
    val currentDifficulty: Int,
    val nextDifficulty: Int,
    
    val frequencyControl: Double,
    val normalizedFrequencyControl: Double,
    val intervalMinutes: Double,
    
    val nextEligibleInterventionAt: Long,
    val mode: ControlMode
)

enum class ControlMode {
    PERSONALIZED,
    SAFE_DEFAULT,
    INSUFFICIENT_HISTORY
}

/**
 * Raw metrics for performance calculation.
 */
data class PerformanceMetrics(
    val responseTimeMs: Long,
    val isSuccess: Boolean,
    val difficulty: Int
)
