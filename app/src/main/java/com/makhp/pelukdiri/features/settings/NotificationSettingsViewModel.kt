package com.makhp.pelukdiri.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationSettingsUiState(
    val isDailySummaryEnabled: Boolean = true,
    val isWeeklyReflectionEnabled: Boolean = true,
    val isLimitReminderEnabled: Boolean = true,
    val isInterventionReminderEnabled: Boolean = true
)

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<NotificationSettingsUiState> = combine(
        userPreferencesRepository.isDailySummaryEnabled,
        userPreferencesRepository.isWeeklyReflectionEnabled,
        userPreferencesRepository.isLimitReminderEnabled,
        userPreferencesRepository.isInterventionReminderEnabled
    ) { daily, weekly, limit, intervention ->
        NotificationSettingsUiState(daily, weekly, limit, intervention)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotificationSettingsUiState()
    )

    fun setDailySummaryEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setDailySummaryEnabled(enabled) }
    }

    fun setWeeklyReflectionEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setWeeklyReflectionEnabled(enabled) }
    }

    fun setLimitReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setLimitReminderEnabled(enabled) }
    }

    fun setInterventionReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setInterventionReminderEnabled(enabled) }
    }
}
