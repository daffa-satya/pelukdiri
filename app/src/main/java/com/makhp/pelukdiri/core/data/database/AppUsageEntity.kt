package com.makhp.pelukdiri.core.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage")
data class AppUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val usageDurationMillis: Long,
    val lastUsedTimestamp: Long,
    val date: String // ISO-8601 format: YYYY-MM-DD
)
