package com.makhp.pelukdiri.core.domain.model

/**
 * Result of the Deviation Engine calculation.
 *
 * @property deviation Final signal D ∈ [0,1]. Null if calculation is not possible (e.g., insufficient history).
 * @property baseline Calculated median of history (B).
 * @property mad Calculated Median Absolute Deviation (M).
 * @property signal Combined signal S = R(1 + αA).
 * @property relativeDeviation R = Δ+ / M.
 * @property relativeMagnitude A = Δ+ / B.
 * @property status Calculation status.
 */
data class DeviationResult(
    val deviation: Double?,
    val baseline: Double?,
    val mad: Double?,
    val signal: Double?,
    val relativeDeviation: Double?,
    val relativeMagnitude: Double?,
    val status: DeviationStatus
)

sealed interface DeviationStatus {
    /** Calculation completed successfully. */
    data object Success : DeviationStatus
    /** History size is below the required threshold (e.g., 7 days). */
    data object InsufficientHistory : DeviationStatus
}
