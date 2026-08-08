package com.makhp.pelukdiri.features.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    fun toggleAdaptiveMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isAdaptiveModeEnabled = enabled)
    }
    
    fun logout() {
        // Handle logout
    }
}
