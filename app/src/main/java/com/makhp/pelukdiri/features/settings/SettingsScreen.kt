package com.makhp.pelukdiri.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.makhp.pelukdiri.R
import com.makhp.pelukdiri.features.dashboard.DashboardTokens
import com.makhp.pelukdiri.ui.components.PelukDiriLogo
import com.makhp.pelukdiri.ui.components.PelukCard
import com.makhp.pelukdiri.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onHomeClick: () -> Unit,
    onProgressClick: () -> Unit,
    onNavigateToAdaptiveMode: () -> Unit,
    onNavigateToApps: () -> Unit,
    onNavigateToInformedConsent: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onExitApp: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showExportDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf<String?>(null) } // "sleep" or "wake"
    
    SettingsLayout(
        state = state,
        onHomeClick = onHomeClick,
        onProgressClick = onProgressClick,
        onNavigateToAdaptiveMode = onNavigateToAdaptiveMode,
        onNavigateToApps = onNavigateToApps,
        onNavigateToInformedConsent = onNavigateToInformedConsent,
        onSleepTimeClick = { showTimePickerDialog = "sleep" },
        onWakeTimeClick = { showTimePickerDialog = "wake" },
        onExportClick = { showExportDialog = true },
        onNavigateToAbout = onNavigateToAbout,
        onLogout = onExitApp
    )

    if (showTimePickerDialog != null) {
        val initialTime = if (showTimePickerDialog == "sleep") state.sleepTime else state.wakeTime
        val parts = initialTime.split(":")
        val timePickerState = rememberTimePickerState(
            initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 22,
            initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePickerDialog = null },
            title = { 
                Text(
                    stringResource(
                        if (showTimePickerDialog == "sleep") R.string.settings_sleep_time_label 
                        else R.string.settings_wake_time_label
                    ),
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val formatted = "%02d:%02d".format(timePickerState.hour, timePickerState.minute)
                    if (showTimePickerDialog == "sleep") viewModel.setSleepTime(formatted)
                    else viewModel.setWakeTime(formatted)
                    showTimePickerDialog = null
                }) {
                    Text(stringResource(R.string.profile_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = null }) {
                    Text(stringResource(R.string.profile_cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(stringResource(R.string.settings_export_confirm_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.settings_export_confirm_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        showExportDialog = false
                        viewModel.exportDatabase()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.settings_export_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(stringResource(R.string.profile_cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (state.isExporting) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.settings_export_progress_title)) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                    Spacer(Modifier.width(Dimens.spaceMedium))
                    Text(stringResource(R.string.settings_export_progress_msg))
                }
            },
            confirmButton = {},
        )
    }

    state.exportedFilePath?.let { path ->
        AlertDialog(
            onDismissRequest = viewModel::clearExportResult,
            title = { Text(stringResource(R.string.settings_export_success_title)) },
            text = { Text(stringResource(R.string.settings_export_success_msg, path)) },
            confirmButton = {
                Button(onClick = viewModel::clearExportResult) {
                    Text(stringResource(R.string.settings_export_close))
                }
            },
        )
    }

    state.exportError?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::clearExportResult,
            title = { Text(stringResource(R.string.export_failed)) },
            text = { Text(error) },
            confirmButton = {
                Button(onClick = viewModel::clearExportResult) {
                    Text(stringResource(R.string.settings_export_close))
                }
            },
        )
    }
}

@Composable
private fun SettingsLayout(
    state: SettingsUiState,
    onHomeClick: () -> Unit,
    onProgressClick: () -> Unit,
    onNavigateToAdaptiveMode: () -> Unit,
    onNavigateToApps: () -> Unit,
    onNavigateToInformedConsent: () -> Unit,
    onSleepTimeClick: () -> Unit,
    onWakeTimeClick: () -> Unit,
    onExportClick: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            SettingsNavigation(onHomeClick, onProgressClick)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(DashboardTokens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardTokens.LargeGap)
        ) {
            item(key = "header") { SettingsHeader() }
            
            item(key = "preferences") {
                SettingsSection(title = stringResource(R.string.settings_preferences_section)) {

                    SettingsItem(
                        title = stringResource(R.string.settings_sleep_time_title),
                        description = stringResource(R.string.settings_sleep_time_desc),
                        icon = Icons.Default.Bedtime,
                        onClick = onSleepTimeClick,
                        trailing = {
                            Text(
                                text = state.sleepTime,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                    SettingsDivider()
                    SettingsItem(
                        title = stringResource(R.string.settings_wake_time_title),
                        description = stringResource(R.string.settings_wake_time_desc),
                        icon = Icons.Default.LightMode,
                        onClick = onWakeTimeClick,
                        trailing = {
                            Text(
                                text = state.wakeTime,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                    SettingsDivider()
                    SettingsItem(
                        title = stringResource(R.string.settings_monitored_apps_title),
                        description = stringResource(R.string.settings_monitored_apps_desc),
                        icon = Icons.Default.Apps,
                        onClick = onNavigateToApps
                    )
                    SettingsDivider()
                    SettingsItem(
                        title = stringResource(R.string.settings_export_csv_title),
                        description = stringResource(R.string.settings_export_csv_desc),
                        icon = Icons.Default.FileDownload,
                        onClick = onExportClick
                    )
                }
            }
            
            item(key = "info") {
                SettingsSection(title = stringResource(R.string.settings_info_section)) {
                    SettingsItem(
                        title = stringResource(R.string.settings_informed_consent_title),
                        description = stringResource(R.string.settings_informed_consent_desc),
                        icon = Icons.Default.Assignment,
                        onClick = onNavigateToInformedConsent
                    )
                    SettingsDivider()
                    SettingsItem(
                        title = stringResource(R.string.settings_about_title),
                        description = stringResource(R.string.settings_about_desc),
                        icon = Icons.Default.Info,
                        onClick = onNavigateToAbout
                    )
                }
            }
            
            item(key = "logout") {
                PelukCard(
                    modifier = Modifier.fillMaxWidth().clickable { onLogout() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(Dimens.minTouchTarget - Dimens.spaceSmall)
                                .clip(RoundedCornerShape(DashboardTokens.SmallRadius))
                                .background(MaterialTheme.colorScheme.errorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(Modifier.width(DashboardTokens.MediumGap))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_logout_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.settings_logout_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            item(key = "bottom_spacer") { Spacer(Modifier.height(DashboardTokens.LargeGap)) }
        }
    }
}

@Composable
private fun SettingsHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.spaceSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(Dimens.minTouchTarget))
        Text(
            text = stringResource(R.string.settings_title),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium
        )
        Box(Modifier.size(Dimens.minTouchTarget), contentAlignment = Alignment.Center) {
            PelukDiriLogo(size = 28.dp)
        }
    }
}

@Composable
private fun SettingsNavigation(
    onHomeClick: () -> Unit,
    onProgressClick: () -> Unit
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NavigationBarItem(
            selected = false,
            onClick = onHomeClick,
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.dashboard_home)) }
        )
        NavigationBarItem(
            selected = false,
            onClick = onProgressClick,
            icon = { Icon(Icons.Default.PieChart, contentDescription = null) },
            label = { Text(stringResource(R.string.dashboard_progress)) }
        )
        NavigationBarItem(
            selected = true,
            onClick = {},
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.dashboard_settings)) }
        )
    }
}
