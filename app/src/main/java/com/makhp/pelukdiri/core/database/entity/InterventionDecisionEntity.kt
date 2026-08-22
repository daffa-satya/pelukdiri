package com.makhp.pelukdiri.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "intervention_decisions")
data class InterventionDecisionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val packageName: String,
    val monitoredUsageMinutes: Double,
    val totalUsageMinutes: Double,
    val ambientLux: Float,
    val historyCount: Int,
    val baselineMedianMinutes: Double?,
    val madMinutes: Double?,
    val deviationSignal: Double?,
    val relativeDeviation: Double?,
    val relativeMagnitude: Double?,
    val deviation: Double?,
    val performance: Double?,
    val qLux: Double?,
    val qTime: Double?,
    val sensitivity: Double?,
    val difficultyControl: Double?,
    val difficultyControlSignal: Double?,
    val difficultyTarget: Double?,
    val currentDifficulty: Int,
    val nextDifficulty: Int?,
    val challengeType: String?,
    val frequencyControl: Double?,
    val normalizedFrequencyControl: Double?,
    val proposedIntervalMinutes: Double?,
    val nextEligibleAt: Long?,
    val shouldTrigger: Boolean,
    val reason: String,
    val controlMode: String?,
    val errorType: String?,
)
