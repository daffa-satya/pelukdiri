package com.makhp.pelukdiri.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.makhp.pelukdiri.R
import com.makhp.pelukdiri.features.analytics.AnalyticsPeriod
import com.makhp.pelukdiri.features.analytics.AnalyticsUiState
import com.makhp.pelukdiri.features.analytics.AnalyticsViewModel
import com.makhp.pelukdiri.ui.components.AppIcon
import com.makhp.pelukdiri.ui.components.formatDuration
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllAppsScreen(
    selectedDate: LocalDate,
    selectedPeriod: AnalyticsPeriod,
    onBackClick: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedApp by remember { mutableStateOf<UiAppUsage?>(null) }

    LaunchedEffect(selectedDate, selectedPeriod) {
        viewModel.load(selectedDate, selectedPeriod)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.all_apps_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is AnalyticsUiState.Success -> if (
                state.selectedDate == selectedDate && state.selectedPeriod == selectedPeriod
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.topApps) { app ->
                        AllAppUsageRow(
                            app = app,
                            totalScreenTimeMillis = state.summary?.totalScreenTimeMillis ?: 0L,
                            onClick = { selectedApp = app }
                        )
                    }
                }
                
                selectedApp?.let { app ->
                    AppDetailBottomSheet(
                        app = app,
                        onDismiss = { selectedApp = null },
                        onViewFullAnalytics = {
                            selectedApp = null
                            onNavigateToAnalytics()
                        }
                    )
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AnalyticsUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AnalyticsUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message)
                }
            }
        }
    }
}

@Composable
private fun AllAppUsageRow(
    app: UiAppUsage,
    totalScreenTimeMillis: Long,
    onClick: () -> Unit
) {
    val progress = appUsageShare(app.usageDurationMillis, totalScreenTimeMillis)
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(app.packageName, app.appName, size = 48.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(app.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().height(6.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.outlineVariant)) {
                    Box(Modifier.fillMaxWidth(progress).height(6.dp).background(MaterialTheme.colorScheme.primary))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(formatDuration(app.usageDurationMillis), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

internal fun appUsageShare(appUsageMillis: Long, totalScreenTimeMillis: Long): Float =
    if (totalScreenTimeMillis <= 0L) 0f
    else (appUsageMillis.toFloat() / totalScreenTimeMillis).coerceIn(0f, 1f)
