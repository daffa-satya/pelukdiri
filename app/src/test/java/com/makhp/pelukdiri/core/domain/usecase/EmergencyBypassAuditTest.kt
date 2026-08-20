package com.makhp.pelukdiri.core.domain.usecase

import com.makhp.pelukdiri.core.domain.model.AggressivenessLevel
import com.makhp.pelukdiri.core.domain.model.InterventionLog
import com.makhp.pelukdiri.core.domain.repository.InterventionLogRepository
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZonedDateTime

class EmergencyBypassAuditTest {

    private lateinit var useCase: PerformEmergencyBypassUseCase
    private lateinit var fakeInterventionLogRepository: FakeInterventionLogRepository
    private lateinit var fakeUserPreferencesRepository: FakeUserPreferencesRepository

    @Before
    fun setup() {
        fakeInterventionLogRepository = FakeInterventionLogRepository()
        fakeUserPreferencesRepository = FakeUserPreferencesRepository()
        useCase = PerformEmergencyBypassUseCase(
            fakeInterventionLogRepository,
            fakeUserPreferencesRepository
        )
    }

    @Test
    fun `successful bypass sets 3 minute window and restores eligibility`() = runBlocking {
        val now = System.currentTimeMillis()
        val result = useCase.invoke(0.5, 0.6, 2, 0, 1000L)

        assertTrue(result is BypassResult.Success)
        
        val bypassUntil = fakeUserPreferencesRepository.bypassUntil
        val nextEligible = fakeUserPreferencesRepository.nextEligibleAt
        
        // Should be exactly 3 minutes (180,000 ms) from "now"
        // Allowing small delta for execution time
        assertTrue(bypassUntil >= now + 180_000L)
        assertEquals(bypassUntil, nextEligible)
    }

    @Test
    fun `bypass overwrites longer proactive cooldown`() = runBlocking {
        // Set a long cooldown (1 hour from now)
        val farFuture = System.currentTimeMillis() + 3_600_000L
        fakeUserPreferencesRepository.setNextEligibleInterventionAt(farFuture)
        
        val result = useCase.invoke(0.5, 0.6, 2, 0, 1000L)
        
        assertTrue(result is BypassResult.Success)
        val nextEligible = fakeUserPreferencesRepository.nextEligibleAt
        
        // nextEligible should now be pulled back to ~3 minutes from now
        assertTrue(nextEligible < farFuture)
        assertTrue(nextEligible >= System.currentTimeMillis() + 179_000L)
    }

    @Test
    fun `daily limit of 5 is strictly enforced`() = runBlocking {
        // Perform 5 successful bypasses
        repeat(5) { i ->
            val result = useCase.invoke(0.5, 0.6, 2, 0, 1000L)
            assertTrue("Bypass #$i should succeed", result is BypassResult.Success)
        }
        
        // 6th bypass should be rejected
        val result6 = useCase.invoke(0.5, 0.6, 2, 0, 1000L)
        assertTrue("6th bypass should be exhausted", result6 is BypassResult.Exhausted)
        
        assertEquals(5, fakeInterventionLogRepository.logs.size)
    }

    @Test
    fun `rejected bypass does not alter timestamps`() = runBlocking {
        // Exhaust quota
        repeat(5) { useCase.invoke(0.5, 0.6, 2, 0, 1000L) }
        
        val lastBypassUntil = fakeUserPreferencesRepository.bypassUntil
        val lastNextEligible = fakeUserPreferencesRepository.nextEligibleAt
        
        // Wait a bit to ensure a potential update would have a different timestamp
        Thread.sleep(10)
        
        val result6 = useCase.invoke(0.5, 0.6, 2, 0, 1000L)
        assertTrue(result6 is BypassResult.Exhausted)
        
        assertEquals(lastBypassUntil, fakeUserPreferencesRepository.bypassUntil)
        assertEquals(lastNextEligible, fakeUserPreferencesRepository.nextEligibleAt)
    }

    // Fakes
    private class FakeInterventionLogRepository : InterventionLogRepository {
        val logs = mutableListOf<InterventionLog>()
        override suspend fun insertLog(log: InterventionLog) { logs.add(log) }
        override fun getAllLogs(): Flow<List<InterventionLog>> = flowOf(logs)
        override suspend fun getAllLogsList(): List<InterventionLog> = logs
        override suspend fun getAverageResponseTime(difficulty: Int): Double? = null
        override suspend fun getRecentValidSuccessfulLogsByDifficulty(difficulty: Int, limit: Int): List<InterventionLog> = emptyList()
        override suspend fun getLatestLog(): InterventionLog? = logs.lastOrNull()
        override suspend fun getLatestValidPerformanceLogByDifficulty(difficulty: Int): InterventionLog? = null
        override suspend fun getBypassCountForDay(startOfDay: Long, endOfDay: Long): Int {
            return logs.count { it.isBypassed && it.timestamp >= startOfDay && it.timestamp < endOfDay }
        }
    }

    private class FakeUserPreferencesRepository : UserPreferencesRepository {
        var bypassUntil: Long = 0L
        var nextEligibleAt: Long = 0L

        override val emergencyBypassUntil: Flow<Long> get() = flowOf(bypassUntil)
        override val nextEligibleInterventionAt: Flow<Long> get() = flowOf(nextEligibleAt)
        override val activeInterventionSession = flowOf<String?>(null)

        override suspend fun setEmergencyBypassUntil(timestamp: Long) { bypassUntil = timestamp }
        override suspend fun setNextEligibleInterventionAt(timestamp: Long) { nextEligibleAt = timestamp }
        override suspend fun setActiveInterventionSession(encodedSession: String?) {}

        // Unused in this test
        override val isHistoryBackfilled = flowOf(false)
        override val lastSyncedTimestamp = flowOf(0L)
        override val monitoredPackages = flowOf(emptySet<String>())
        override val aggressivenessLevel = flowOf(AggressivenessLevel.BALANCED)
        override val isFixedLimitEnabled = flowOf(false)
        override val fixedDailyLimitMinutes = flowOf(60)
        override val bedtime = flowOf<String?>(null)
        override val wakeTime = flowOf<String?>(null)
        override val currentDifficulty = flowOf(2)
        override val userNickname = flowOf("")
        override val username = flowOf("")
        override val profileImagePath = flowOf<String?>(null)
        override val isOnboardingCompleted = flowOf(false)
        override val isDailySummaryEnabled = flowOf(true)
        override val isWeeklyReflectionEnabled = flowOf(true)
        override val isLimitReminderEnabled = flowOf(true)
        override val isInterventionReminderEnabled = flowOf(true)

        override suspend fun setHistoryBackfilled(isBackfilled: Boolean) {}
        override suspend fun setLastSyncedTimestamp(timestamp: Long) {}
        override suspend fun toggleMonitoredPackage(packageName: String) {}
        override suspend fun setAggressivenessLevel(level: AggressivenessLevel) {}
        override suspend fun setFixedLimitEnabled(enabled: Boolean) {}
        override suspend fun setFixedDailyLimitMinutes(minutes: Int) {}
        override suspend fun setBedtime(time: String?) {}
        override suspend fun setWakeTime(time: String?) {}
        override suspend fun setCurrentDifficulty(difficulty: Int) {}
        override suspend fun setUserNickname(nickname: String) {}
        override suspend fun setUsername(username: String) {}
        override suspend fun setProfileImagePath(path: String?) {}
        override suspend fun setOnboardingCompleted(completed: Boolean) {}
        override suspend fun setDailySummaryEnabled(enabled: Boolean) {}
        override suspend fun setWeeklyReflectionEnabled(enabled: Boolean) {}
        override suspend fun setLimitReminderEnabled(enabled: Boolean) {}
        override suspend fun setInterventionReminderEnabled(enabled: Boolean) {}
    }
}
