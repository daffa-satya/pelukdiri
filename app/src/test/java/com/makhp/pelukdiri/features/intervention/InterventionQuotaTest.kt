package com.makhp.pelukdiri.features.intervention

import com.makhp.pelukdiri.core.domain.model.InterventionLog
import com.makhp.pelukdiri.core.domain.repository.InterventionLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class FakeInterventionLogRepository : InterventionLogRepository {
    val logs = mutableListOf<InterventionLog>()

    override suspend fun insertLog(log: InterventionLog) {
        logs.add(log)
    }

    override fun getAllLogs(): Flow<List<InterventionLog>> = flowOf(logs)
    override suspend fun getAllLogsList(): List<InterventionLog> = logs
    override suspend fun getRecentLogs(limit: Int): List<InterventionLog> = logs.asReversed().take(limit)
    override suspend fun getAverageResponseTime(difficulty: Int): Double? = null
    override suspend fun getRecentValidSuccessfulLogsByDifficulty(difficulty: Int, limit: Int): List<InterventionLog> = emptyList()
    override suspend fun getLatestValidPerformanceLogByDifficulty(difficulty: Int): InterventionLog? = null
    override suspend fun getLatestLog(): InterventionLog? = logs.lastOrNull()
    
    override suspend fun getBypassCountForDay(startOfDay: Long, endOfDay: Long): Int {
        return logs.count { it.isBypassed && it.timestamp >= startOfDay && it.timestamp < endOfDay }
    }
}

class InterventionQuotaTest {

    @Test
    fun `bypass count derivation - boundary tests`() = runBlocking {
        val repo = FakeInterventionLogRepository()
        val zone = ZoneId.systemDefault()
        val today = ZonedDateTime.now(zone).toLocalDate()
        val startOfDay = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        
        val yesterday = startOfDay - 1000
        val tomorrow = endOfDay + 1000
        
        // Logs from yesterday (should not count)
        repo.insertLog(InterventionLog(timestamp = yesterday, deviation = 0.0, difficultyControlSignal = 0.0, difficultyLevel = 1, responseTimeMs = 0, isSuccess = false, isBypassed = true, penaltyAppliedMinutes = 0))
        
        // Logs from tomorrow (should not count)
        repo.insertLog(InterventionLog(timestamp = tomorrow, deviation = 0.0, difficultyControlSignal = 0.0, difficultyLevel = 1, responseTimeMs = 0, isSuccess = false, isBypassed = true, penaltyAppliedMinutes = 0))
        
        // Today non-bypass log (should not count)
        repo.insertLog(InterventionLog(timestamp = startOfDay + 1000, deviation = 0.0, difficultyControlSignal = 0.0, difficultyLevel = 1, responseTimeMs = 0, isSuccess = true, isBypassed = false, penaltyAppliedMinutes = 0))
        
        assertEquals("Should have 0 bypasses for today initially", 0, repo.getBypassCountForDay(startOfDay, endOfDay))
        
        // 1st bypass today
        repo.insertLog(InterventionLog(timestamp = startOfDay + 2000, deviation = 0.0, difficultyControlSignal = 0.0, difficultyLevel = 1, responseTimeMs = 0, isSuccess = false, isBypassed = true, penaltyAppliedMinutes = 0))
        assertEquals("Should have 1 bypass for today", 1, repo.getBypassCountForDay(startOfDay, endOfDay))
        
        // Up to 5 bypasses today
        repeat(4) {
             repo.insertLog(InterventionLog(timestamp = startOfDay + 3000, deviation = 0.0, difficultyControlSignal = 0.0, difficultyLevel = 1, responseTimeMs = 0, isSuccess = false, isBypassed = true, penaltyAppliedMinutes = 0))
        }
        assertEquals("Should have 5 bypasses for today", 5, repo.getBypassCountForDay(startOfDay, endOfDay))
        
        // 6th bypass today
        repo.insertLog(InterventionLog(timestamp = startOfDay + 4000, deviation = 0.0, difficultyControlSignal = 0.0, difficultyLevel = 1, responseTimeMs = 0, isSuccess = false, isBypassed = true, penaltyAppliedMinutes = 0))

        assertEquals("Should have 6 bypasses for today in the log", 6, repo.getBypassCountForDay(startOfDay, endOfDay))
    }

    @Test
    fun `local day boundary calculation logic`() {
        val zone = ZoneId.of("UTC+7")
        val now = ZonedDateTime.of(2026, 8, 18, 15, 0, 0, 0, zone)
        
        val startOfDay = now.toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDay = now.toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        
        // Start of day should be 2026-08-18T00:00:00+07:00
        val expectedStart = ZonedDateTime.of(2026, 8, 18, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
        val expectedEnd = ZonedDateTime.of(2026, 8, 19, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
        
        assertEquals(expectedStart, startOfDay)
        assertEquals(expectedEnd, endOfDay)
        
        // Verify duration is exactly 24 hours
        assertEquals(24 * 60 * 60 * 1000L, endOfDay - startOfDay)
    }
}
