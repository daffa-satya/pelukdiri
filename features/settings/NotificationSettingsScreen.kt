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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Timeline
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
fun NotificationSettingsScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NotificationSettingsHeader(onBackClick)
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
                    text = "Kelola notifikasi yang ingin kamu terima.\nKamu bisa mengaktifkan atau menonaktifkan sesuai kebutuhanmu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            item {
                NotificationNoticeBanner()
            }
            
            item {
                SettingsSection(title = "Pengaturan Notifikasi") {
                    NotificationToggleItem(
                        title = "Daily Summary",
                        description = "Dapatkan ringkasan penggunaan harianmu setiap malam.",
                        icon = Icons.Default.CalendarMonth,
                        checked = true,
                        time = "20.00"
                    )
                    SettingsDivider()
                    NotificationToggleItem(
                        title = "Weekly Reflection",
                        description = "Lihat refleksi dan progres mingguanmu setiap hari Minggu.",
                        icon = Icons.Default.Timeline,
                        checked = true,
                        time = "19.00"
                    )
                    SettingsDivider()
                    NotificationToggleItem(
                        title = "Limit Reminder",
                        description = "Pengingat ketika kamu mendekati atau mencapai batas waktu layar adaptif.",
                        icon = Icons.Default.HourglassBottom,
                        checked = true,
                        time = "Saat mendekati limit"
                    )
                    SettingsDivider()
                    NotificationToggleItem(
                        title = "Intervention Reminder",
                        description = "Pengingat untuk mengerjakan intervensi cerdas yang membantumu tetap fokus.",
                        icon = Icons.Default.Psychology,
                        checked = true,
                        time = "Saat intervensi muncul"
                    )
                }
            }
            
            item {
                PelukCard(
                    modifier = Modifier.fillMaxWidth().clickable { /* Handle DND */ }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(DashboardTokens.SmallRadius))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoNotDisturbOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(DashboardTokens.MediumGap))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Mode Jangan Ganggu",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Nonaktifkan semua notifikasi sementara waktu.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Nonaktif",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                    Text("🌿", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(DashboardTokens.MediumGap))
                    Text(
                        text = "Kamu akan tetap menerima notifikasi penting meskipun beberapa notifikasi dinonaktifkan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun NotificationSettingsHeader(onBackClick: () -> Unit) {
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
            text = "Notifikasi",
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
private fun NotificationNoticeBanner() {
    PelukCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(DashboardTokens.LargeRadius))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(DashboardTokens.CardPadding))
            Column {
                Text(
                    text = "Notifikasi aktif membantu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Agar kamu tetap teringat dan termotivasi untuk mengelola waktu layar dengan lebih baik.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NotificationToggleItem(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    time: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DashboardTokens.MediumGap)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(DashboardTokens.SmallRadius))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(DashboardTokens.MediumGap))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SettingsSwitch(checked = checked, onCheckedChange = {})
        }
        
        if (time != null) {
            Spacer(Modifier.height(DashboardTokens.SmallGap))
            Box(
                modifier = Modifier
                    .padding(start = 56.dp)
                    .clip(RoundedCornerShape(DashboardTokens.SmallRadius))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationSettingsPreview() {
    PELUKDIRITheme {
        NotificationSettingsScreen(onBackClick = {})
    }
}
