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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.makhp.pelukdiri.features.dashboard.DashboardTokens
import com.makhp.pelukdiri.ui.components.PelukCard
import com.makhp.pelukdiri.ui.theme.PELUKDIRITheme

@Composable
fun SettingsScreen(
    onHomeClick: () -> Unit,
    onProgressClick: () -> Unit,
    onNavigateToAdaptiveMode: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    SettingsLayout(
        state = state,
        onHomeClick = onHomeClick,
        onProgressClick = onProgressClick,
        onNavigateToAdaptiveMode = onNavigateToAdaptiveMode,
        onNavigateToNotifications = onNavigateToNotifications,
        onNavigateToExport = onNavigateToExport,
        onNavigateToPrivacy = onNavigateToPrivacy,
        onNavigateToTerms = onNavigateToTerms,
        onNavigateToAbout = onNavigateToAbout,
        onLogout = viewModel::logout
    )
}

@Composable
private fun SettingsLayout(
    state: SettingsUiState,
    onHomeClick: () -> Unit,
    onProgressClick: () -> Unit,
    onNavigateToAdaptiveMode: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            SettingsNavigation(onHomeClick, onProgressClick)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(DashboardTokens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardTokens.LargeGap)
        ) {
            item { SettingsHeader() }
            
            item { SettingsBanner() }
            
            item {
                SettingsSection(title = "Preferensi & Kontrol") {
                    SettingsItem(
                        title = "Adaptive Mode",
                        description = "Atur sensitivitas dan cara kerja algoritma adaptive sesuai kebiasaanmu.",
                        icon = Icons.Default.Tune,
                        onClick = onNavigateToAdaptiveMode
                    )
                    SettingsDivider()
                    SettingsItem(
                        title = "Notifications",
                        description = "Kelola notifikasi, ringkasan harian, dan pengingat batas waktu adaptif.",
                        icon = Icons.Default.Notifications,
                        onClick = onNavigateToNotifications,
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = state.notificationStatus,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsItem(
                        title = "Export CSV",
                        description = "Ekspor data penggunaan aplikasi dan intervensi kamu.",
                        icon = Icons.Default.FileDownload,
                        onClick = onNavigateToExport
                    )
                }
            }
            
            item {
                SettingsSection(title = "Informasi & Dukungan") {
                    SettingsItem(
                        title = "Privacy",
                        description = "Lihat kebijakan privasi dan cara kami melindungi datamu.",
                        icon = Icons.Default.Shield,
                        onClick = onNavigateToPrivacy
                    )
                    SettingsDivider()
                    SettingsItem(
                        title = "Terms",
                        description = "Baca syarat dan ketentuan penggunaan PELUKDIRI.",
                        icon = Icons.Default.Description,
                        onClick = onNavigateToTerms
                    )
                    SettingsDivider()
                    SettingsItem(
                        title = "About",
                        description = "Informasi tentang PELUKDIRI, versi aplikasi, dan tim pengembang.",
                        icon = Icons.Default.Info,
                        onClick = onNavigateToAbout
                    )
                }
            }
            
            item {
                PelukCard(
                    modifier = Modifier.fillMaxWidth().clickable { onLogout() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(DashboardTokens.SmallRadius))
                                .background(Color(0xFFFEE2E2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                tint = Color(0xFFEF4444)
                            )
                        }
                        Spacer(Modifier.width(DashboardTokens.MediumGap))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Keluar",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Keluar dari akun PELUKDIRI di perangkat ini.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            item { Spacer(Modifier.height(DashboardTokens.LargeGap)) }
        }
    }
}

@Composable
private fun SettingsHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { /* Handle back */ }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(
            text = "Pengaturan",
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
private fun SettingsBanner() {
    PelukCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(DashboardTokens.LargeRadius))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("🌿", style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(Modifier.width(DashboardTokens.CardPadding))
            Column {
                Text(
                    text = "Kamu memegang kendali",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Atur PELUKDIRI sesuai kebutuhanmu.\nKamu bisa mengubahnya kapan saja.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsNavigation(
    onHomeClick: () -> Unit,
    onProgressClick: () -> Unit
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NavigationBarItem(
            selected = false,
            onClick = onHomeClick,
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = false,
            onClick = onProgressClick,
            icon = { Icon(Icons.Default.PieChart, null) },
            label = { Text("Analytics") }
        )
        NavigationBarItem(
            selected = true,
            onClick = {},
            icon = { Icon(Icons.Default.Settings, null) },
            label = { Text("Settings") }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    PELUKDIRITheme {
        SettingsLayout(
            state = SettingsUiState(),
            onHomeClick = {},
            onProgressClick = {},
            onNavigateToAdaptiveMode = {},
            onNavigateToNotifications = {},
            onNavigateToExport = {},
            onNavigateToPrivacy = {},
            onNavigateToTerms = {},
            onNavigateToAbout = {},
            onLogout = {}
        )
    }
}
