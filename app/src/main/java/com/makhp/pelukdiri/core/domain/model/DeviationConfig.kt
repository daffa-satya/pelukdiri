package com.makhp.pelukdiri.core.domain.model

/**
 * Configuration parameters for a deviation policy.
 *
 * @property alpha Magnitude correction weight. Default: 0.1.
 * @property k Logistic curve slope (steepness). Default: 0.25.
 * @property s0 Logistic midpoint (sensitivity delay). Default: 3.0.
 * @property minimumMadMinutes Absolute effective-MAD floor. Default: 1 minute.
 * @property minimumMadFractionOfBaseline Relative effective-MAD floor. Default: 20%.
 * @property minimumHistory Minimum required daily observations. Default: 7.
 */
data class DeviationConfig(
    val alpha: Double = 0.1,
    val k: Double = 0.25,
    val s0: Double = 3.0,
    val minimumMadMinutes: Double = 1.0,
    val minimumMadFractionOfBaseline: Double = 0.2,
    val minimumHistory: Int = HistoricalConfig.MINIMUM_HISTORY_DAYS
) {
    companion object {
        /** Pre-tuning deviation constants, retained as a comparison baseline. */
        val LEGACY_DEFAULT = DeviationConfig(
            k = 0.75,
            s0 = 2.0,
            minimumMadMinutes = 0.0,
            minimumMadFractionOfBaseline = 0.0,
        )

        /** First synthetic tuning result; also represented by constructor defaults. */
        val CANDIDATE_1 = DeviationConfig()

        /** Selected production deviation constants. */
        val CANDIDATE_3 = DeviationConfig(
            k = 0.3,
            s0 = 2.0,
            minimumMadMinutes = 1.0,
            minimumMadFractionOfBaseline = 0.5,
        )
    }
}
