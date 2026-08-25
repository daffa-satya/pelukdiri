package com.makhp.pelukdiri.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makhp.pelukdiri.R
import com.makhp.pelukdiri.core.domain.model.AggressivenessLevel
import com.makhp.pelukdiri.features.dashboard.DashboardTokens
import com.makhp.pelukdiri.ui.components.PelukDiriLogo
import com.makhp.pelukdiri.ui.components.PelukCard
import com.makhp.pelukdiri.ui.theme.Dimens

@Composable
fun AdaptiveModeScreen(
    onBackClick: () -> Unit,
    state: AdaptiveModeUiState = AdaptiveModeUiState(),
    onAggressivenessChange: (AggressivenessLevel) -> Unit = {}
) {
    var activeDialog by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AdaptiveModeHeader(onBackClick)
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(DashboardTokens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardTokens.LargeGap)
        ) {
            item(key = "header_info") {
                AdaptiveModeInfoCard()
            }

            item(key = "aggressiveness") {
                AggressivenessSection(
                    currentLevel = state.aggressivenessLevel,
                    onLevelChange = onAggressivenessChange
                )
            }

            item(key = "prediction") {
                PredictionSection(
                    state = state,
                    onViewDetail = { activeDialog = "prediction_detail" }
                )
            }
        }
    }

    when (activeDialog) {
        "prediction_detail" -> DetailDialog(
            title = stringResource(R.string.adaptive_mode_prediction_detail_title),
            content = stringResource(R.string.adaptive_mode_prediction_detail_msg),
            onDismiss = { activeDialog = null }
        )
    }
}

@Composable
private fun DetailDialog(title: String, content: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(content, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.adaptive_mode_understand)) }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun AdaptiveModeInfoCard() {
    PelukCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(Dimens.buttonHeight)
                    .clip(RoundedCornerShape(DashboardTokens.LargeRadius))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                PelukDiriLogo(size = 40.dp)
            }
            Spacer(Modifier.width(DashboardTokens.CardPadding))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.adaptive_mode_active),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.adaptive_mode_active_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AggressivenessSection(
    currentLevel: AggressivenessLevel,
    onLevelChange: (AggressivenessLevel) -> Unit
) {
    PelukCard {
        Column {
            Text(
                text = stringResource(R.string.adaptive_mode_intensity_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.adaptive_mode_intensity_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AggressivenessLevel.entries.forEach { level ->
                    val isSelected = currentLevel == level
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable { onLevelChange(level) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                            val label = when(level) {
                                AggressivenessLevel.CONSERVATIVE -> stringResource(R.string.adaptive_mode_level_conservative)
                                AggressivenessLevel.BALANCED -> stringResource(R.string.adaptive_mode_level_balanced)
                                AggressivenessLevel.AGGRESSIVE -> stringResource(R.string.adaptive_mode_level_aggressive)
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PredictionSection(
    state: AdaptiveModeUiState,
    onViewDetail: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.adaptive_mode_prediction_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(Dimens.spaceExtraSmall))
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.spaceMedium),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = stringResource(R.string.adaptive_mode_view_detail),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onViewDetail)
            )
        }
        
        Spacer(Modifier.height(DashboardTokens.MediumGap))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSmall)
        ) {
            PredictionCard(
                label = stringResource(R.string.adaptive_mode_predicted_limit),
                value = state.predictedLimit,
                subValue = stringResource(R.string.adaptive_mode_prediction_title),
                icon = Icons.Default.AccessTime,
                modifier = Modifier.weight(1f)
            )
            PredictionCard(
                label = stringResource(R.string.adaptive_mode_predicted_intensity),
                value = state.interventionIntensity,
                subValue = stringResource(R.string.app_detail_completed_today), // Reusing similar string
                icon = Icons.Default.Psychology,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(Modifier.height(Dimens.spaceSmall))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSmall)
        ) {
            PredictionCard(
                label = stringResource(R.string.adaptive_mode_difficulty),
                value = state.difficultyLevel,
                subValue = stringResource(R.string.app_detail_interventions),
                icon = Icons.Default.Extension,
                modifier = Modifier.weight(1f)
            )
            PredictionCard(
                label = stringResource(R.string.adaptive_mode_monitored_apps),
                value = state.monitoredAppsCount.toString(),
                subValue = stringResource(R.string.adaptive_mode_monitored_apps_desc_format, state.totalAppsCount),
                icon = Icons.Default.Apps,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PredictionCard(
    label: String,
    value: String,
    subValue: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    PelukCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = subValue,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AdaptiveModeHeader(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.spaceExtraSmall, vertical = Dimens.spaceSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }
        Text(
            text = stringResource(R.string.adaptive_mode_title),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium
        )
        Box(Modifier.size(Dimens.minTouchTarget), contentAlignment = Alignment.Center) {
            PelukDiriLogo(size = 28.dp)
        }
    }
}
