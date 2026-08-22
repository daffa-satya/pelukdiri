package com.makhp.pelukdiri.features.settings

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import com.makhp.pelukdiri.R
import com.makhp.pelukdiri.features.dashboard.DashboardTokens
import com.makhp.pelukdiri.ui.components.PelukCard
import com.makhp.pelukdiri.ui.theme.Dimens
import com.makhp.pelukdiri.ui.theme.PELUKDIRITheme

@Composable
fun PrivacyScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PrivacyHeader(onBackClick)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(DashboardTokens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardTokens.LargeGap)
        ) {
            item {
                Text(
                    text = stringResource(R.string.privacy_commitment),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            item {
                PrivacyNoticeBanner()
            }
            
            item {
                SettingsSection(title = stringResource(R.string.privacy_collected_section)) {
                    SettingsItem(
                        title = stringResource(R.string.privacy_item_screen_time_title),
                        description = stringResource(R.string.privacy_item_screen_time_desc),
                        icon = Icons.Default.AccessTime,
                        onClick = {}
                    )
                    SettingsDivider()
                    SettingsItem(
                        title = stringResource(R.string.privacy_item_apps_opened_title),
                        description = stringResource(R.string.privacy_item_apps_opened_desc),
                        icon = Icons.Default.Apps,
                        onClick = {}
                    )
                    SettingsDivider()
                    SettingsItem(
                        title = stringResource(R.string.privacy_item_intervention_title),
                        description = stringResource(R.string.privacy_item_intervention_desc),
                        icon = Icons.Default.Psychology,
                        onClick = {}
                    )
                    SettingsDivider()
                    SettingsItem(
                        title = stringResource(R.string.privacy_item_device_data_title),
                        description = stringResource(R.string.privacy_item_device_data_desc),
                        icon = Icons.Default.Devices,
                        onClick = {}
                    )
                }
            }
            
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(DashboardTokens.MediumRadius))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(DashboardTokens.CardPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Shield, contentDescription = stringResource(R.string.privacy_title), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(Dimens.iconSizeMedium))
                    Spacer(Modifier.width(DashboardTokens.MediumGap))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.privacy_not_collected_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.privacy_not_collected_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            item {
                SettingsSection(title = stringResource(R.string.privacy_ownership_section)) {
                    SettingsItem(
                        title = stringResource(R.string.privacy_item_local_storage_title),
                        description = stringResource(R.string.privacy_item_local_storage_desc),
                        icon = Icons.Default.CloudDone,
                        trailing = {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(Dimens.spaceExtraSmall))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = Dimens.spaceSmall - Dimens.dividerThickness * 2, vertical = Dimens.dividerThickness * 2)
                            ) {
                                Text(
                                    text = stringResource(R.string.privacy_item_local_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsItem(
                        title = stringResource(R.string.privacy_delete_data_title),
                        description = stringResource(R.string.privacy_delete_data_desc),
                        icon = Icons.Default.DeleteOutline,
                        iconColor = MaterialTheme.colorScheme.error,
                        iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                        onClick = {}
                    )
                    SettingsDivider()
                    SettingsItem(
                        title = stringResource(R.string.settings_export_csv_title),
                        description = stringResource(R.string.settings_export_csv_desc),
                        icon = Icons.Default.FileDownload,
                        onClick = {}
                    )
                }
            }
            
            item {
                PelukCard(
                    modifier = Modifier.fillMaxWidth().clickable { /* Handle email */ }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(Dimens.minTouchTarget - Dimens.spaceSmall)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(DashboardTokens.MediumGap))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.privacy_questions_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.privacy_questions_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyHeader(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.spaceExtraSmall, vertical = Dimens.spaceSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
        }
        Text(
            text = stringResource(R.string.privacy_title),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium
        )
        IconButton(onClick = {}) {
            Icon(Icons.Default.Shield, contentDescription = stringResource(R.string.privacy_title), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun PrivacyNoticeBanner() {
    PelukCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(Dimens.buttonHeight)
                    .clip(RoundedCornerShape(DashboardTokens.LargeRadius))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(DashboardTokens.CardPadding))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.privacy_prioritized_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.privacy_prioritized_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(Modifier.size(Dimens.spaceExtraLarge * 2.5f), contentAlignment = Alignment.Center) {
                 // Placeholder for shield illustration
                 Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(Dimens.spaceExtraLarge * 2), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PrivacyPreview() {
    PELUKDIRITheme {
        PrivacyScreen(onBackClick = {})
    }
}
