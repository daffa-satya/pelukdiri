package com.makhp.pelukdiri.core.domain.model

data class AppUsage(
    val packageName: String,
    val appName: String,
    val usageDurationMillis: Long,
    val lastUsedTimestamp: Long,
    val iconUri: String? = null
)
