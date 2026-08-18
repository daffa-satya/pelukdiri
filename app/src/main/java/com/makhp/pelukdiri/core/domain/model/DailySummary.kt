package com.makhp.pelukdiri.core.domain.model

import java.time.LocalDate

data class DailySummary(
    val date: LocalDate,
    val totalScreenTimeMillis: Long,
    val totalScreenOnMillis: Long,
    val unlockCount: Int,
    val mostUsedApp: String?,
    val wellbeingScore: Int? = null
)
