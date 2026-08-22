package com.makhp.pelukdiri.core.domain.model

/**
 * Global configuration for historical sample sizes and backfill policies.
 * Centralizes historical sample, backfill, and fallback lookback sizes.
 */
object HistoricalConfig {
    /**
     * Default number of historical days to fetch during backfill and adaptive limit calculations.
     */
    const val HISTORY_SAMPLE_DAYS: Int = 14

    /** Number of past calendar days collected by the manual/default backfill. */
    const val BACKFILL_DAYS: Int = HISTORY_SAMPLE_DAYS

    /** Minimum valid observations required before historical calculations are allowed. */
    const val MINIMUM_HISTORY_DAYS: Int = 7

    /**
     * Number of past calendar days to scan when searching for valid historical days (lookback fallback window).
     */
    const val CALENDAR_LOOKBACK_DAYS: Long = 21L

}
