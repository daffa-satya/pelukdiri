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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.makhp.pelukdiri.features.dashboard.DashboardTokens
import com.makhp.pelukdiri.ui.components.PelukCard
import com.makhp.pelukdiri.ui.theme.PELUKDIRITheme

@Composable
fun AboutScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AboutHeader(onBackClick)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(DashboardTokens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardTokens.LargeGap),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(Modifier.height(DashboardTokens.LargeGap))
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(DashboardTokens.CardRadius))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🌿", style = MaterialTheme.typography.displayLarge)
                }
                Spacer(Modifier.height(DashboardTokens.MediumGap))
                Text(
                    text = "PELUKDIRI",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Think. Control. Freedom.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            item {
                SettingsSection(title = "Informasi Aplikasi") {
                    SettingsItem(
                        title = "Versi",
                        description = "1.0.0 (Build 20260807)",
                        icon = Icons.Default.Update,
                        onClick = {}
                    )
                    SettingsDivider()
                    SettingsItem(
                        title = "Website",
                        description = "www.pelukdiri.com",
                        icon = Icons.Default.Language,
                        onClick = {}
                    )
                    SettingsDivider()
                    SettingsItem(
                        title = "Tim Pengembang",
                        description = "MAKHP Studio",
                        icon = Icons.Default.Code,
                        onClick = {}
                    )
                }
            }
            
            item {
                PelukCard {
                    Text(
                        text = "PELUKDIRI adalah aplikasi kesejahteraan digital adaptif yang membantu Anda mengelola waktu layar secara mindful. Dengan teknologi intervensi cerdas, kami membantu Anda kembali memegang kendali atas perangkat Anda.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            item {
                Text(
                    text = "© 2026 MAKHP. All Rights Reserved.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AboutHeader(onBackClick: () -> Unit) {
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
            text = "About",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium
        )
        Box(Modifier.size(48.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutPreview() {
    PELUKDIRITheme {
        AboutScreen(onBackClick = {})
    }
}
