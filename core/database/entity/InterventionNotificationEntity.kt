package com.makhp.pelukdiri.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "interventions")
data class InterventionNotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val type: String,
    val timestamp: Long,
    val isAcknowledged: Boolean
)
