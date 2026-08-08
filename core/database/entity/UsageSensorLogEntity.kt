package com.makhp.pelukdiri.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_sensor_logs")
data class UsageSensorLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val packageName: String,
    val rawScreenTimeMs: Long,      // Untuk perhitungan H
    val appOpeningFrequency: Int,    // Untuk perhitungan F
    val ambientLightLux: Float       // Variabel L
)
