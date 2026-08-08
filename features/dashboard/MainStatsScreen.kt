package com.makhp.pelukdiri.features.dashboard

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PieChartOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.makhp.pelukdiri.core.domain.model.AppUsage
import com.makhp.pelukdiri.core.domain.model.DailySummary
import com.makhp.pelukdiri.ui.components.FreedomRing
import com.makhp.pelukdiri.ui.components.InsightCard
import com.makhp.pelukdiri.ui.components.PelukCard
import com.makhp.pelukdiri.ui.components.WeeklyChart
import com.makhp.pelukdiri.ui.components.formatDuration
import com.makhp.pelukdiri.ui.theme.PELUKDIRITheme
import java.time.LocalDate
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale
import kotlin.math.max

@Composable
fun MainStatsScreen(
    onProgressClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    DashboardScaffold(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onRefresh = viewModel::forceRefresh,
        onBackfill = viewModel::backfillHistory,
        onExport = viewModel::exportDatabase,
        onGrantUsageAccess = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
        onGrantAccessibility = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
        onGrantBatteryExemption = {
            context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            })
        },
        onRetry = viewModel::loadData,
        onProgressClick = onProgressClick,
        onSettingsClick = onSettingsClick
    )
}

@Composable
private fun DashboardScaffold(
    uiState: DashboardUiState,
    snackbarHostState: SnackbarHostState,
    onRefresh: () -> Unit,
    onBackfill: () -> Unit,
    onExport: () -> Unit,
    onGrantUsageAccess: () -> Unit,
    onGrantAccessibility: () -> Unit,
    onGrantBatteryExemption: () -> Unit,
    onRetry: () -> Unit,
    onProgressClick: () -> Unit,
    onSettingsClick: () -> Unit
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
                modifier = Modifier.padding(paddingValues),
                onRefresh = onRefresh,
                onBackfill = onBackfill,
                onExport = onExport,
                onGrantUsageAccess = onGrantUsageAccess,
                onGrantAccessibility = onGrantAccessibility,
                onGrantBatteryExemption = onGrantBatteryExemption
            )
        }
    }
}

@Composable
private fun DashboardContent(
    state: DashboardUiState.Success,
    modifier: Modifier,
    onRefresh: () -> Unit,
    onBackfill: () -> Unit,
    onExport: () -> Unit,
    onGrantUsageAccess: () -> Unit,
    onGrantAccessibility: () -> Unit,
    onGrantBatteryExemption: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(DashboardTokens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(DashboardTokens.LargeGap)
    ) {
        item { DashboardHeader(onRefresh, onBackfill, onExport) }
        if (!state.isPermissionGranted) item { PermissionNotice("Usage access diperlukan agar waktu layar bisa dihitung.", "Buka izin", onGrantUsageAccess) }
        if (!state.isAccessibilityEnabled) item { PermissionNotice("Aktifkan layanan aksesibilitas untuk menjalankan intervensi.", "Aktifkan", onGrantAccessibility) }
        if (!state.isBatteryOptimizationIgnored) item { PermissionNotice("Izinkan aplikasi berjalan tanpa optimasi baterai agar pemantauan tetap aktif.", "Izinkan", onGrantBatteryExemption) }
        item { ScreenTimeCard(state.todaySummary, state.weeklySummaries) }
        item { WeeklyChart(state.weeklySummaries) }
        item {
            val appName = state.topApps.firstOrNull()?.appName ?: state.todaySummary?.mostUsedApp ?: "your phone"
            InsightCard(
                emoji = "🌿",
                title = "TODAY'S INSIGHT",
                message = "Your usage of $appName is being tracked mindfully today."
            )
        }
        item { TopAppsCard(state.topApps) }
        item { EncouragementCard() }
    }
}

@Composable
private fun DashboardHeader(onRefresh: () -> Unit, onBackfill: () -> Unit, onExport: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.Menu, "Dashboard actions") }
        Spacer(Modifier.width(DashboardTokens.SmallGap))
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PELUK DIRI", style = MaterialTheme.typography.headlineMedium)
            Text("Think. Control. Freedom.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = {}) { Icon(Icons.Default.NotificationsNone, "Notifications") }
        Box {
            IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, "More options") }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(text = { Text("Sync sekarang") }, onClick = { menuExpanded = false; onRefresh() })
                DropdownMenuItem(text = { Text("Backfill 7 hari") }, onClick = { menuExpanded = false; onBackfill() })
                DropdownMenuItem(text = { Text("Export data") }, onClick = { menuExpanded = false; onExport() })
            }
        }
    }
}

@Composable
private fun ScreenTimeCard(today: DailySummary?, history: List<DailySummary>) {
    val total = today?.totalScreenTimeMillis ?: 0L
    val adaptiveLimit = adaptiveLimitMillis(total)
    val progress = if (adaptiveLimit == 0L) 0f else (total.toFloat() / adaptiveLimit).coerceIn(0f, 1f)
    val previous = history.firstOrNull { it.date == LocalDate.now().minusDays(1) }?.totalScreenTimeMillis ?: total
    val change = if (previous == 0L) 0 else (((total - previous) * 100) / previous).toInt()

    PelukCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("SCREEN TIME TODAY", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(DashboardTokens.MediumGap))
                Text(formatDuration(total), style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(DashboardTokens.SmallGap))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowDownward, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(DashboardTokens.MediumGap))
                    Spacer(Modifier.width(DashboardTokens.SmallGap))
                    Text(changeLabel(change), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(DashboardTokens.CardPadding))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(DashboardTokens.CardPadding))
                Text("Adaptive Limit", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatDuration(adaptiveLimit), style = MaterialTheme.typography.titleLarge)
                    Icon(Icons.Default.Edit, "Edit adaptive limit", modifier = Modifier.padding(start = DashboardTokens.SmallGap).size(DashboardTokens.MediumGap), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            FreedomRing(progress, Modifier.size(DashboardTokens.RingSize))
        }
    }
}

@Composable
private fun TopAppsCard(apps: List<AppUsage>) {
    PelukCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("TOP APPS TODAY", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text("View all", style = MaterialTheme.typography.labelLarge)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(DashboardTokens.MediumGap), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(DashboardTokens.MediumGap))
        if (apps.isEmpty()) {
            Text("Belum ada data penggunaan hari ini.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else apps.take(3).forEach { app -> AppUsageRow(app, apps.maxOf { it.usageDurationMillis }) }
    }
}

@Composable
private fun AppUsageRow(app: AppUsage, maxUsage: Long) {
    val initial = app.appName.firstOrNull()?.uppercase() ?: "A"
    val progress = if (maxUsage == 0L) 0f else app.usageDurationMillis.toFloat() / maxUsage
    Row(Modifier.fillMaxWidth().padding(vertical = DashboardTokens.SmallGap), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(DashboardTokens.AppIconSize).clip(androidx.compose.foundation.shape.RoundedCornerShape(DashboardTokens.SmallRadius)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Text(initial, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(DashboardTokens.MediumGap))
        Text(app.appName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(DashboardTokens.MediumGap))
        Column(Modifier.width(112.dp)) {
            Box(Modifier.fillMaxWidth().height(DashboardTokens.SmallGap).clip(androidx.compose.foundation.shape.RoundedCornerShape(DashboardTokens.SmallRadius)).background(MaterialTheme.colorScheme.outlineVariant)) {
                Box(Modifier.fillMaxWidth(progress).height(DashboardTokens.SmallGap).background(MaterialTheme.colorScheme.primary))
            }
        }
        Spacer(Modifier.width(DashboardTokens.MediumGap))
        Text(formatDuration(app.usageDurationMillis), style = MaterialTheme.typography.labelLarge)
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.padding(start = DashboardTokens.SmallGap).size(DashboardTokens.MediumGap), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EncouragementCard() {
    PelukCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(DashboardTokens.LargeRadius)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Text("🌱", style = MaterialTheme.typography.headlineMedium) }
            Spacer(Modifier.width(DashboardTokens.CardPadding))
            Text("Small pauses, big freedom.\nYou're building a better you.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PermissionNotice(message: String, action: String, onAction: () -> Unit) {
    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(DashboardTokens.MediumRadius), color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth().clickable(onClick = onAction)) {
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
        NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
        NavigationBarItem(selected = false, onClick = onProgressClick, icon = { Icon(Icons.Default.PieChartOutline, null) }, label = { Text("Progress") })
        NavigationBarItem(selected = false, onClick = onSettingsClick, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
    }
}

@Composable
private fun DashboardError(modifier: Modifier, message: String, onRetry: () -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$message\nTap to retry", textAlign = TextAlign.Center, modifier = Modifier.clickable(onClick = onRetry))
    }
}

private fun adaptiveLimitMillis(total: Long): Long = max(total, 3 * 60 * 60 * 1000L)
private fun changeLabel(change: Int): String = if (change <= 0) "${-change}% from yesterday" else "$change% more than yesterday"

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DashboardPreview() {
    PELUKDIRITheme {
        DashboardScaffold(
            uiState = DashboardUiState.Success(
                isPermissionGranted = true,
                todaySummary = DailySummary(LocalDate.now(), 151 * 60_000L, 0, "Instagram"),
                weeklySummaries = (0..6).map { DailySummary(LocalDate.now().minusDays(it.toLong()), (60L + it * 15L) * 60_000L, 0, null) },
                topApps = listOf(AppUsage("instagram", "Instagram", 72 * 60_000L, 0), AppUsage("tiktok", "TikTok", 42 * 60_000L, 0), AppUsage("youtube", "YouTube", 37 * 60_000L, 0))
            ),
            snackbarHostState = remember { SnackbarHostState() }, onRefresh = {}, onBackfill = {}, onExport = {}, onGrantUsageAccess = {}, onGrantAccessibility = {}, onGrantBatteryExemption = {}, onRetry = {}, onProgressClick = {}, onSettingsClick = {}
        )
    }
}
