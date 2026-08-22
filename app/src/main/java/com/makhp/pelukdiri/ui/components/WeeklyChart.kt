package com.makhp.pelukdiri.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.makhp.pelukdiri.R
import com.makhp.pelukdiri.core.domain.model.DailySummary
import com.makhp.pelukdiri.ui.theme.Dimens
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
    val maxUsage = remember(history) {
        val highestUsage = max(
            history.maxOfOrNull { it.totalScreenTimeMillis } ?: 0L,
            3 * 60 * 60 * 1000L // Min 3h for scale
        )
        val hourMillis = 60 * 60 * 1000L
        ((highestUsage + hourMillis - 1) / hourMillis) * hourMillis
    }
    
    PelukCard(modifier = modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Analytics,
                contentDescription = stringResource(R.string.analytics_weekly_progress_icon_desc),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(DashboardTokens.SmallGap))
            Text(
                text = stringResource(R.string.analytics_weekly_progress_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.analytics_vs_last_week),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(DashboardTokens.LargeGap))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.spaceExtraLarge * 4 + Dimens.spaceMedium),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.height(Dimens.spaceLarge * 4 + Dimens.spaceSmall),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(formatDuration(maxUsage), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatDuration(maxUsage / 2), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatDuration(0), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(DashboardTokens.SmallGap))
            val usageByDate = remember(history) { history.associateBy { it.date } }
            Row(
                modifier = Modifier.fillMaxHeight().weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }.forEach { date ->
                    val usage = usageByDate[date]?.totalScreenTimeMillis ?: 0L
                    UsageBar(date, usage, maxUsage, date == LocalDate.now())
                }
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
    val locale = Locale.forLanguageTag(LocalConfiguration.current.locales[0].toLanguageTag())
    val ratio = if (maxUsage == 0L) 0f else (usage.toFloat() / maxUsage).coerceIn(.08f, 1f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.height(Dimens.spaceExtraLarge * 4 + Dimens.spaceMedium)
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
                    .height((Dimens.spaceLarge * 4 + Dimens.spaceSmall) * ratio)
                    .clip(RoundedCornerShape(DashboardTokens.SmallRadius))
                    .background(if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            )
        }
        Spacer(Modifier.height(DashboardTokens.SmallGap))
        Text(
            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
