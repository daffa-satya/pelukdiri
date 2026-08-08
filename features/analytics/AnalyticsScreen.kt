package com.makhp.pelukdiri.features.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.makhp.pelukdiri.core.domain.model.AppUsage
import com.makhp.pelukdiri.core.domain.model.DailySummary
import com.makhp.pelukdiri.features.dashboard.DashboardTokens
import com.makhp.pelukdiri.ui.components.FreedomRing
import com.makhp.pelukdiri.ui.components.InsightCard
import com.makhp.pelukdiri.ui.components.PelukCard
import com.makhp.pelukdiri.ui.components.formatDuration
import com.makhp.pelukdiri.ui.theme.PELUKDIRITheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

@Composable
fun AnalyticsScreen(
    onHomeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AnalyticsLayout(state, onHomeClick, onSettingsClick, viewModel::load)
}

@Composable
private fun AnalyticsLayout(
    state: AnalyticsUiState,
    onHomeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { AnalyticsNavigation(onHomeClick, onSettingsClick) }
    ) { padding ->
        when (state) {
            AnalyticsUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            is AnalyticsUiState.Error -> Box(
                Modifier.fillMaxSize().padding(padding).clickable { onDateSelected(LocalDate.now()) },
                contentAlignment = Alignment.Center
            ) { Text(state.message) }
            is AnalyticsUiState.Success -> AnalyticsContent(state, Modifier.padding(padding), onHomeClick, onDateSelected)
        }
    }
}

@Composable
private fun AnalyticsContent(
    state: AnalyticsUiState.Success,
    modifier: Modifier,
    onHomeClick: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(DashboardTokens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(DashboardTokens.LargeGap)
    ) {
        item { AnalyticsHeader(onHomeClick) }
        item { PeriodSelector() }
        item { DateSelector(state.selectedDate, onDateSelected) }
        item { MetricGrid(state) }
        item { ScreenTimeChart(state.hourlyUsage) }
        item { AnalyticsAppsCard(state.topApps) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(DashboardTokens.MediumGap)) {
                CategoryCard(Modifier.weight(1f))
                LimitCard(state.summary, Modifier.weight(1f))
            }
        }
        item {
            InsightCard(
                emoji = "🌿",
                title = "INSIGHT",
                message = "Your screen time is ${formatDuration(state.summary?.totalScreenTimeMillis ?: 0)} today. Keep building healthy habits."
            )
        }
    }
}

@Composable
private fun AnalyticsHeader(onHomeClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onHomeClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(
            text = "Analytics",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium
        )
        IconButton(onClick = {}) {
            Icon(Icons.Default.CalendarToday, contentDescription = "Select date")
        }
    }
}

@Composable
private fun PeriodSelector() {
    val periods = listOf("Daily", "Weekly", "Monthly", "Yearly")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DashboardTokens.LargeRadius))
            .background(MaterialTheme.colorScheme.surface),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        periods.forEachIndexed { index, label ->
            val isSelected = index == 0
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(DashboardTokens.LargeRadius))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                    .clickable { /* Handle period selection */ }
                    .padding(vertical = DashboardTokens.MediumGap),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    maxLines = 1,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun DateSelector(date: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onDateSelected(date.minusDays(1)) }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous day")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(DashboardTokens.SmallGap))
            Text(
                text = date.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())),
                style = MaterialTheme.typography.titleLarge
            )
        }
        IconButton(onClick = { onDateSelected(date.plusDays(1)) }) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next day")
        }
    }
}

@Composable
private fun MetricGrid(state: AnalyticsUiState.Success) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DashboardTokens.SmallGap)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DashboardTokens.SmallGap)
        ) {
            MetricCard(
                label = "Total Screen Time",
                value = formatDuration(state.summary?.totalScreenTimeMillis ?: 0),
                change = "↘ 23% vs yesterday",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "Unlocks",
                value = "${state.summary?.unlockCount ?: 0}",
                change = "↘ 13% vs yesterday",
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DashboardTokens.SmallGap)
        ) {
            MetricCard(
                label = "Longest Session",
                value = formatDuration(state.topApps.firstOrNull()?.usageDurationMillis ?: 0),
                change = "↘ 8% vs yesterday",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "Interventions",
                value = "${state.interventionCount}",
                change = "Good job!",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, change: String, modifier: Modifier) {
    PelukCard(modifier = modifier.height(DashboardTokens.MetricCardHeight)) {
        Text(
            text = label,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(DashboardTokens.MediumGap))
        Text(
            text = value,
            maxLines = 1,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = change,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ScreenTimeChart(hourlyUsage: List<Long>) {
    PelukCard(Modifier.fillMaxWidth()) {
        Text("SCREEN TIME OVER TIME", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(DashboardTokens.CardPadding))
        UsageLineChart(hourlyUsage, Modifier.fillMaxWidth().height(180.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("00", "04", "08", "12", "16", "20", "24").forEach {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UsageLineChart(values: List<Long>, modifier: Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        val maxValue = max(values.maxOrNull() ?: 0L, 1L).toFloat()
        // Draw grid lines
        repeat(4) { index ->
            val y = size.height * index / 3f
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }
        
        // Draw path
        if (values.isNotEmpty()) {
            val path = Path()
            val stepX = size.width / (values.size - 1).coerceAtLeast(1)
            values.forEachIndexed { index, value ->
                val x = stepX * index
                val y = size.height - (value / maxValue) * size.height
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun AnalyticsAppsCard(apps: List<AppUsage>) {
    PelukCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "TOP APPS",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "View all",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(DashboardTokens.MediumGap))
        if (apps.isEmpty()) {
            Text("No usage data yet.", style = MaterialTheme.typography.bodySmall)
        } else {
            val maxDuration = apps.maxOfOrNull { it.usageDurationMillis } ?: 1L
            apps.take(5).forEach { app ->
                AppUsageItem(app, maxDuration)
            }
        }
    }
}

@Composable
private fun AppUsageItem(app: AppUsage, maxDuration: Long) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = DashboardTokens.SmallGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = app.appName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .weight(2f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(app.usageDurationMillis.toFloat() / maxDuration)
                    .height(8.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Spacer(Modifier.width(DashboardTokens.MediumGap))
        Text(
            text = formatDuration(app.usageDurationMillis),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun CategoryCard(modifier: Modifier) {
    PelukCard(modifier = modifier) {
        Text("BY CATEGORY", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(DashboardTokens.LargeGap))
        Box(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            FreedomRing(
                progress = 0.7f, // Mock category progress
                modifier = Modifier.size(110.dp)
            )
        }
        Spacer(Modifier.height(DashboardTokens.SmallGap))
        Text(
            text = "Most used: Productivity",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LimitCard(summary: DailySummary?, modifier: Modifier) {
    val used = summary?.totalScreenTimeMillis ?: 0
    val limit = max(used, 3 * 60 * 60 * 1000L)
    val progress = used.toFloat() / limit
    
    PelukCard(modifier = modifier) {
        Text("VS LIMIT", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(DashboardTokens.LargeGap))
        Box(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            FreedomRing(
                progress = progress,
                modifier = Modifier.size(110.dp)
            )
        }
        Spacer(Modifier.height(DashboardTokens.SmallGap))
        Text(
            text = "Used ${formatDuration(used)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AnalyticsNavigation(
    onHomeClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NavigationBarItem(
            selected = false,
            onClick = onHomeClick,
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = true,
            onClick = {},
            icon = { Icon(Icons.Default.PieChart, null) },
            label = { Text("Progress") }
        )
        NavigationBarItem(
            selected = false,
            onClick = onSettingsClick,
            icon = { Icon(Icons.Default.Settings, null) },
            label = { Text("Settings") }
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AnalyticsPreview() {
    PELUKDIRITheme {
        AnalyticsLayout(
            state = AnalyticsUiState.Success(
                selectedDate = LocalDate.now(),
                summary = DailySummary(LocalDate.now(), 151L * 60_000L, 29, "Instagram"),
                topApps = listOf(
                    AppUsage("instagram", "Instagram", 72L * 60_000L, 0),
                    AppUsage("tiktok", "TikTok", 42L * 60_000L, 0)
                ),
                hourlyUsage = List(24) { (if (it in 8..19) (it * 60_000L) else 0L) },
                interventionCount = 8
            ),
            onHomeClick = {},
            onSettingsClick = {},
            onDateSelected = {}
        )
    }
}
