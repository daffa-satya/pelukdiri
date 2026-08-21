package com.makhp.pelukdiri.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.makhp.pelukdiri.ui.theme.Dimens
import com.makhp.pelukdiri.features.dashboard.DashboardTokens
import com.makhp.pelukdiri.ui.components.PelukDiriLogo

@Composable
fun InsightCard(
    emoji: String,
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    PelukCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(Dimens.spaceExtraLarge * 2)
                    .clip(RoundedCornerShape(DashboardTokens.LargeRadius))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (emoji == "🌿" || emoji == "🌱") {
                    PelukDiriLogo(size = 40.dp)
                } else {
                    Text(text = emoji, style = MaterialTheme.typography.headlineMedium)
                }
            }
            Spacer(modifier = Modifier.width(DashboardTokens.CardPadding))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(DashboardTokens.SmallGap))
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}
