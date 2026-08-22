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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.makhp.pelukdiri.R
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import com.makhp.pelukdiri.core.domain.model.AppUsage
import com.makhp.pelukdiri.features.dashboard.DashboardTokens
import com.makhp.pelukdiri.ui.components.PelukDiriLogo
import com.makhp.pelukdiri.ui.components.AppIcon
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
            contentPadding = PaddingValues(
                start = DashboardTokens.ScreenPadding,
                end = DashboardTokens.ScreenPadding,
                top = Dimens.spaceExtraLarge,
                bottom = Dimens.spaceExtraLarge * 2
            ),
            verticalArrangement = Arrangement.spacedBy(DashboardTokens.MediumGap)
        ) {
            item {
                Text(
                    text = stringResource(R.string.apps_intervention_desc),
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
                        text = stringResource(R.string.apps_intervention_installed_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.apps_intervention_selected_count_label, state.selectedCount),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (state.isLoading) {
                item {
                    val loadingDescription = stringResource(R.string.apps_intervention_loading)
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(Dimens.spaceLarge),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(Dimens.minTouchTarget)
                                .semantics {
                                    contentDescription = loadingDescription
                                }
                        )
                    }
                }
            } else {
                items(state.apps, key = { it.packageName }) { app ->
                    AppSelectionItem(
                        app = app,
                        isSelected = state.selectedPackageNames.contains(app.packageName),
                        onToggle = { onToggleApp(app.packageName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppsInterventionHeader(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Dimens.spaceExtraSmall, vertical = Dimens.spaceSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
        }
        Text(
            text = stringResource(R.string.apps_intervention_title),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium
        )
        Box(Modifier.size(Dimens.minTouchTarget), contentAlignment = Alignment.Center) {
            PelukDiriLogo(size = 28.dp)
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
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(DashboardTokens.CardPadding))
            Column {
                Text(
                    text = stringResource(R.string.apps_intervention_focus_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.apps_intervention_focus_desc),
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
        placeholder = { Text(stringResource(R.string.common_search_hint)) },
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
            AppIcon(
                packageName = app.packageName,
                appName = app.appName,
                size = DashboardTokens.AppIconSize
            )
            
            Spacer(Modifier.width(DashboardTokens.MediumGap))
            
            Column(Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.apps_intervention_per_day_format, formatDuration(app.usageDurationMillis)),
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
            .navigationBarsPadding()
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
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(Modifier.width(DashboardTokens.SmallGap))
            Text(stringResource(R.string.apps_intervention_save_button), style = MaterialTheme.typography.titleMedium)
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
