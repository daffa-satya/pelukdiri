package com.makhp.pelukdiri.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "intervention_logs")
data class InterventionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val targetPackageName: String,
    val questionType: String,         // ARITHMETIC, LOGIC, PATTERN, REFLECTION
    val difficultyLevel: String,      // EASY, MEDIUM, HARD (Variabel D)
    val responseTimeMs: Long,         // Waktu tempuh jawab (Variabel T)
    val isCorrect: Boolean,
    val isBypassed: Boolean           // Apakah user menyerah/membatalkan
)
