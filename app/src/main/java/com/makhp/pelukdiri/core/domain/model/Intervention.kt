package com.makhp.pelukdiri.core.domain.model

import java.time.LocalDateTime

data class Intervention(
    val id: String,
    val title: String,
    val message: String,
    val type: InterventionType,
    val timestamp: LocalDateTime,
    val isAcknowledged: Boolean = false
)

enum class InterventionType {
    NUDGE,      // Gentle reminder
    WARNING,    // Usage limit reached
    ENCOURAGEMENT // Positive feedback for good behavior
}
