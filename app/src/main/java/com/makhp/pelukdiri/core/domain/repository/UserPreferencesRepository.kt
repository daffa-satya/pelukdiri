package com.makhp.pelukdiri.core.domain.repository

import com.makhp.pelukdiri.core.domain.model.AggressivenessLevel
import kotlinx.coroutines.flow.Flow

data class UserPreferencesSnapshot(
    val isHistoryBackfilled: Boolean = false,
    val lastSyncedTimestamp: Long = 0L,
    val emergencyBypassUntil: Long = 0L,
    val monitoredPackages: Set<String> = emptySet(),
    val aggressivenessLevel: AggressivenessLevel = AggressivenessLevel.BALANCED,
    val isFixedLimitEnabled: Boolean = false,
    val fixedDailyLimitMinutes: Int = 60,
    val bedtime: String? = null,
    val wakeTime: String? = null,
    val currentDifficulty: Int = 2,
    val nextEligibleInterventionAt: Long = 0L,
    val activeInterventionSession: String? = null,
    val userNickname: String = "User",
    val username: String = "@user",
    val profileImagePath: String? = null,
    val isOnboardingCompleted: Boolean = false,
    val isDailySummaryEnabled: Boolean = true,
    val isWeeklyReflectionEnabled: Boolean = true,
    val isLimitReminderEnabled: Boolean = true,
    val isInterventionReminderEnabled: Boolean = true,
    val isDndEnabled: Boolean = false,
    val lastDailySummaryDate: String? = null,
    val lastWeeklyReflectionDate: String? = null,
    val lastLimitReminderTimestamp: Long = 0L,
)

interface UserPreferencesRepository {
    val snapshot: Flow<UserPreferencesSnapshot>
    val isHistoryBackfilled: Flow<Boolean>
    val lastSyncedTimestamp: Flow<Long>
    val emergencyBypassUntil: Flow<Long>
    val monitoredPackages: Flow<Set<String>>
    val aggressivenessLevel: Flow<AggressivenessLevel>
    val isFixedLimitEnabled: Flow<Boolean>
    val fixedDailyLimitMinutes: Flow<Int>
    val bedtime: Flow<String?>
    val wakeTime: Flow<String?>
    val currentDifficulty: Flow<Int>
    val nextEligibleInterventionAt: Flow<Long>
    val activeInterventionSession: Flow<String?>

    // User profile
    val userNickname: Flow<String>
    val username: Flow<String>
    val profileImagePath: Flow<String?>
    val isOnboardingCompleted: Flow<Boolean>

    // Notifications
    val isDailySummaryEnabled: Flow<Boolean>
    val isWeeklyReflectionEnabled: Flow<Boolean>
    val isLimitReminderEnabled: Flow<Boolean>
    val isInterventionReminderEnabled: Flow<Boolean>
    val isDndEnabled: Flow<Boolean>

    val lastDailySummaryDate: Flow<String?>
    val lastWeeklyReflectionDate: Flow<String?>
    val lastLimitReminderTimestamp: Flow<Long>

    suspend fun setHistoryBackfilled(isBackfilled: Boolean)
    suspend fun setLastSyncedTimestamp(timestamp: Long)
    suspend fun setEmergencyBypassUntil(timestamp: Long)
    suspend fun toggleMonitoredPackage(packageName: String)
    suspend fun setAggressivenessLevel(level: AggressivenessLevel)
    suspend fun setFixedLimitEnabled(enabled: Boolean)
    suspend fun setFixedDailyLimitMinutes(minutes: Int)
    suspend fun setBedtime(time: String?)
    suspend fun setWakeTime(time: String?)
    suspend fun setCurrentDifficulty(difficulty: Int)
    suspend fun setNextEligibleInterventionAt(timestamp: Long)
    suspend fun setActiveInterventionSession(encodedSession: String?)

    suspend fun setUserNickname(nickname: String)
    suspend fun setUsername(username: String)
    suspend fun setProfileImagePath(path: String?)
    suspend fun setOnboardingCompleted(completed: Boolean)

    suspend fun setDailySummaryEnabled(enabled: Boolean)
    suspend fun setWeeklyReflectionEnabled(enabled: Boolean)
    suspend fun setLimitReminderEnabled(enabled: Boolean)
    suspend fun setInterventionReminderEnabled(enabled: Boolean)
    suspend fun setDndEnabled(enabled: Boolean)

    suspend fun setLastDailySummaryDate(date: String?)
    suspend fun setLastWeeklyReflectionDate(date: String?)
    suspend fun setLastLimitReminderTimestamp(timestamp: Long)
}
