package com.makhp.pelukdiri.collector

data class AppUsageInsight(
    val launchCount: Int,
    val longestSessionStartMillis: Long?,
    val longestSessionEndMillis: Long?,
    val longestSequenceDurationMillis: Long? = null,
) {
    val longestSessionDurationMillis: Long
        get() = longestSequenceDurationMillis ?: if (longestSessionStartMillis != null && longestSessionEndMillis != null) {
            longestSessionEndMillis - longestSessionStartMillis
        } else 0L
}
