package com.makhp.pelukdiri.features.dashboard

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.makhp.pelukdiri.collector.AppUsageCollector

@Composable
fun MainStatsScreen(activityContext: Context, collector: AppUsageCollector) {
    var isPermissionGranted by remember { mutableStateOf(collector.isPermissionGranted()) }
    var statsText by remember { mutableStateOf("Loading statistics...") }

    // 1. Get access to the current Android lifecycle owner
    val lifecycleOwner = LocalLifecycleOwner.current

    // 2. This hook watches when the user leaves or comes BACK to the app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            // ON_RESUME fires every single time the app screen becomes visible again
            if (event == Lifecycle.Event.ON_RESUME) {
                // Re-check the actual system setting status right now
                isPermissionGranted = collector.isPermissionGranted()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 3. This updates the text as soon as the check in step 2 switches to true
    LaunchedEffect(isPermissionGranted) {
        statsText = if (isPermissionGranted) {
            collector.fetchRecentEventsPlainText(6)
        } else {
            "Permission is required to view usage statistics."
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "App Usage Statistics Log", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (!isPermissionGranted) {
            Button(onClick = {
                // Fix: Use ONLY Settings.ACTION_USAGE_ACCESS_SETTINGS
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                activityContext.startActivity(intent)
            }) {
                Text("Grant Usage Permission")
            }
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Button(onClick = { statsText = collector.fetchRecentEventsPlainText(6) }) {
                Text("Refresh Data")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Text(text = statsText, style = MaterialTheme.typography.bodyMedium)
        }
    }
}