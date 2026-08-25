package com.makhp.pelukdiri.core.domain.model

/**
 * Configuration for an intervention control policy.
 * Centralizes all tunable parameters for difficulty and frequency control.
 */
data class ControlConfig(
    // Difficulty Sensitivity Strength (0 <= lambda <= 1)
    val lambdaDifficulty: Double = 0.35,
    
    // Frequency Sensitivity Strength (0 <= lambda <= 1)
    val lambdaFrequency: Double = 0.35,
    
    // Performance Correctness Floor (a = 0.5)
    val performanceCorrectnessFloor: Double = 0.5,
    
    // Prior consecutive successful responses required to establish a speed baseline
    val performanceEvidenceWindow: Int = 5,

    // Consecutive failed responses required before decreasing difficulty
    val difficultyDecreaseEvidenceWindow: Int = 0,
    
    // Sleep Sensitivity Ramp (minutes before bedtime)
    val sleepSensitivityRampMinutes: Int = 90,
    
    // Lux Reference Bounds
    val luxDarkReference: Float = 10f,
    val luxBrightReference: Float = 500f,
    
    // Frequency Limits (minutes)
    val minFrequencyMinutes: Double = 3.0,
    val maxFrequencyMinutes: Double = 30.0,
    val defaultFrequencyMinutes: Double = 15.0,
    
    // Default Difficulty
    val defaultDifficulty: Int = 2,
    
    // Maximum Difficulty Change per update
    val maxDifficultyChangePerUpdate: Int = 1,

    // Valid answered interventions required before reversing direction again
    val reversalGuardInterventions: Int = 3,
) {
    companion object {
        const val POLICY_VERSION = "v1.1-failure-streak-decrease"

        /** Pre-tuning control constants, retained as a safe comparison baseline. */
        val LEGACY_DEFAULT = ControlConfig(
            lambdaDifficulty = 0.5,
            lambdaFrequency = 0.5,
        )

        /** First synthetic tuning result; also represented by constructor defaults. */
        val CANDIDATE_1 = ControlConfig()

        /** Selected production control constants. */
        val CANDIDATE_3 = ControlConfig(
            lambdaDifficulty = 0.2,
            lambdaFrequency = 1.0,
            performanceEvidenceWindow = 2,
            difficultyDecreaseEvidenceWindow = 3,
        )
    }
}
