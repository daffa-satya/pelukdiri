package com.makhp.pelukdiri.features.settings

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SpeakerNotes
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.makhp.pelukdiri.R
import com.makhp.pelukdiri.features.dashboard.DashboardTokens
import com.makhp.pelukdiri.ui.components.PelukCard
import com.makhp.pelukdiri.ui.theme.Dimens
import com.makhp.pelukdiri.ui.theme.PELUKDIRITheme

@Composable
fun ExportCsvScreen(
    onBackClick: () -> Unit,
    onExportClick: () -> Unit = {}
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ExportCsvHeader(onBackClick)
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
                    text = stringResource(R.string.export_intro_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            item {
                ExportNoticeBanner()
            }
            
            item {
                SettingsSection(title = stringResource(R.string.settings_export_confirm_title)) {
                    ExportDataItem(stringResource(R.string.export_item_usage_title), stringResource(R.string.export_item_usage_desc), Icons.Default.AccessTime, true)
                    SettingsDivider()
                    ExportDataItem(stringResource(R.string.export_item_sensor_title), stringResource(R.string.export_item_sensor_desc), Icons.Default.Lock, true)
                    SettingsDivider()
                    ExportDataItem(stringResource(R.string.export_item_summary_title), stringResource(R.string.export_item_summary_desc), Icons.Default.GridView, true)
                    SettingsDivider()
                    ExportDataItem(stringResource(R.string.export_item_intervention_title), stringResource(R.string.export_item_intervention_desc), Icons.Default.Psychology, true)
                    SettingsDivider()
                    ExportDataItem(stringResource(R.string.export_item_decisions_title), stringResource(R.string.export_item_decisions_desc), Icons.Default.SpeakerNotes, true)
                    SettingsDivider()
                    ExportDataItem(stringResource(R.string.export_item_limits_title), stringResource(R.string.export_item_limits_desc), Icons.Default.Lock, true)
                }
            }
            
            item {
                PelukCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(Dimens.minTouchTarget)
                                .clip(RoundedCornerShape(DashboardTokens.SmallRadius))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Storage, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(DashboardTokens.MediumGap))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.export_format_label),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.export_format_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(DashboardTokens.SmallRadius),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = stringResource(R.string.export_format_all_zip),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = Dimens.spaceSmall, vertical = Dimens.spaceExtraSmall),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            
            item {
                Button(
                    onClick = onExportClick,
                    modifier = Modifier.fillMaxWidth().height(Dimens.minTouchTarget + Dimens.spaceSmall),
                    shape = RoundedCornerShape(DashboardTokens.MediumRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = stringResource(R.string.export_download_icon_desc))
                    Spacer(Modifier.width(DashboardTokens.SmallGap))
                    Text(stringResource(R.string.export_button_text), style = MaterialTheme.typography.titleMedium)
                }
            }
            
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.export_storage_location_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.export_folder_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
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
                    Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.privacy_title), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(Dimens.iconSizeMedium - Dimens.spaceExtraSmall))
                    Spacer(Modifier.width(DashboardTokens.MediumGap))
                    Text(
                        text = stringResource(R.string.export_privacy_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportCsvHeader(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.spaceExtraSmall, vertical = Dimens.spaceSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali ke pengaturan")
        }
        Spacer(Modifier.width(DashboardTokens.SmallGap))
        Text(
            text = "Export CSV / ZIP",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ExportNoticeBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DashboardTokens.MediumRadius))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
            .padding(DashboardTokens.CardPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(DashboardTokens.MediumGap))
        Text(
            text = "File ekspor berisi log sensor, intervensi, batas adaptif, dan audit keputusan evaluasi lengkap dalam format CSV standar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ExportDataItem(title: String, description: String, icon: ImageVector, checked: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DashboardTokens.MediumGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.minTouchTarget - Dimens.spaceSmall)
                .clip(RoundedCornerShape(DashboardTokens.SmallRadius))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(Dimens.iconSizeMedium))
        }
        Spacer(Modifier.width(DashboardTokens.MediumGap))
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Checkbox(
            checked = checked,
            onCheckedChange = {},
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExportCsvPreview() {
    PELUKDIRITheme {
        ExportCsvScreen(onBackClick = {})
    }
}
