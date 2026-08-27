package com.makhp.pelukdiri.features.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makhp.pelukdiri.core.domain.model.AppUsage
import com.makhp.pelukdiri.core.domain.repository.UsageRepository
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AppsInterventionViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val usageRepository: UsageRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _installedApps = MutableStateFlow<List<AppUsage>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    
    val uiState: StateFlow<AppsInterventionUiState> = combine(
        userPreferencesRepository.monitoredPackages,
        _searchQuery,
        _installedApps,
        _isLoading
    ) { monitored, query, allApps, isLoading ->
        val filtered = if (query.isBlank()) {
            allApps
        } else {
            allApps.filter { it.appName.contains(query, ignoreCase = true) }
        }
        
        AppsInterventionUiState(
            searchQuery = query,
            apps = filtered.sortedByDescending { it.packageName in monitored }.toImmutableList(),
            installedPackageNames = allApps.mapTo(mutableSetOf()) { it.packageName }.toImmutableSet(),
            selectedPackageNames = monitored.toImmutableSet(),
            isLoading = isLoading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppsInterventionUiState()
    )

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            try {
                val apps = getInstalledApps()
                _installedApps.value = apps

                try {
                    val todayUsage = usageRepository.getDailyUsage(LocalDate.now()).first()
                        .associateBy { it.packageName }

                    _installedApps.value = apps.map { app ->
                        todayUsage[app.packageName]?.let { usage ->
                            app.copy(usageDurationMillis = usage.usageDurationMillis)
                        } ?: app
                    }.sortedByDescending { it.usageDurationMillis }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    // Usage duration is optional; keep the installed-app list available.
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

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
                usageDurationMillis = 0,
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
