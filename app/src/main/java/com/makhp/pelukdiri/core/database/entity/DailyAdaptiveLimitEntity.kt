package com.makhp.pelukdiri.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_adaptive_limits")
data class DailyAdaptiveLimitEntity(
    @PrimaryKey val dateString: String, // Format: "YYYY-MM-DD"
    val calculatedLimitMinutes: Int,  // Variabel S
    val actualScreenTimeMinutes: Int, // Variabel A
    val reclaimedTimeMinutes: Int
)
