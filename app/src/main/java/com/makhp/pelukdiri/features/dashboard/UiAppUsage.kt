package com.makhp.pelukdiri.features.dashboard

import com.makhp.pelukdiri.core.domain.model.AppUsage

data class UiAppUsage(
    val domain: AppUsage,
    val usageDurationYesterdayMillis: Long? = null,
    val openingsToday: Int? = null,
    val openingsYesterday: Int? = null,
    val peakTimeToday: String? = null,
    val peakTimeYesterday: String? = null,
    val longestSessionTodayMillis: Long? = null,
    val longestSessionYesterdayMillis: Long? = null,
    val interventionsToday: Int? = null,
    val interventionsLimit: Int? = 10
) {
    val packageName: String get() = domain.packageName
    val appName: String get() = domain.appName
    val usageDurationMillis: Long get() = domain.usageDurationMillis
    val lastUsedTimestamp: Long get() = domain.lastUsedTimestamp
}
