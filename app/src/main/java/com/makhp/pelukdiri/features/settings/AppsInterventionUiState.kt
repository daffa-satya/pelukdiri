package com.makhp.pelukdiri.features.settings

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import com.makhp.pelukdiri.core.domain.model.AppUsage

@Immutable
data class AppsInterventionUiState(
    val searchQuery: String = "",
    val apps: ImmutableList<AppUsage> = persistentListOf(),
    val installedPackageNames: ImmutableSet<String> = persistentSetOf(),
    val selectedPackageNames: ImmutableSet<String> = persistentSetOf(),
    val isLoading: Boolean = true
) {
    val selectedCount: Int get() = selectedPackageNames.count { it in installedPackageNames }
}
