package com.makhp.pelukdiri.features.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.makhp.pelukdiri.R
import com.makhp.pelukdiri.core.domain.model.AppUsage
import com.makhp.pelukdiri.features.dashboard.AppDetailBottomSheet
import com.makhp.pelukdiri.features.dashboard.DashboardTokens
import com.makhp.pelukdiri.features.dashboard.UiAppUsage
import com.makhp.pelukdiri.ui.components.AppIcon
import com.makhp.pelukdiri.ui.components.PelukDiriLogo
import com.makhp.pelukdiri.ui.components.InsightCard
import com.makhp.pelukdiri.ui.components.PelukCard
import com.makhp.pelukdiri.ui.components.formatDuration
import com.makhp.pelukdiri.ui.theme.Dimens
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

@Composable
fun AnalyticsScreen(
    onHomeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onViewAllClick: (LocalDate, AnalyticsPeriod) -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AnalyticsLayout(
        state = state,
        onHomeClick = onHomeClick,
        onSettingsClick = onSettingsClick,
        onViewAllClick = onViewAllClick,
        onDateSelected = { 
            val currentPeriod = (state as? AnalyticsUiState.Success)?.selectedPeriod ?: AnalyticsPeriod.DAILY
            viewModel.load(it, currentPeriod) 
        },
        onPeriodSelected = { 
            val currentDate = (state as? AnalyticsUiState.Success)?.selectedDate ?: LocalDate.now()
            viewModel.load(currentDate, it)
        },
        onUpdateAppUsage = viewModel::updateAppUsage,
        onCalculateGraph = viewModel::calculateGraph,
    )
}

@Composable
private fun AnalyticsLayout(
    state: AnalyticsUiState,
    onHomeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onViewAllClick: (LocalDate, AnalyticsPeriod) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onPeriodSelected: (AnalyticsPeriod) -> Unit,
    onUpdateAppUsage: (String, Long) -> Unit,
    onCalculateGraph: () -> Unit,
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
            is AnalyticsUiState.Success -> AnalyticsContent(
                state = state, 
                modifier = Modifier.padding(padding), 
                onViewAllClick = onViewAllClick,
                onDateSelected = onDateSelected, 
                onPeriodSelected = onPeriodSelected,
                onUpdateAppUsage = onUpdateAppUsage,
                onCalculateGraph = onCalculateGraph,
            )
        }
    }
}

@Composable
private fun AnalyticsContent(
    state: AnalyticsUiState.Success,
    modifier: Modifier,
    onViewAllClick: (LocalDate, AnalyticsPeriod) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onPeriodSelected: (AnalyticsPeriod) -> Unit,
    onUpdateAppUsage: (String, Long) -> Unit,
    onCalculateGraph: () -> Unit,
) {
    var selectedApp by remember { mutableStateOf<UiAppUsage?>(null) }
    var appBeingEdited by remember { mutableStateOf<UiAppUsage?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(DashboardTokens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(DashboardTokens.LargeGap)
    ) {
        item(key = "header") { AnalyticsHeader() }
        item(key = "period") { PeriodSelector(state.selectedPeriod, onPeriodSelected) }
        item(key = "date") { DateSelector(state.selectedDate, state.selectedPeriod, onDateSelected) }
        
        if (state.selectedPeriod == AnalyticsPeriod.DAILY) {
            item(key = "daily_summary_card") {
                DailyInsightCard(state)
            }
        }

        item(key = "metrics") { MetricGrid(state) }
        item(key = "chart") {
            ScreenTimeChart(
                hourlyUsage = state.hourlyUsage,
                isCalculated = state.isGraphCalculated,
                isCalculating = state.isCalculatingGraph,
                error = state.graphError,
                onCalculate = onCalculateGraph,
            )
        }
        item(key = "top_apps") { 
            AnalyticsAppsCard(
                apps = state.topApps,
                onViewAllClick = {
                    onViewAllClick(state.selectedDate, state.selectedPeriod)
                },
                onAppClick = { selectedApp = it },
                onEditApp = if (state.selectedPeriod == AnalyticsPeriod.DAILY) {
                    { appBeingEdited = it }
                } else null,
            ) 
        }

    }

    selectedApp?.let { app ->
        AppDetailBottomSheet(
            app = app,
            onDismiss = { selectedApp = null },
            onViewFullAnalytics = { selectedApp = null }
        )
    }

    appBeingEdited?.let { app ->
        EditAppUsageDialog(
            app = app,
            onDismiss = { appBeingEdited = null },
            onSave = { durationMillis ->
                onUpdateAppUsage(app.packageName, durationMillis)
                appBeingEdited = null
            },
        )
    }
}

@Composable
private fun AnalyticsHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.spaceSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(Dimens.minTouchTarget))
        Text(
            text = stringResource(R.string.analytics_title),
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
private fun PeriodSelector(
    selectedPeriod: AnalyticsPeriod,
    onPeriodSelected: (AnalyticsPeriod) -> Unit
) {
    val periods = listOf(
        AnalyticsPeriod.DAILY to stringResource(R.string.analytics_period_daily),
        AnalyticsPeriod.WEEKLY to stringResource(R.string.analytics_period_weekly),
        AnalyticsPeriod.MONTHLY to stringResource(R.string.analytics_period_monthly)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DashboardTokens.LargeRadius))
            .background(MaterialTheme.colorScheme.surface),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        periods.forEach { (period, label) ->
            val isSelected = selectedPeriod == period
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(DashboardTokens.LargeRadius))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                    .clickable { onPeriodSelected(period) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelector(
    date: LocalDate, 
    period: AnalyticsPeriod,
    onDateSelected: (LocalDate) -> Unit
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    // Start Date Picker
    if (showStartPicker) {
        val initialStart = when(period) {
            AnalyticsPeriod.DAILY -> date
            AnalyticsPeriod.WEEKLY -> date.minusDays(6)
            AnalyticsPeriod.MONTHLY -> date.withDayOfMonth(1)
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialStart.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= System.currentTimeMillis()
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val selectedStart = java.time.Instant.ofEpochMilli(it)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        val newEndDate = when(period) {
                            AnalyticsPeriod.DAILY -> selectedStart
                            AnalyticsPeriod.WEEKLY -> selectedStart.plusDays(6)
                            AnalyticsPeriod.MONTHLY -> selectedStart.plusMonths(1).minusDays(1)
                        }
                        // Cap at today
                        val cappedDate = if (newEndDate.isAfter(LocalDate.now())) LocalDate.now() else newEndDate
                        onDateSelected(cappedDate)
                    }
                    showStartPicker = false
                }) { Text(stringResource(R.string.profile_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text(stringResource(R.string.profile_cancel)) }
            }
        ) { DatePicker(state = datePickerState) }
    }

    // End Date Picker
    if (showEndPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= System.currentTimeMillis()
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val selectedEnd = java.time.Instant.ofEpochMilli(it)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        onDateSelected(selectedEnd)
                    }
                    showEndPicker = false
                }) { Text(stringResource(R.string.profile_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text(stringResource(R.string.profile_cancel)) }
            }
        ) { DatePicker(state = datePickerState) }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { 
            val newDate = when(period) {
                AnalyticsPeriod.DAILY -> date.minusDays(1)
                AnalyticsPeriod.WEEKLY -> date.minusWeeks(1)
                AnalyticsPeriod.MONTHLY -> date.minusMonths(1)
            }
            onDateSelected(newDate) 
        }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.analytics_prev))
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            
            val localeID = remember { Locale.forLanguageTag("id-ID") }
            if (period == AnalyticsPeriod.DAILY) {
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", localeID)),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.clickable { showEndPicker = true }
                )
            } else {
                val start = when(period) {
                    AnalyticsPeriod.WEEKLY -> date.minusDays(6)
                    AnalyticsPeriod.MONTHLY -> date.withDayOfMonth(1)
                    else -> date
                }
                val end = when(period) {
                    AnalyticsPeriod.MONTHLY -> date.withDayOfMonth(date.lengthOfMonth())
                    else -> date
                }
                val formatter = DateTimeFormatter.ofPattern("d MMM", localeID)
                val yearFormatter = DateTimeFormatter.ofPattern("yyyy", localeID)
                
                Surface(
                    onClick = { showStartPicker = true },
                    color = Color.Transparent,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = start.format(formatter),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                Text(" - ", style = MaterialTheme.typography.titleMedium)
                Surface(
                    onClick = { showEndPicker = true },
                    color = Color.Transparent,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    val endText = if (period == AnalyticsPeriod.MONTHLY) {
                        end.format(DateTimeFormatter.ofPattern("MMMM yyyy", localeID))
                    } else {
                        "${end.format(formatter)} ${end.format(yearFormatter)}"
                    }
                    Text(
                        text = endText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }

        val canGoForward = remember(date, period) {
            val today = LocalDate.now()
            when (period) {
                AnalyticsPeriod.DAILY -> date.isBefore(today)
                AnalyticsPeriod.WEEKLY -> date.isBefore(today)
                AnalyticsPeriod.MONTHLY -> {
                    date.year < today.year || (date.year == today.year && date.monthValue < today.monthValue)
                }
            }
        }

        IconButton(
            onClick = {
                val newDate = when(period) {
                    AnalyticsPeriod.DAILY -> date.plusDays(1)
                    AnalyticsPeriod.WEEKLY -> date.plusWeeks(1)
                    AnalyticsPeriod.MONTHLY -> date.plusMonths(1)
                }
                val cappedDate = if (newDate.isAfter(LocalDate.now())) LocalDate.now() else newDate
                onDateSelected(cappedDate)
            },
            enabled = canGoForward
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.analytics_next),
                tint = if (canGoForward) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        }
    }
}


@Composable
private fun DailyInsightCard(state: AnalyticsUiState.Success) {
    val usedMinutes = (state.summary?.totalScreenTimeMillis ?: 0L) / 60_000L
    val limitMinutes = state.adaptiveLimitMinutes?.toLong()
    
    if (limitMinutes == null) {
        PelukCard(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.analytics_insufficient_data),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val isPositive = usedMinutes <= limitMinutes
    val diffMinutes = if (isPositive) limitMinutes - usedMinutes else usedMinutes - limitMinutes
    val formattedDiff = formatDuration(diffMinutes * 60_000L)

    val titles = if (isPositive) {
        listOf(
            R.string.analytics_insight_positive_1,
            R.string.analytics_insight_positive_2,
            R.string.analytics_insight_positive_3,
            R.string.analytics_insight_positive_4
        )
    } else {
        listOf(
            R.string.analytics_insight_negative_1,
            R.string.analytics_insight_negative_2,
            R.string.analytics_insight_negative_3,
            R.string.analytics_insight_negative_4
        )
    }

    val selectedTitle = remember(state.selectedDate, isPositive) {
        titles.random()
    }

    PelukCard(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(selectedTitle),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    if (isPositive) R.string.analytics_insight_sub_positive 
                    else R.string.analytics_insight_sub_negative,
                    formattedDiff
                ),
                style = MaterialTheme.typography.labelSmall,
                color = if (isPositive) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun MetricGrid(state: AnalyticsUiState.Success) {
    val isDaily = state.selectedPeriod == AnalyticsPeriod.DAILY
    val comparisonPeriod = when (state.selectedPeriod) {
        AnalyticsPeriod.DAILY -> stringResource(R.string.analytics_comparison_yesterday)
        AnalyticsPeriod.WEEKLY -> stringResource(R.string.analytics_comparison_previous_week)
        AnalyticsPeriod.MONTHLY -> stringResource(R.string.analytics_comparison_previous_month)
    }
    @Composable
    fun comparison(current: Long, previous: Long): Pair<String, Boolean> {
        val change = percentageChange(current, previous)
        val value = when {
            change != null && change > 0 -> "↗ ${change}%"
            change != null && change < 0 -> "↘ ${-change}%"
            change != null -> "0%"
            current > 0 -> stringResource(R.string.analytics_comparison_new)
            else -> "0%"
        }
        return stringResource(R.string.analytics_vs_period, value, comparisonPeriod) to
            (change?.let { it > 0 } ?: (current > 0))
    }
    val totalScreenTime = state.summary?.totalScreenTimeMillis ?: 0L
    val comparisonTotalScreenTime = state.comparisonSummary?.totalScreenTimeMillis ?: 0L
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DashboardTokens.SmallGap)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DashboardTokens.SmallGap)
        ) {
            MetricCard(
                label = stringResource(R.string.analytics_metric_social_media_screentime),
                value = formatDuration(state.socialMediaUsageMillis),
                comparison = comparison(state.socialMediaUsageMillis, state.comparisonSocialMediaUsageMillis),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = if (isDaily) stringResource(R.string.analytics_metric_total_screentime) 
                        else stringResource(R.string.analytics_avg_screentime),
                value = formatDuration(totalScreenTime),
                comparison = comparison(totalScreenTime, comparisonTotalScreenTime),
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DashboardTokens.SmallGap)
        ) {
            MetricCard(
                label = stringResource(R.string.analytics_metric_longest_session),
                value = formatDuration(state.longestSessionMillis),
                comparison = comparison(state.longestSessionMillis, state.comparisonLongestSessionMillis),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = stringResource(R.string.analytics_metric_interventions),
                value = "${state.interventionCount}",
                comparison = comparison(
                    state.interventionCount.toLong(),
                    state.comparisonInterventionCount.toLong()
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    comparison: Pair<String, Boolean>,
    modifier: Modifier,
) {
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
            text = comparison.first,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
            color = if (comparison.second) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ScreenTimeChart(
    hourlyUsage: List<Long>,
    isCalculated: Boolean,
    isCalculating: Boolean,
    error: String?,
    onCalculate: () -> Unit,
) {
    val axisMinutes = listOf(60, 40, 20, 0)

    PelukCard(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.analytics_screen_time_over_time), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(DashboardTokens.CardPadding))

        if (!isCalculated) {
            Button(
                onClick = onCalculate,
                enabled = !isCalculating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isCalculating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(DashboardTokens.MediumGap),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(DashboardTokens.SmallGap))
                }
                Text(
                    stringResource(
                        if (isCalculating) R.string.analytics_graph_calculating
                        else R.string.analytics_graph_calculate
                    )
                )
            }
            error?.let {
                Spacer(Modifier.height(DashboardTokens.SmallGap))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            return@PelukCard
        }
        
        Row(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            // Y-Axis Labels
            Column(
                modifier = Modifier.fillMaxHeight().width(40.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                axisMinutes.forEach { minutes ->
                    Text(
                        stringResource(R.string.analytics_minutes_short, minutes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp
                    )
                }
            }
            
            Spacer(Modifier.width(8.dp))
            
            // Chart
            UsageLineChart(hourlyUsage, 60 * 60 * 1000L, Modifier.weight(1f).fillMaxHeight())
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Dimens.spaceSmall).padding(start = 48.dp),
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
        Text(
            text = stringResource(R.string.analytics_x_axis_explanation),
            modifier = Modifier.fillMaxWidth().padding(top = Dimens.spaceExtraSmall),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun UsageLineChart(values: List<Long>, maxValue: Long, modifier: Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val dividerStroke = Dimens.dividerThickness
    val pathStroke = Dimens.spaceExtraSmall.minus(Dimens.dividerThickness)

    Canvas(modifier) {
        val scaleMax = max(maxValue, 1L).toFloat()
        repeat(4) { index ->
            val y = size.height * index / 3f
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = dividerStroke.toPx()
            )
        }
        
        if (values.isNotEmpty()) {
            val path = Path()
            val stepX = size.width / (values.size - 1).coerceAtLeast(1)
            values.forEachIndexed { index, value ->
                val x = stepX * index
                val y = size.height - (value.coerceAtMost(maxValue) / scaleMax) * size.height
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = pathStroke.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun AnalyticsAppsCard(
    apps: List<UiAppUsage>,
    onViewAllClick: () -> Unit,
    onAppClick: (UiAppUsage) -> Unit,
    onEditApp: ((UiAppUsage) -> Unit)?,
) {
    PelukCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onViewAllClick), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.analytics_top_apps),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.dashboard_view_all),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp).padding(start = 4.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(DashboardTokens.MediumGap))
        if (apps.isEmpty()) {
            Text(stringResource(R.string.analytics_no_usage_data), style = MaterialTheme.typography.bodySmall)
        } else {
            apps.take(5).forEach { app ->
                AppUsageItem(
                    app = app,
                    onClick = { onAppClick(app) },
                    onEdit = onEditApp?.let { edit -> { edit(app) } },
                )
            }
        }
    }
}

@Composable
private fun AppUsageItem(app: UiAppUsage, onClick: () -> Unit, onEdit: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = DashboardTokens.SmallGap).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(app.packageName, app.appName, size = 32.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = app.appName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = formatDuration(app.usageDurationMillis),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        if (onEdit != null) {
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(R.string.analytics_edit_usage),
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            null,
            modifier = Modifier.size(16.dp).padding(start = 4.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EditAppUsageDialog(
    app: UiAppUsage,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
) {
    val totalMinutes = app.usageDurationMillis / 60_000L
    var hours by remember(app.packageName) { mutableStateOf((totalMinutes / 60).toString()) }
    var minutes by remember(app.packageName) { mutableStateOf((totalMinutes % 60).toString()) }
    val parsedHours = hours.toIntOrNull()
    val parsedMinutes = minutes.toIntOrNull()
    val isValid = parsedHours != null && parsedHours in 0..24 &&
        parsedMinutes != null && parsedMinutes in 0..59 &&
        (parsedHours < 24 || parsedMinutes == 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.analytics_edit_usage_title, app.appName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceMedium)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMedium)) {
                    OutlinedTextField(
                        value = hours,
                        onValueChange = { value -> hours = value.filter(Char::isDigit).take(2) },
                        label = { Text(stringResource(R.string.analytics_edit_usage_hours)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = minutes,
                        onValueChange = { value -> minutes = value.filter(Char::isDigit).take(2) },
                        label = { Text(stringResource(R.string.analytics_edit_usage_minutes)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    text = stringResource(R.string.analytics_edit_usage_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    onSave(((parsedHours!! * 60L) + parsedMinutes!!) * 60_000L)
                },
            ) { Text(stringResource(R.string.analytics_edit_usage_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.analytics_edit_usage_cancel))
            }
        },
    )
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
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.dashboard_home)) }
        )
        NavigationBarItem(
            selected = true,
            onClick = {},
            icon = { Icon(Icons.Default.PieChart, contentDescription = null) },
            label = { Text(stringResource(R.string.dashboard_progress)) }
        )
        NavigationBarItem(
            selected = false,
            onClick = onSettingsClick,
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.dashboard_settings)) }
        )
    }
}
