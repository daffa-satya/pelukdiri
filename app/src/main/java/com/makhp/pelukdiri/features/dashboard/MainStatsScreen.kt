package com.makhp.pelukdiri.features.dashboard

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.makhp.pelukdiri.R
import com.makhp.pelukdiri.ui.components.PelukDiriLogo
import com.makhp.pelukdiri.core.domain.model.DailySummary
import com.makhp.pelukdiri.features.dashboard.UiAppUsage
import com.makhp.pelukdiri.features.intervention.InterventionActivity
import com.makhp.pelukdiri.ui.components.*
import com.makhp.pelukdiri.ui.theme.Dimens
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.max

@Composable
fun MainStatsScreen(
    onProgressClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onViewAllClick: () -> Unit,
    onOnboardingClick: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ProfileSidebar(
                viewModel = profileViewModel,
                onClose = { scope.launch { drawerState.close() } },
                onOnboardingClick = {
                    scope.launch { drawerState.close() }
                    onOnboardingClick()
                },
                onInterventionClick = {
                    if (profileViewModel.tryAcquireInterventionLock()) {
                        scope.launch { drawerState.close() }
                        val intent = Intent(context, InterventionActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                            putExtra(InterventionActivity.EXTRA_PACKAGE_NAME, context.packageName)
                            putExtra(InterventionActivity.EXTRA_MONITORED_USAGE, 120.0)
                            putExtra(InterventionActivity.EXTRA_LAUNCH_FREQ, 20.0)
                            putExtra(InterventionActivity.EXTRA_AMBIENT_LUX, 100f)
                            putExtra(InterventionActivity.EXTRA_DEVIATION, 0.5)
                            putExtra(InterventionActivity.EXTRA_DIFFICULTY_CONTROL_SIGNAL, 0.7)
                            putExtra(InterventionActivity.EXTRA_DIFFICULTY, 3)
                        }
                        context.startActivity(intent)
                    }
                },
                onPatternInterventionClick = {
                    if (profileViewModel.tryAcquireInterventionLock()) {
                        scope.launch { drawerState.close() }
                        val intent = Intent(context, InterventionActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP or 
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                            putExtra(InterventionActivity.EXTRA_PACKAGE_NAME, context.packageName)
                            putExtra(InterventionActivity.EXTRA_MONITORED_USAGE, 120.0)
                            putExtra(InterventionActivity.EXTRA_LAUNCH_FREQ, 20.0)
                            putExtra(InterventionActivity.EXTRA_AMBIENT_LUX, 100f)
                            putExtra(InterventionActivity.EXTRA_DEVIATION, 0.5)
                            putExtra(InterventionActivity.EXTRA_DIFFICULTY_CONTROL_SIGNAL, 0.7)
                            putExtra(InterventionActivity.EXTRA_DIFFICULTY, 3)
                            putExtra(InterventionActivity.EXTRA_CHALLENGE_TYPE, com.makhp.pelukdiri.core.domain.engine.InterventionChallengeType.PATTERN.name)
                        }
                        context.startActivity(intent)
                    }
                }
            )
        }
    ) {
        DashboardScaffold(
            uiState = uiState,
            profileState = profileState,
            snackbarHostState = snackbarHostState,
            onRefresh = viewModel::forceRefresh,
            onBackfill = viewModel::backfillHistory,
            onRecalculateAdaptiveLimit = viewModel::recalculateAdaptiveLimit,
            onGrantUsageAccess = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
            onGrantAccessibility = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            onGrantBatteryExemption = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    })
                }
            },
            onRetry = viewModel::loadData,
            onProgressClick = onProgressClick,
            onSettingsClick = onSettingsClick,
            onViewAllClick = onViewAllClick,
            onMenuClick = { scope.launch { drawerState.open() } }
        )
    }
}

@Composable
private fun DashboardScaffold(
    uiState: DashboardUiState,
    profileState: ProfileUiState,
    snackbarHostState: SnackbarHostState,
    onRefresh: () -> Unit,
    onBackfill: () -> Unit,
    onRecalculateAdaptiveLimit: () -> Unit,
    onGrantUsageAccess: () -> Unit,
    onGrantAccessibility: () -> Unit,
    onGrantBatteryExemption: () -> Unit,
    onRetry: () -> Unit,
    onProgressClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onViewAllClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { DashboardNavigation(onProgressClick, onSettingsClick) }
    ) { paddingValues ->
        when (uiState) {
            DashboardUiState.Loading -> Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is DashboardUiState.Error -> DashboardError(Modifier.padding(paddingValues), uiState.message, onRetry)
            is DashboardUiState.Success -> DashboardContent(
                state = uiState,
                profileState = profileState,
                modifier = Modifier.padding(paddingValues),
                onRefresh = onRefresh,
                onBackfill = onBackfill,
                onRecalculateAdaptiveLimit = onRecalculateAdaptiveLimit,
                onGrantUsageAccess = onGrantUsageAccess,
                onGrantAccessibility = onGrantAccessibility,
                onGrantBatteryExemption = onGrantBatteryExemption,
                onViewAllClick = onViewAllClick,
                onNavigateToAnalytics = onProgressClick,
                onMenuClick = onMenuClick
            )
        }
    }
}

@Composable
private fun DashboardContent(
    state: DashboardUiState.Success,
    profileState: ProfileUiState,
    modifier: Modifier,
    onRefresh: () -> Unit,
    onBackfill: () -> Unit,
    onRecalculateAdaptiveLimit: () -> Unit,
    onGrantUsageAccess: () -> Unit,
    onGrantAccessibility: () -> Unit,
    onGrantBatteryExemption: () -> Unit,
    onViewAllClick: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onMenuClick: () -> Unit
) {
    var selectedApp by remember { mutableStateOf<UiAppUsage?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(DashboardTokens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(DashboardTokens.LargeGap)
    ) {
        item(key = "header") { 
            DashboardHeader(
                profileState = profileState,
                onMenuClick = onMenuClick
            ) 
        }
        if (!state.isPermissionGranted) item(key = "perm_usage") { PermissionNotice(stringResource(R.string.dashboard_usage_access_needed), stringResource(R.string.dashboard_open_permission), onGrantUsageAccess) }
        if (!state.isAccessibilityEnabled) item(key = "perm_acc") { PermissionNotice(stringResource(R.string.dashboard_accessibility_needed), stringResource(R.string.dashboard_enable), onGrantAccessibility) }
        if (!state.isBatteryOptimizationIgnored) item(key = "perm_batt") { PermissionNotice(stringResource(R.string.dashboard_battery_optimization_needed), stringResource(R.string.dashboard_allow), onGrantBatteryExemption) }
        item(key = "screentime") {
            ScreenTimeCard(
                usageMillis = state.socialMediaUsageMillis,
                yesterdayMillis = state.yesterdaySocialMediaUsageMillis,
                adaptiveLimitMinutes = state.todayAdaptiveLimit,
                isRecalculatingLimit = state.isRecalculatingAdaptiveLimit,
                adaptiveLimitError = state.adaptiveLimitError,
                onRecalculateLimit = onRecalculateAdaptiveLimit
            )
        }
        item(key = "data_actions") {
            DashboardDataActions(
                isRefreshing = state.isRefreshing,
                isBackfilling = state.isBackfilling,
                onRefresh = onRefresh,
                onBackfill = onBackfill
            )
        }
        item(key = "weekly_chart") { WeeklyChart(state.weeklySummaries) }

        item(key = "top_apps") {
            TopAppsCard(
                apps = state.topApps,
                onViewAllClick = onViewAllClick,
                onAppClick = { selectedApp = it }
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
}

@Composable
private fun DashboardHeader(
    profileState: ProfileUiState,
    onMenuClick: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(DashboardTokens.AppIconSize).clickable(onClick = onMenuClick),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            if (profileState.profileImagePath != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(profileState.profileImagePath)
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(R.string.profile_image_description),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, null, modifier = Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(Modifier.width(DashboardTokens.MediumGap))
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.dashboard_header_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.size(Dimens.minTouchTarget))
    }
}

@Composable
private fun DashboardDataActions(
    isRefreshing: Boolean,
    isBackfilling: Boolean,
    onRefresh: () -> Unit,
    onBackfill: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DashboardTokens.MediumGap)
    ) {
        DashboardDataActionCard(
            label = stringResource(R.string.dashboard_sync_now),
            icon = Icons.Default.Sync,
            isLoading = isRefreshing,
            enabled = !isRefreshing && !isBackfilling,
            onClick = onRefresh,
            modifier = Modifier.weight(1f),
        )
        DashboardDataActionCard(
            label = stringResource(R.string.dashboard_backfill_14_days),
            icon = Icons.Default.History,
            isLoading = isBackfilling,
            enabled = !isRefreshing && !isBackfilling,
            onClick = onBackfill,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DashboardDataActionCard(
    label: String,
    icon: ImageVector,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PelukCard(modifier = modifier.clickable(enabled = enabled, onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.size(DashboardTokens.MediumGap), strokeWidth = 2.dp)
            } else {
                Icon(icon, contentDescription = null, modifier = Modifier.size(DashboardTokens.MediumGap))
            }
            Spacer(Modifier.width(DashboardTokens.SmallGap))
            Text(
                text = label,
                maxLines = 2,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ScreenTimeCard(
    usageMillis: Long,
    yesterdayMillis: Long,
    adaptiveLimitMinutes: Int?,
    isRecalculatingLimit: Boolean,
    adaptiveLimitError: String?,
    onRecalculateLimit: () -> Unit
) {
    val usage = usageMillis
    val adaptiveLimitMillis = adaptiveLimitMinutes?.let { it * 60_000L } ?: 0L
    val progress = remember(usage, adaptiveLimitMillis) {
        if (adaptiveLimitMillis == 0L) 0f else (usage.toFloat() / adaptiveLimitMillis).coerceIn(0f, 1f)
    }
    val previous = yesterdayMillis
    val change = remember(usage, previous) {
        if (previous == 0L) 0 else (((usage - previous) * 100) / previous).toInt()
    }
    val usageFormatted = remember(usage) { formatDuration(usage) }
    val limitFormatted = if (adaptiveLimitMinutes != null) {
        formatDuration(adaptiveLimitMillis)
    } else {
        stringResource(R.string.dashboard_insufficient_data)
    }
    val changeTxt = remember(change) { 
        if (change <= 0) -change else change
    }
    val changeLabel = if (change <= 0) stringResource(R.string.dashboard_from_yesterday, changeTxt) else stringResource(R.string.dashboard_more_than_yesterday, changeTxt)

    PelukCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1.2f)) {
                Text(stringResource(R.string.dashboard_screen_time_today), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text(usageFormatted, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (change <= 0) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, 
                        null, 
                        tint = if (change <= 0) Color(0xFF4CAF50) else Color(0xFFE53935), 
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(changeLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.width(60.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.dashboard_adaptive_limit), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(limitFormatted, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                TextButton(
                    onClick = onRecalculateLimit,
                    enabled = !isRecalculatingLimit,
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    if (isRecalculatingLimit) {
                        CircularProgressIndicator(Modifier.size(DashboardTokens.MediumGap), strokeWidth = 2.dp)
                        Spacer(Modifier.width(DashboardTokens.SmallGap))
                    }
                    Text(stringResource(R.string.dashboard_recalculate_limit))
                }
                adaptiveLimitError?.let { error ->
                    Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                FreedomRing(progress, Modifier.size(DashboardTokens.RingSize))
            }
        }
    }
}

@Composable
private fun TopAppsCard(
    apps: List<UiAppUsage>,
    onViewAllClick: () -> Unit,
    onAppClick: (UiAppUsage) -> Unit
) {
    PelukCard {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onViewAllClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.dashboard_top_apps_today), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.dashboard_view_all), style = MaterialTheme.typography.labelLarge)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(DashboardTokens.MediumGap), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(DashboardTokens.MediumGap))
        if (apps.isEmpty()) {
            Text(stringResource(R.string.dashboard_no_usage_data), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else apps.take(3).forEach { app -> 
            AppUsageRow(
                app = app, 
                onClick = { onAppClick(app) }
            ) 
        }
    }
}

@Composable
private fun AppUsageRow(
    app: UiAppUsage, 
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = DashboardTokens.SmallGap).clickable(onClick = onClick), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(app.packageName, app.appName, size = DashboardTokens.AppIconSize)
        Spacer(Modifier.width(DashboardTokens.MediumGap))
        Text(app.appName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(DashboardTokens.MediumGap))
        Text(formatDuration(app.usageDurationMillis), style = MaterialTheme.typography.labelLarge)
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.padding(start = DashboardTokens.SmallGap).size(DashboardTokens.MediumGap), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EncouragementCard() {
    PelukCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(Dimens.buttonHeight).clip(RoundedCornerShape(DashboardTokens.LargeRadius)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { 
                PelukDiriLogo(size = 32.dp)
            }
            Spacer(Modifier.width(DashboardTokens.CardPadding))
            Text(stringResource(R.string.dashboard_encouragement), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PermissionNotice(message: String, action: String, onAction: () -> Unit) {
    Surface(shape = RoundedCornerShape(DashboardTokens.MediumRadius), color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth().clickable(onClick = onAction)) {
        Row(Modifier.padding(DashboardTokens.CardPadding), verticalAlignment = Alignment.CenterVertically) {
            Text(message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            Text(action, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun DashboardNavigation(
    onProgressClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Home, null) }, label = { Text(stringResource(R.string.dashboard_home)) })
        NavigationBarItem(selected = false, onClick = onProgressClick, icon = { Icon(Icons.Default.PieChartOutline, null) }, label = { Text(stringResource(R.string.dashboard_progress)) })
        NavigationBarItem(selected = false, onClick = onSettingsClick, icon = { Icon(Icons.Default.Settings, null) }, label = { Text(stringResource(R.string.dashboard_settings)) })
    }
}

@Composable
private fun DashboardError(modifier: Modifier, message: String, onRetry: () -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.dashboard_tap_to_retry, message), textAlign = TextAlign.Center, modifier = Modifier.clickable(onClick = onRetry))
    }
}
