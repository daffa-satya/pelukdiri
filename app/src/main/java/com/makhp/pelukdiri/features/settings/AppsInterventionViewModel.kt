package com.makhp.pelukdiri.features.settings

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makhp.pelukdiri.core.domain.model.AppUsage
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AppsInterventionViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    
    val uiState: StateFlow<AppsInterventionUiState> = combine(
        userPreferencesRepository.monitoredPackages,
        _searchQuery
    ) { monitored, query ->
        val allApps = getInstalledApps()
        val filtered = if (query.isBlank()) {
            allApps
        } else {
            allApps.filter { it.appName.contains(query, ignoreCase = true) }
        }
        
        AppsInterventionUiState(
            searchQuery = query,
            apps = filtered.toImmutableList(),
            selectedPackageNames = monitored.toImmutableSet()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppsInterventionUiState()
    )

    private suspend fun getInstalledApps(): List<AppUsage> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null)
        intent.addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        
        pm.queryIntentActivities(intent, 0).map { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            val appName = resolveInfo.loadLabel(pm).toString()
            AppUsage(
                packageName = packageName,
                appName = appName,
                usageDurationMillis = 0, // Not needed here
                lastUsedTimestamp = 0
            )
        }.distinctBy { it.packageName }.sortedBy { it.appName }
    }

    fun onQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleApp(packageName: String) {
        viewModelScope.launch {
            userPreferencesRepository.toggleMonitoredPackage(packageName)
        }
    }
}
