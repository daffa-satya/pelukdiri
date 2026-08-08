package com.makhp.pelukdiri.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.makhp.pelukdiri.core.domain.model.DailySummary
import com.makhp.pelukdiri.features.dashboard.DashboardTokens
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max

@Composable
fun WeeklyChart(
    history: List<DailySummary>,
    modifier: Modifier = Modifier
) {
    val maxUsage = max(
        history.maxOfOrNull { it.totalScreenTimeMillis } ?: 0L,
        3 * 60 * 60 * 1000L // Min 3h for scale
    )
    
    PelukCard(modifier = modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Analytics,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(DashboardTokens.SmallGap))
            Text(
                text = "WEEKLY PROGRESS",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "vs last week",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(DashboardTokens.LargeGap))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            val usageByDate = history.associateBy { it.date }
            (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }.forEach { date ->
                val usage = usageByDate[date]?.totalScreenTimeMillis ?: 0L
                UsageBar(date, usage, maxUsage, date == LocalDate.now())
            }
        }
    }
}

@Composable
private fun UsageBar(
    date: LocalDate,
    usage: Long,
    maxUsage: Long,
    isToday: Boolean
) {
    val ratio = if (maxUsage == 0L) 0f else (usage.toFloat() / maxUsage).coerceAtLeast(.08f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.height(148.dp)
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
            Box(
                Modifier
                    .width(DashboardTokens.BarWidth)
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
            Box(
                Modifier
                    .width(DashboardTokens.BarWidth)
                    .height((104.dp * ratio))
                    .clip(RoundedCornerShape(DashboardTokens.SmallRadius))
                    .background(if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            )
        }
        Spacer(Modifier.height(DashboardTokens.SmallGap))
        Text(
            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
