package com.makhp.pelukdiri.core.domain.model

/**
 * Configuration parameters for the Adaptive Limit generation.
 *
 * @property beta Variance weight (multiplier for MAD). Default: 1.0 (v0.1).
 */
data class AdaptiveLimitConfig(
    val beta: Double = 1.0
)
