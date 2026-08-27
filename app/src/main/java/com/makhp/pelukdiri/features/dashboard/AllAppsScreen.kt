package com.makhp.pelukdiri.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.makhp.pelukdiri.R
import com.makhp.pelukdiri.features.analytics.AnalyticsPeriod
import com.makhp.pelukdiri.features.analytics.AnalyticsUiState
import com.makhp.pelukdiri.features.analytics.AnalyticsViewModel
import com.makhp.pelukdiri.features.analytics.EditAppUsageDialog
import com.makhp.pelukdiri.ui.components.AppIcon
import com.makhp.pelukdiri.ui.components.formatDuration
import com.makhp.pelukdiri.ui.theme.Dimens
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val allAppsDateFormatter =
    DateTimeFormatter.ofPattern("EEEE, dd MMM", Locale.forLanguageTag("id-ID"))

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
    val canEditUsage = (uiState as? AnalyticsUiState.Success)?.let { state ->
        state.selectedDate == selectedDate &&
            state.selectedPeriod == selectedPeriod &&
            state.canEditUsage
    } == true
    var selectedApp by remember { mutableStateOf<UiAppUsage?>(null) }
    var appBeingEdited by remember { mutableStateOf<UiAppUsage?>(null) }
    var showAddAppSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val editUsageError = (uiState as? AnalyticsUiState.Success)?.editUsageError

    LaunchedEffect(editUsageError) {
        editUsageError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearEditUsageError()
        }
    }

    LaunchedEffect(selectedDate, selectedPeriod) {
        viewModel.load(selectedDate, selectedPeriod)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            R.string.all_apps_title,
                            selectedDate.format(allAppsDateFormatter),
                        ),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (canEditUsage) {
                        TextButton(
                            onClick = {
                                viewModel.loadInstalledApps()
                                showAddAppSheet = true
                            }
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.all_apps_add_app))
                        }
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
                            onEdit = if (canEditUsage) {
                                { appBeingEdited = app }
                            } else null,
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

                appBeingEdited?.let { app ->
                    EditAppUsageDialog(
                        app = app,
                        onDismiss = { appBeingEdited = null },
                        onSave = { durationMillis ->
                            viewModel.updateAppUsage(app.packageName, app.appName, durationMillis)
                            appBeingEdited = null
                        },
                    )
                }

                if (showAddAppSheet) {
                    AddAppBottomSheet(
                        installedApps = state.allInstalledApps,
                        isLoading = state.isInstalledAppsLoading,
                        error = state.installedAppsError,
                        onDismiss = { showAddAppSheet = false },
                        onRetry = viewModel::loadInstalledApps,
                        onEdit = {
                            appBeingEdited = it
                            showAddAppSheet = false
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAppBottomSheet(
    installedApps: List<UiAppUsage>,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onEdit: (UiAppUsage) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(installedApps, searchQuery) {
        installedApps.filter { it.appName.contains(searchQuery, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.all_apps_installed_not_used),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                placeholder = { Text(stringResource(R.string.common_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(Dimens.cardCornerRadius)
            )

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry) {
                        Text(stringResource(R.string.common_retry))
                    }
                }
            } else if (filteredApps.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.all_apps_no_apps_available))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(filteredApps) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIcon(app.packageName, app.appName, size = 40.dp)
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = app.appName,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(onClick = { onEdit(app) }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.analytics_edit_usage),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AllAppUsageRow(
    app: UiAppUsage,
    totalScreenTimeMillis: Long,
    onEdit: (() -> Unit)? = null,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onEdit != null) {
                        IconButton(onClick = onEdit) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = stringResource(R.string.analytics_edit_usage),
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

internal fun appUsageShare(appUsageMillis: Long, totalScreenTimeMillis: Long): Float =
    if (totalScreenTimeMillis <= 0L) 0f
    else (appUsageMillis.toFloat() / totalScreenTimeMillis).coerceIn(0f, 1f)
