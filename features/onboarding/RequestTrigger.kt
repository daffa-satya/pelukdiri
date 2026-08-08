package com.makhp.pelukdiri.features.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.makhp.pelukdiri.collector.isUsageStatsPermissionGranted

@Composable
fun UsagePermissionPrompt() {
    val context = LocalContext.current

    Button(onClick = {
        // Double check if they already turned it on
        if (!isUsageStatsPermissionGranted(context)) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }) {
        Text("Grant Usage Access")
    }
}