package com.makhp.pelukdiri.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val nickname: String = "",
    val username: String = "",
    val profileImagePath: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val lockManager: com.makhp.pelukdiri.core.domain.InterventionLockManager
) : ViewModel() {

    fun tryAcquireInterventionLock(): Boolean {
        return lockManager.acquireLock()
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        userPreferencesRepository.userNickname,
        userPreferencesRepository.username,
        userPreferencesRepository.profileImagePath
    ) { nickname, username, path ->
        ProfileUiState(nickname, username, path)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState()
    )

    fun updateNickname(nickname: String) {
        viewModelScope.launch {
            userPreferencesRepository.setUserNickname(nickname)
        }
    }

    fun updateUsername(username: String) {
        viewModelScope.launch {
            userPreferencesRepository.setUsername(username)
        }
    }

    fun updateProfileImage(path: String?) {
        viewModelScope.launch {
            userPreferencesRepository.setProfileImagePath(path)
        }
    }
}
