package com.makhp.pelukdiri.core.domain.model

/**
 * Configuration parameters for the Deviation Engine v0.1.
 *
 * @property alpha Magnitude correction weight. Default: 0.1.
 * @property k Logistic curve slope (steepness). Default: 0.75.
 * @property s0 Logistic midpoint (sensitivity delay). Default: 2.0.
 * @property minimumHistory Minimum required daily observations. Default: 7.
 */
data class DeviationConfig(
    val alpha: Double = 0.1,
    val k: Double = 0.75,
    val s0: Double = 2.0,
    val minimumHistory: Int = 7
)
