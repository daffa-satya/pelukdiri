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
    private val adaptiveLimitGenerator = AdaptiveLimitGenerator(AdaptiveLimitConfig())
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
    fun `6 valid observations within 21 days returns insufficient history`() = runBlocking {
        usageRepository.history = List(6) { i ->
            DailySummary(LocalDate.now().minusDays(i + 1L), 60_000L * 60, 60_000L * 60, 60_000L * 60, 10, null)
        }

        useCase.invoke()

        assertNull(adaptiveLimitRepository.getLimitForDate(LocalDate.now().toString()))
    }

    @Test
    fun `7 valid observations within 21 days persists personalized limit`() = runBlocking {
        // History: [10, 20, ..., 70], B = 40, MAD = 20, limit = 60.
        usageRepository.history = (1..7).map { i ->
            DailySummary(LocalDate.now().minusDays(i.toLong()), 60_000L * (i * 10), 0, 60_000L * (i * 10), 0, null)
        }

        useCase.invoke()

        val persisted = adaptiveLimitRepository.getLimitForDate(LocalDate.now().toString())
        assertNotNull(persisted)
        assertEquals(60, persisted!!.calculatedLimitMinutes)
    }

    @Test
    fun `10 valid observations uses all 10 rather than truncating to minimum`() = runBlocking {
        // History: [10, 20, ..., 100], B = 55, MAD = 25, limit = 80.
        usageRepository.history = (1..10).map { i ->
            DailySummary(LocalDate.now().minusDays(i.toLong()), 60_000L * (i * 10), 0, 60_000L * (i * 10), 0, null)
        }

        useCase.invoke()

        val persisted = adaptiveLimitRepository.getLimitForDate(LocalDate.now().toString())
        assertEquals(80, persisted!!.calculatedLimitMinutes)
    }

    @Test
    fun `17 valid observations within 21 days uses only 14 most recent`() = runBlocking {
        usageRepository.history = (1..17).map { i ->
            DailySummary(LocalDate.now().minusDays(i.toLong()), 60_000L * (i * 10), 0, 60_000L * (i * 10), 0, null)
        }

        useCase.invoke()

        val persisted = adaptiveLimitRepository.getLimitForDate(LocalDate.now().toString())
        assertEquals(110, persisted!!.calculatedLimitMinutes)
    }

    @Test
    fun `gaps in 21-day lookback select 14 most recent valid observations`() = runBlocking {
        val invalidDays = setOf(4, 7, 9, 15, 18, 20, 21)
        val validDays = (1..21).filterNot { it in invalidDays }
        
        val history = mutableListOf<DailySummary>()
        validDays.forEach { i -> history.add(DailySummary(LocalDate.now().minusDays(i.toLong()), 60_000L * 100, 0, 60_000L * 100, 0, null)) }
        invalidDays.forEach { i -> history.add(DailySummary(LocalDate.now().minusDays(i.toLong()), 0, 0, 0, 0, null)) }
        
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
        usageRepository.history = (1..14).map { i ->
            DailySummary(LocalDate.now().minusDays(i.toLong()), 60_000L * (i * 10), 0, 60_000L * (i * 10), 0, null)
        }

        useCase.invoke()

        val persisted = adaptiveLimitRepository.getLimitForDate(today)
        assertEquals(120, persisted!!.calculatedLimitMinutes) // Preserved
    }

    @Test
    fun `forced recalculation replaces limit and preserves accumulated values`() = runBlocking {
        val today = LocalDate.now().toString()
        adaptiveLimitRepository.limits[today] = DailyAdaptiveLimit(today, 120, 45, 12)
        usageRepository.history = (1..14).map { i ->
            DailySummary(LocalDate.now().minusDays(i.toLong()), 60_000L * (i * 10), 0, 60_000L * (i * 10), 0, null)
        }

        useCase.invoke(force = true)

        val persisted = adaptiveLimitRepository.getLimitForDate(today)!!
        assertEquals(110, persisted.calculatedLimitMinutes)
        assertEquals(45, persisted.actualScreenTimeMinutes)
        assertEquals(12, persisted.reclaimedTimeMinutes)
    }

    @Test
    fun `today incomplete usage is NOT included in historical baseline`() = runBlocking {
        // Today must not make six valid previous days appear sufficient.
        val previousDays = (1..6).map { i ->
            DailySummary(LocalDate.now().minusDays(i.toLong()), 60_000L * (i * 10), 0, 60_000L * (i * 10), 0, null)
        }
        val todayUsage = DailySummary(LocalDate.now(), 60_000L * 500, 0, 60_000L * 500, 0, null)
        
        usageRepository.history = previousDays + todayUsage

        useCase.invoke()

        // Insufficient because only 6 previous days are valid.
        assertNull(adaptiveLimitRepository.getLimitForDate(LocalDate.now().toString()))
    }

    @Test
    fun `new local date queries a new row and does not reuse yesterday`() = runBlocking {
        val yesterday = LocalDate.now().minusDays(1).toString()
        val today = LocalDate.now().toString()
        
        // Yesterday's row exists
        adaptiveLimitRepository.limits[yesterday] = DailyAdaptiveLimit(yesterday, 120, 0, 0)
        
        // Setup sufficient history for today (B = 75, MAD = 35 -> L = 110)
        usageRepository.history = (1..14).map { i ->
            DailySummary(LocalDate.now().minusDays(i.toLong()), 60_000L * (i * 10), 0, 60_000L * (i * 10), 0, null)
        }

        useCase.invoke()

        val yesterdayRow = adaptiveLimitRepository.getLimitForDate(yesterday)
        val todayRow = adaptiveLimitRepository.getLimitForDate(today)
        
        assertEquals(120, yesterdayRow!!.calculatedLimitMinutes)
        assertEquals(110, todayRow!!.calculatedLimitMinutes)
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
        override suspend fun updateAppScreenTime(
            packageName: String,
            date: LocalDate,
            newScreenTimeMillis: Long,
        ) {}
    }

    private class FakeAdaptiveLimitRepository : AdaptiveLimitRepository {
        val limits = mutableMapOf<String, DailyAdaptiveLimit>()
        override suspend fun insertOrUpdateLimit(limit: DailyAdaptiveLimit) { limits[limit.dateString] = limit }
        override suspend fun insertInitialLimit(limit: DailyAdaptiveLimit) {
            if (!limits.containsKey(limit.dateString)) {
                limits[limit.dateString] = limit
            }
        }
        override suspend fun updateCalculatedLimit(date: String, limitMinutes: Int) {
            limits[date] = limits.getValue(date).copy(calculatedLimitMinutes = limitMinutes)
        }
        override suspend fun getLimitForDate(date: String) = limits[date]
        override fun getAllLimits() = flowOf(limits.values.toList())
        override suspend fun getAllLimitsList() = limits.values.toList()
    }
}
