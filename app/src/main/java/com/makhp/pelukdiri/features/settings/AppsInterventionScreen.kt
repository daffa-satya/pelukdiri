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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import com.makhp.pelukdiri.core.domain.model.AppUsage
import com.makhp.pelukdiri.features.dashboard.DashboardTokens
import com.makhp.pelukdiri.ui.components.PelukCard
import com.makhp.pelukdiri.ui.components.formatDuration
import com.makhp.pelukdiri.ui.theme.Dimens
import com.makhp.pelukdiri.ui.theme.PELUKDIRITheme

@Composable
fun AppsInterventionScreen(
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    state: AppsInterventionUiState = AppsInterventionUiState(),
    onQueryChange: (String) -> Unit = {},
    onToggleApp: (String) -> Unit = {}
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppsInterventionHeader(onBackClick)
        },
        bottomBar = {
            AppsInterventionBottomBar(onSaveClick)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(DashboardTokens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardTokens.MediumGap)
        ) {
            item {
                Text(
                    text = "Pilih aplikasi yang ingin kamu batasi dan diintervensi.\nIntervensi akan muncul saat kamu membuka aplikasi ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            item {
                RecommendationBanner()
            }
            
            item {
                SearchBar(state.searchQuery, onQueryChange)
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Aplikasi Terpasang",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${state.selectedCount} dipilih",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            items(state.apps, key = { it.packageName }) { app ->
                AppSelectionItem(
                    app = app,
                    isSelected = state.selectedPackageNames.contains(app.packageName),
                    onToggle = { onToggleApp(app.packageName) }
                )
            }
            
            item { Spacer(Modifier.height(DashboardTokens.LargeGap)) }
        }
    }
}

@Composable
private fun AppsInterventionHeader(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.spaceExtraSmall, vertical = Dimens.spaceSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(
            text = "Aplikasi yang Diintervensi",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium
        )
        Box(Modifier.size(Dimens.minTouchTarget), contentAlignment = Alignment.Center) {
            Text("🌿", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun RecommendationBanner() {
    PelukCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(Dimens.minTouchTarget)
                    .clip(RoundedCornerShape(DashboardTokens.LargeRadius))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Rekomendasi",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(DashboardTokens.CardPadding))
            Column {
                Text(
                    text = "Fokus pada yang penting",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Pilih aplikasi yang paling sering membuatmu terdistraksi agar intervensi lebih efektif dan tidak mengganggu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Cari aplikasi...") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        shape = RoundedCornerShape(DashboardTokens.LargeRadius),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun AppSelectionItem(
    app: AppUsage,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    PelukCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(DashboardTokens.AppIconSize)
                    .clip(RoundedCornerShape(DashboardTokens.SmallRadius))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.appName.firstOrNull()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(Modifier.width(DashboardTokens.MediumGap))
            
            Column(Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${formatDuration(app.usageDurationMillis)} per hari",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }
    }
}

@Composable
private fun AppsInterventionBottomBar(onSaveClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(DashboardTokens.ScreenPadding)
    ) {
        Button(
            onClick = onSaveClick,
            modifier = Modifier.fillMaxWidth().height(Dimens.minTouchTarget + Dimens.spaceSmall),
            shape = RoundedCornerShape(DashboardTokens.MediumRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Check, contentDescription = "Simpan perubahan")
            Spacer(Modifier.width(DashboardTokens.SmallGap))
            Text("Simpan Pilihan", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppsInterventionPreview() {
    PELUKDIRITheme {
        AppsInterventionScreen(
            onBackClick = {},
            onSaveClick = {},
            state = AppsInterventionUiState(
                apps = persistentListOf(
                    AppUsage("com.instagram", "Instagram", 3600000L, 0),
                    AppUsage("com.tiktok", "TikTok", 7200000L, 0)
                ),
                selectedPackageNames = persistentSetOf("com.instagram")
            )
        )
    }
}
