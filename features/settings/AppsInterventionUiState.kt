package com.makhp.pelukdiri.features.settings

import androidx.compose.runtime.Immutable
import com.makhp.pelukdiri.core.domain.model.AppUsage

@Immutable
data class AppsInterventionUiState(
    val searchQuery: String = "",
    val apps: List<AppUsage> = emptyList(),
    val selectedPackageNames: Set<String> = emptySet(),
    val isLoading: Boolean = false
) {
    val selectedCount: Int get() = selectedPackageNames.size
}
