package com.makhp.pelukdiri.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_usage",
    indices = [Index(value = ["packageName", "date"], unique = true)]
)
data class AppUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val usageDurationMillis: Long,
    val lastUsedTimestamp: Long,
    val date: String // ISO-8601 format: YYYY-MM-DD
)
