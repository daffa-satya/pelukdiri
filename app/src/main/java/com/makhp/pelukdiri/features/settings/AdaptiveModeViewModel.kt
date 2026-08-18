package com.makhp.pelukdiri.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makhp.pelukdiri.core.domain.model.AggressivenessLevel
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdaptiveModeViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<AdaptiveModeUiState> = combine(
        userPreferencesRepository.aggressivenessLevel,
        userPreferencesRepository.monitoredPackages
    ) { aggressiveness, monitored ->
        AdaptiveModeUiState(
            isEnabled = true,
            interventionIntensity = aggressiveness.name,
            monitoredAppsCount = monitored.size,
            aggressivenessLevel = aggressiveness
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AdaptiveModeUiState()
    )

    fun setAggressiveness(level: AggressivenessLevel) {
        viewModelScope.launch {
            userPreferencesRepository.setAggressivenessLevel(level)
        }
    }
}
