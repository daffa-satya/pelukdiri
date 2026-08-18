package com.makhp.pelukdiri.features.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makhp.pelukdiri.core.domain.model.AggressivenessLevel
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        userPreferencesRepository.aggressivenessLevel,
        userPreferencesRepository.isFixedLimitEnabled,
        userPreferencesRepository.fixedDailyLimitMinutes,
        userPreferencesRepository.bedtime,
        userPreferencesRepository.wakeTime
    ) { aggressiveness, isFixed, fixedLimit, sleep, wake ->
        SettingsUiState(
            aggressivenessLevel = aggressiveness,
            isFixedLimitEnabled = isFixed,
            fixedDailyLimitMinutes = fixedLimit,
            sleepTime = sleep ?: "22:00",
            wakeTime = wake ?: "06:00",
            appVersion = "1.0.0 (Beta)"
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setAggressiveness(level: AggressivenessLevel) {
        viewModelScope.launch {
            userPreferencesRepository.setAggressivenessLevel(level)
        }
    }

    fun toggleFixedLimit(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setFixedLimitEnabled(enabled)
        }
    }

    fun setFixedLimitMinutes(minutes: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setFixedDailyLimitMinutes(minutes)
        }
    }

    fun setSleepTime(time: String) {
        viewModelScope.launch {
            userPreferencesRepository.setBedtime(time)
        }
    }

    fun setWakeTime(time: String) {
        viewModelScope.launch {
            userPreferencesRepository.setWakeTime(time)
        }
    }
    
    fun logout() {
        // Handle logout logic
    }
}
