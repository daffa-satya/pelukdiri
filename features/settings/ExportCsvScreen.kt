package com.makhp.pelukdiri.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.makhp.pelukdiri.features.dashboard.DashboardTokens
import com.makhp.pelukdiri.ui.components.PelukCard
import com.makhp.pelukdiri.ui.theme.PELUKDIRITheme

@Composable
fun ExportCsvScreen(
    onBackClick: () -> Unit
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
                    text = "Ekspor data penggunaan aplikasi dan intervensi kamu dalam format CSV untuk keperluan penelitian.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            item {
                ExportNoticeBanner()
            }
            
            item {
                SettingsSection(title = "Rentang Tanggal") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = DashboardTokens.MediumGap),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DateDropdown(label = "Tanggal Awal", date = "1 Mei 2025", modifier = Modifier.weight(1f))
                        Text("—", modifier = Modifier.padding(horizontal = 8.dp))
                        DateDropdown(label = "Tanggal Akhir", date = "31 Mei 2025", modifier = Modifier.weight(1f))
                    }
                    Text(
                        text = "Maksimal rentang 31 hari per ekspor.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            item {
                SettingsSection(title = "Jenis Data yang Diekspor") {
                    ExportDataItem("Screen Time", "Total waktu layar harian per aplikasi.", Icons.Default.AccessTime, true)
                    SettingsDivider()
                    ExportDataItem("Unlock Count", "Jumlah membuka kunci dan membuka aplikasi.", Icons.Default.Lock, true)
                    SettingsDivider()
                    ExportDataItem("Top Apps", "Daftar aplikasi teratas setiap hari.", Icons.Default.GridView, true)
                    SettingsDivider()
                    ExportDataItem("Intervention History", "Riwayat soal intervensi (jenis, waktu, hasil).", Icons.Default.Psychology, true)
                    SettingsDivider()
                    ExportDataItem("Daily Reflection", "Refleksi harian & insight (jika ada).", Icons.Default.SpeakerNotes, false)
                }
            }
            
            item {
                PelukCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(DashboardTokens.SmallRadius))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Storage, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(DashboardTokens.MediumGap))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Format File",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Data akan diekspor dalam format CSV (.csv) yang bisa dibuka dengan Excel atau Google Sheets.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.outlineVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("CSV", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            item {
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(DashboardTokens.MediumRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.FileDownload, null)
                    Spacer(Modifier.width(DashboardTokens.SmallGap))
                    Text("Export CSV", style = MaterialTheme.typography.titleMedium)
                }
            }
            
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "File akan tersimpan di perangkat kamu",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Folder: PELUKDIRI/Export",
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
                        .background(Color(0xFFF1F8F1))
                        .padding(DashboardTokens.CardPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(DashboardTokens.MediumGap))
                    Text(
                        text = "Privasi Terjamin: Seluruh data diproses di perangkat kamu dan tidak dikirim ke server.",
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
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(
            text = "Export CSV",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium
        )
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Text("🌿", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun ExportNoticeBanner() {
    PelukCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(DashboardTokens.CardPadding))
            Text(
                text = "Data kamu aman dan hanya kamu yang memiliki akses. Kami tidak membagikan data ini ke pihak manapun.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DateDropdown(label: String, date: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DashboardTokens.SmallRadius))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(Icons.Default.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = date, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
                .size(40.dp)
                .clip(RoundedCornerShape(DashboardTokens.SmallRadius))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
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
