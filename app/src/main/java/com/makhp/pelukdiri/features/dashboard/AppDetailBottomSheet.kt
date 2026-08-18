package com.makhp.pelukdiri.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import com.makhp.pelukdiri.R
import com.makhp.pelukdiri.core.domain.model.AppUsage
import com.makhp.pelukdiri.ui.components.AppIcon
import com.makhp.pelukdiri.ui.components.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailBottomSheet(
    app: UiAppUsage,
    onDismiss: () -> Unit,
    onViewFullAnalytics: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            HeaderSection(onDismiss)
            
            Spacer(Modifier.height(24.dp))
            
            InsightTitleSection(app)
            
            Spacer(Modifier.height(24.dp))
            
            MetricsGrid(app)
            
            Spacer(Modifier.height(24.dp))
            
            SuggestionSection(app)
            
            Spacer(Modifier.height(32.dp))
            
            Button(
                onClick = onViewFullAnalytics,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, null, modifier = Modifier.size(20.dp), tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.app_detail_view_full_analytics), color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(20.dp), tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Lightbulb, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.dashboard_insight_title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, "Close")
        }
    }
}

@Composable
private fun InsightTitleSection(app: UiAppUsage) {
    val durationToday = app.usageDurationMillis
    val durationYesterday = app.usageDurationYesterdayMillis ?: durationToday
    val diffPercent = if (durationYesterday > 0) {
        ((durationYesterday - durationToday) * 100 / durationYesterday).toInt()
    } else 0
    
    val isPositive = diffPercent >= 0

    val titles = if (isPositive) {
        listOf(
            R.string.app_detail_great_job,
            R.string.app_detail_great_job_2,
            R.string.app_detail_great_job_3,
            R.string.app_detail_great_job_4
        )
    } else {
        listOf(
            R.string.app_detail_watch_out,
            R.string.app_detail_watch_out_2,
            R.string.app_detail_watch_out_3,
            R.string.app_detail_watch_out_4
        )
    }

    val selectedTitle = remember(app.packageName, isPositive) {
        titles.random()
    }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(selectedTitle),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isPositive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(4.dp))
            
            val text = if (isPositive) {
                stringResource(R.string.app_detail_spent_less, diffPercent, app.appName)
            } else {
                stringResource(R.string.app_detail_spent_more, -diffPercent, app.appName)
            }
            
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Box(contentAlignment = Alignment.BottomEnd) {
            AppIcon(app.packageName, app.appName, size = 64.dp)
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPositive) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun MetricsGrid(app: UiAppUsage) {
    val durationToday = app.usageDurationMillis
    val durationYesterday = app.usageDurationYesterdayMillis ?: 0L
    val isDurationPositive = durationToday <= durationYesterday

    val openingsToday = app.openingsToday ?: 0
    val openingsYesterday = app.openingsYesterday ?: 0
    val isOpeningsPositive = openingsToday <= openingsYesterday

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.app_detail_time_spent),
                icon = Icons.Default.AccessTime,
                value = formatDuration(durationToday),
                subValue = stringResource(R.string.app_detail_yesterday, formatDuration(durationYesterday)),
                change = if (durationYesterday > 0 && durationToday != durationYesterday) {
                    val diff = durationYesterday - durationToday
                    if (diff > 0) "- ${formatDuration(diff)}" else "+ ${formatDuration(-diff)}"
                } else null,
                isPositive = isDurationPositive
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.app_detail_openings),
                icon = Icons.AutoMirrored.Filled.OpenInNew,
                value = stringResource(R.string.app_detail_times_count, openingsToday),
                subValue = stringResource(R.string.app_detail_yesterday, stringResource(R.string.app_detail_times_count, openingsYesterday)),
                change = if (openingsYesterday > 0 && openingsToday != openingsYesterday) {
                    val diff = openingsYesterday - openingsToday
                    if (diff > 0) "↓ $diff" else "↑ ${-diff}"
                } else null,
                isPositive = isOpeningsPositive
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.app_detail_peak_time),
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                value = app.peakTimeToday ?: "--:--",
                subValue = stringResource(R.string.app_detail_yesterday, app.peakTimeYesterday ?: "--:--"),
                change = null, // Can add logic for earlier/later if peak time is parsed
                isPositive = true
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.app_detail_interventions),
                icon = Icons.Default.DoneAll,
                value = "${app.interventionsToday ?: 0} kali",
                subValue = stringResource(R.string.app_detail_completed_today),
                change = if ((app.interventionsToday ?: 0) > 0) "Good job!" else null,
                isPositive = true
            )
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    value: String,
    subValue: String,
    change: String?,
    isPositive: Boolean
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subValue, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            
            if (change != null) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isPositive) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        null,
                        modifier = Modifier.size(12.dp),
                        tint = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        change,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionSection(app: UiAppUsage) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Lightbulb, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.app_detail_suggestion), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.app_detail_suggestion_message, app.peakTimeToday ?: "19:00 - 21:00"),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
