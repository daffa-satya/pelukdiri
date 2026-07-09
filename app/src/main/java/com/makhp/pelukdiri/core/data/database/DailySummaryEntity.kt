package com.makhp.pelukdiri.core.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_summary")
data class DailySummaryEntity(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val totalScreenTimeMillis: Long,
    val unlockCount: Int,
    val mostUsedApp: String?,
    val wellbeingScore: Int?
)
