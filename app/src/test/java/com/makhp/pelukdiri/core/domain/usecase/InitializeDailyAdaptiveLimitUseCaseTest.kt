package com.makhp.pelukdiri.core.domain.usecase

import com.makhp.pelukdiri.core.domain.engine.AdaptiveLimitGenerator
import com.makhp.pelukdiri.core.domain.engine.DeviationEngine
import com.makhp.pelukdiri.core.domain.model.*
import com.makhp.pelukdiri.core.domain.repository.AdaptiveLimitRepository
import com.makhp.pelukdiri.core.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InitializeDailyAdaptiveLimitUseCaseTest {

    private lateinit var usageRepository: FakeUsageRepository
    private lateinit var adaptiveLimitRepository: FakeAdaptiveLimitRepository
    private val deviationEngine = DeviationEngine(DeviationConfig())
    private val adaptiveLimitGenerator = AdaptiveLimitGenerator()
    private lateinit var getAdaptiveHistoryUseCase: GetAdaptiveHistoryUseCase
    private lateinit var useCase: InitializeDailyAdaptiveLimitUseCase

    @Before
    fun setup() {
        usageRepository = FakeUsageRepository()
        adaptiveLimitRepository = FakeAdaptiveLimitRepository()
        getAdaptiveHistoryUseCase = GetAdaptiveHistoryUseCase(usageRepository)
        useCase = InitializeDailyAdaptiveLimitUseCase(
            getAdaptiveHistoryUseCase,
            adaptiveLimitRepository,
            deviationEngine,
            adaptiveLimitGenerator
        )
    }

    @Test
    fun `6 valid observations within 14 days - returns InsufficientHistory and no persistence`() = runBlocking {
        usageRepository.history = List(6) { i ->
            DailySummary(LocalDate.now().minusDays(i + 1L), 60_000L * 60, 60_000L * 60, 10, null)
        }

        useCase.invoke()

        assertNull(adaptiveLimitRepository.getLimitForDate(LocalDate.now().toString()))
    }

    @Test
    fun `7 valid observations within 14 days - persists personalized limit equal to baseline`() = runBlocking {
        // Median of [10, 20, 30, 40, 50, 60, 70] is 40
        usageRepository.history = (1..7).map { i ->
            DailySummary(LocalDate.now().minusDays(i.toLong()), 60_000L * (i * 10), 0, 0, null)
        }

        useCase.invoke()

        val persisted = adaptiveLimitRepository.getLimitForDate(LocalDate.now().toString())
        assertNotNull(persisted)
        assertEquals(40, persisted!!.calculatedLimitMinutes)
    }

    @Test
    fun `10 valid observations within 14 days - only 7 most recent used`() = runBlocking {
        // Most recent 7: [10, 20, 30, 40, 50, 60, 70] -> Median 40
        // Older 3: [80, 90, 100] -> Should be ignored
        usageRepository.history = (1..10).map { i ->
            DailySummary(LocalDate.now().minusDays(i.toLong()), 60_000L * (i * 10), 0, 0, null)
        }

        useCase.invoke()

        val persisted = adaptiveLimitRepository.getLimitForDate(LocalDate.now().toString())
        assertEquals(40, persisted!!.calculatedLimitMinutes)
    }

    @Test
    fun `gaps in history - selects 7 most recent valid observations`() = runBlocking {
        // Days 1, 2, 3, 5, 6, 8, 10 are valid (7 total)
        // Days 4, 7, 9 are invalid (0 screen time)
        val validDays = listOf(1, 2, 3, 5, 6, 8, 10)
        val invalidDays = listOf(4, 7, 9)
        
        val history = mutableListOf<DailySummary>()
        validDays.forEach { i -> history.add(DailySummary(LocalDate.now().minusDays(i.toLong()), 60_000L * 100, 0, 0, null)) }
        invalidDays.forEach { i -> history.add(DailySummary(LocalDate.now().minusDays(i.toLong()), 0, 0, 0, null)) }
        
        usageRepository.history = history

        useCase.invoke()

        val persisted = adaptiveLimitRepository.getLimitForDate(LocalDate.now().toString())
        assertNotNull(persisted)
        assertEquals(100, persisted!!.calculatedLimitMinutes)
    }

    @Test
    fun `idempotency - does not overwrite existing row`() = runBlocking {
        val today = LocalDate.now().toString()
        adaptiveLimitRepository.limits[today] = DailyAdaptiveLimit(today, 120, 0, 0)
        
        // Setup sufficient history that would generate a different limit (40)
        usageRepository.history = (1..7).map { i ->
            DailySummary(LocalDate.now().minusDays(i.toLong()), 60_000L * (i * 10), 0, 0, null)
        }

        useCase.invoke()

        val persisted = adaptiveLimitRepository.getLimitForDate(today)
        assertEquals(120, persisted!!.calculatedLimitMinutes) // Preserved
    }

    @Test
    fun `today incomplete usage is NOT included in historical baseline`() = runBlocking {
        // If today was included, median of [10, 20, 30, 40, 50, 60, 500] would be 40
        // If today is NOT included, we need 7 PREVIOUS days.
        // Let's provide 6 previous days and today.
        val previousDays = (1..6).map { i ->
            DailySummary(LocalDate.now().minusDays(i.toLong()), 60_000L * (i * 10), 0, 0, null)
        }
        val todayUsage = DailySummary(LocalDate.now(), 60_000L * 500, 0, 0, null)
        
        usageRepository.history = previousDays + todayUsage

        useCase.invoke()

        // Should be insufficient because only 6 previous days are valid
        assertNull(adaptiveLimitRepository.getLimitForDate(LocalDate.now().toString()))
    }

    @Test
    fun `new local date queries a new row and does not reuse yesterday`() = runBlocking {
        val yesterday = LocalDate.now().minusDays(1).toString()
        val today = LocalDate.now().toString()
        
        // Yesterday's row exists
        adaptiveLimitRepository.limits[yesterday] = DailyAdaptiveLimit(yesterday, 120, 0, 0)
        
        // Setup sufficient history for today (median 40)
        usageRepository.history = (1..7).map { i ->
            DailySummary(LocalDate.now().minusDays(i.toLong()), 60_000L * (i * 10), 0, 0, null)
        }

        useCase.invoke()

        val yesterdayRow = adaptiveLimitRepository.getLimitForDate(yesterday)
        val todayRow = adaptiveLimitRepository.getLimitForDate(today)
        
        assertEquals(120, yesterdayRow!!.calculatedLimitMinutes)
        assertEquals(40, todayRow!!.calculatedLimitMinutes)
        assertNotEquals(yesterdayRow.dateString, todayRow.dateString)
    }

    // Fakes
    private class FakeUsageRepository : UsageRepository {
        var history: List<DailySummary> = emptyList()
        override fun getDailyUsage(date: LocalDate) = flowOf(emptyList<AppUsage>())
        override fun getDailySummary(date: LocalDate) = flowOf(null)
        override fun getUsageHistory(startDate: LocalDate, endDate: LocalDate) = flowOf(
            history.filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) }
        )
        override suspend fun refreshUsageData() {}
        override suspend fun syncRecentEventsOnly() {}
        override suspend fun executeFullBackfill(daysHistory: Int, force: Boolean) {}
    }

    private class FakeAdaptiveLimitRepository : AdaptiveLimitRepository {
        val limits = mutableMapOf<String, DailyAdaptiveLimit>()
        override suspend fun insertOrUpdateLimit(limit: DailyAdaptiveLimit) { limits[limit.dateString] = limit }
        override suspend fun insertInitialLimit(limit: DailyAdaptiveLimit) {
            if (!limits.containsKey(limit.dateString)) {
                limits[limit.dateString] = limit
            }
        }
        override suspend fun getLimitForDate(date: String) = limits[date]
        override fun getAllLimits() = flowOf(limits.values.toList())
        override suspend fun getAllLimitsList() = limits.values.toList()
    }
}
