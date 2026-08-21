package com.makhp.pelukdiri.ui.components

fun formatDuration(millis: Long): String {
    val totalMinutes = millis / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        "${hours}j ${minutes}m"
    } else {
        "${minutes} menit"
    }
}
