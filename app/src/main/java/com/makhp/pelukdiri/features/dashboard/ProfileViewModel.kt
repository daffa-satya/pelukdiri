package com.makhp.pelukdiri.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makhp.pelukdiri.core.domain.InterventionLockManager
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import com.makhp.pelukdiri.core.util.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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
    private val lockManager: InterventionLockManager,
    private val notificationHelper: NotificationHelper
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

    fun triggerTestNotification() {
        viewModelScope.launch {
            // 1. Tes Daily Summary
            notificationHelper.showDailySummaryNotification(3600000L) 
            delay(2000)
            
            // 2. Tes Weekly Reflection
            notificationHelper.showWeeklyReflectionNotification()
            delay(2000)
            
            // 3. Tes Limit Reminder
            notificationHelper.showLimitReminderNotification()
        }
    }

}
