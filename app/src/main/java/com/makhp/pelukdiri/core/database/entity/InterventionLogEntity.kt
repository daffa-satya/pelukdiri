package com.makhp.pelukdiri.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "intervention_logs")
data class InterventionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val riskScore: Double,
    val difficultyLevel: Int,
    val responseTimeMs: Long,
    val isSuccess: Boolean,
    val isBypassed: Boolean = false,
    val penaltyAppliedMinutes: Int
)
