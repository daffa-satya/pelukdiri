package com.makhp.pelukdiri.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.makhp.pelukdiri.R

fun formatDuration(context: Context, millis: Long): String {
    val totalMinutes = millis / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val localizedContext = ContextCompat.getContextForLanguage(context)
    return if (hours > 0) {
        localizedContext.resources.getQuantityString(R.plurals.duration_hours, hours.toInt(), hours) + " " +
            localizedContext.resources.getQuantityString(R.plurals.duration_minutes, minutes.toInt(), minutes)
    } else {
        localizedContext.resources.getQuantityString(R.plurals.duration_minutes, minutes.toInt(), minutes)
    }
}

@Composable
fun formatDuration(millis: Long): String = formatDuration(LocalContext.current, millis)
