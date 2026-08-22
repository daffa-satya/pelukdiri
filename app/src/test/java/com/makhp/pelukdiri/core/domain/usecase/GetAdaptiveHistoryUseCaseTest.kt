package com.makhp.pelukdiri.core.domain.usecase

import com.makhp.pelukdiri.core.domain.model.AppUsage
import com.makhp.pelukdiri.core.domain.model.DailySummary
import com.makhp.pelukdiri.core.domain.model.HistoricalConfig
import com.makhp.pelukdiri.core.domain.repository.UsageRepository
import com.makhp.pelukdiri.core.domain.time.TimeProvider
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetAdaptiveHistoryUseCaseTest {
    private val today = LocalDate.of(2026, 8, 22)

    @Test
    fun `uses 21-day fallback and returns 14 most recent valid prior days`() = runBlocking {
        val repository = FakeUsageRepository().apply {
            history = (1L..21L).map { daysAgo ->
                summary(today.minusDays(daysAgo), minutes = daysAgo)
            } + summary(today, minutes = 999L)
            history = history.map {
                if (it.date == today.minusDays(2) || it.date == today.minusDays(4)) {
                    it.copy(monitoredUsageMillis = 0L)
                } else {
                    it
                }
            }
        }

        val result = GetAdaptiveHistoryUseCase(repository, FixedTimeProvider(today))()

        assertEquals(today.minusDays(21), repository.queriedStart)
        assertEquals(today.minusDays(1), repository.queriedEnd)
        assertEquals(14, result.size)
        assertEquals(
            listOf(1.0, 3.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0),
            result,
        )
    }

    @Test
    fun `global history policy uses 7 to 14 valid days with 21-day fallback`() {
        assertEquals(14, HistoricalConfig.HISTORY_SAMPLE_DAYS)
        assertEquals(14, HistoricalConfig.BACKFILL_DAYS)
        assertEquals(7, HistoricalConfig.MINIMUM_HISTORY_DAYS)
        assertEquals(21L, HistoricalConfig.CALENDAR_LOOKBACK_DAYS)
    }

    private fun summary(date: LocalDate, minutes: Long) = DailySummary(
        date = date,
        totalScreenTimeMillis = minutes * 60_000L,
        totalScreenOnMillis = minutes * 60_000L,
        monitoredUsageMillis = minutes * 60_000L,
        unlockCount = 0,
        mostUsedApp = null,
    )

    private class FixedTimeProvider(private val date: LocalDate) : TimeProvider {
        override fun nowMillis(): Long = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        override fun zoneId(): ZoneId = ZoneOffset.UTC
    }

    private class FakeUsageRepository : UsageRepository {
        var history: List<DailySummary> = emptyList()
        var queriedStart: LocalDate? = null
        var queriedEnd: LocalDate? = null

        override fun getDailyUsage(date: LocalDate) = flowOf(emptyList<AppUsage>())
        override fun getDailySummary(date: LocalDate) = flowOf<DailySummary?>(null)
        override fun getUsageHistory(startDate: LocalDate, endDate: LocalDate) =
            flowOf(history.filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) })
                .also {
                    queriedStart = startDate
                    queriedEnd = endDate
                }

        override suspend fun refreshUsageData() = Unit
        override suspend fun syncRecentEventsOnly() = Unit
        override suspend fun executeFullBackfill(daysHistory: Int, force: Boolean) = Unit
        override suspend fun updateAppScreenTime(
            packageName: String,
            date: LocalDate,
            newScreenTimeMillis: Long,
        ) = Unit
    }
}
