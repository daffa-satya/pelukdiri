package com.makhp.pelukdiri.core.domain.model

/**
 * Configuration for Control Engine v0.1.
 * Centralizes all tunable parameters for difficulty and frequency control.
 */
data class ControlConfig(
    // Difficulty Sensitivity Strength (0 <= lambda <= 1)
    val lambdaDifficulty: Double = 0.5,
    
    // Frequency Sensitivity Strength (0 <= lambda <= 1)
    val lambdaFrequency: Double = 0.5,
    
    // Performance Correctness Floor (a = 0.5)
    val performanceCorrectnessFloor: Double = 0.5,
    
    // Minimum successful responses to establish a baseline
    val performanceEvidenceWindow: Int = 5,
    
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
    val maxDifficultyChangePerUpdate: Int = 1
)
