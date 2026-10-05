package com.makhp.pelukdiri.core.data.repository

import com.makhp.pelukdiri.core.database.dao.UsageDao
import com.makhp.pelukdiri.core.database.entity.DailySummaryEntity
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import com.makhp.pelukdiri.core.domain.time.TimeProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageRepositoryImplTest {
    private val today = LocalDate.of(2026, 8, 26)
    private val nowMillis = 1_787_690_123_456L
    private val dao: UsageDao = mockk(relaxed = true)
    private val appUsageCollector: com.makhp.pelukdiri.collector.AppUsageCollector = mockk(relaxed = true)
    private val usageEventCollector: com.makhp.pelukdiri.collector.UsageEventCollector = mockk(relaxed = true)
    private val preferences: UserPreferencesRepository = mockk {
        every { monitoredPackages } returns flowOf(setOf("com.example.monitored"))
        coEvery { setLastSyncedTimestamp(any()) } returns Unit
        coEvery { setHistoryBackfilled(any()) } returns Unit
    }
    private val timeProvider: TimeProvider = mockk {
        every { today() } returns this@UsageRepositoryImplTest.today
        every { nowMillis() } returns nowMillis
    }
    private val repository = UsageRepositoryImpl(
        dao = dao,
        appUsageCollector = appUsageCollector,
        usageEventCollector = usageEventCollector,
        userPreferencesRepository = preferences,
        timeProvider = timeProvider,
    )

    @Test
    fun `recent sync uses injected date and timestamp`() = runBlocking {
        every { appUsageCollector.isPermissionGranted() } returns true
        every { usageEventCollector.getUsageAndScreenOnForDay(today) } returns
            com.makhp.pelukdiri.collector.UsageEventCollector.DailyUsageAndScreenOn(emptyList(), 123L)
        every { dao.getDailySummary(today.toString()) } returns flowOf(null)

        repository.syncRecentEventsOnly()

        verify(exactly = 1) { usageEventCollector.getUsageAndScreenOnForDay(today) }
        coVerify(exactly = 1) { preferences.setLastSyncedTimestamp(nowMillis) }
    }

    @Test
    fun `backfill dates are anchored to injected today`() = runBlocking {
        var monitoredPackageCollections = 0
        every { preferences.monitoredPackages } returns flow {
            monitoredPackageCollections++
            emit(setOf("com.example.monitored"))
        }
        every { appUsageCollector.isPermissionGranted() } returns true
        repeat(2) { offset ->
            val date = today.minusDays(offset + 1L)
            every { usageEventCollector.getUsageAndScreenOnForDay(date) } returns
                com.makhp.pelukdiri.collector.UsageEventCollector.DailyUsageAndScreenOn(emptyList(), 123L)
            every { dao.getDailySummary(date.toString()) } returns flowOf(null)
        }

        repository.executeFullBackfill(daysHistory = 2, force = false)

        verify(exactly = 1) { usageEventCollector.getUsageAndScreenOnForDay(today.minusDays(1)) }
        verify(exactly = 1) { usageEventCollector.getUsageAndScreenOnForDay(today.minusDays(2)) }
        coVerify(exactly = 1) { preferences.setHistoryBackfilled(true) }
        assertEquals(1, monitoredPackageCollections)
    }

    @Test
    fun `valid per-app edit updates app row and summary transaction`() = runBlocking {
        val date = today.minusDays(1)
        coEvery { dao.getDailySummaryOnce(date.toString()) } returns DailySummaryEntity(
            date = date.toString(),
            totalScreenTimeMillis = 60L * 60_000L,
            totalScreenOnMillis = 120L * 60_000L,
            monitoredUsageMillis = 60L * 60_000L,
            unlockCount = 1,
            mostUsedApp = "Monitored App",
            wellbeingScore = null,
        )

        repository.updateAppScreenTime("com.example.monitored", "Monitored App", date, 90L * 60_000L)

        coVerify(exactly = 1) {
            dao.updateAppUsageAndSummary(
                date.toString(),
                "com.example.monitored",
                "Monitored App",
                90L * 60_000L,
                setOf("com.example.monitored"),
                120L * 60_000L,
            )
        }
    }

    @Test
    fun `missing summary reconstructs screen-on time for transaction`() = runBlocking {
        val date = today.minusDays(1)
        coEvery { dao.getDailySummaryOnce(date.toString()) } returns null
        every { usageEventCollector.getScreenOnMillisForDay(date) } returns 120L * 60_000L

        repository.updateAppScreenTime("com.example", "Example App", date, 60L * 60_000L)

        coVerify(exactly = 1) {
            dao.updateAppUsageAndSummary(
                date.toString(),
                "com.example",
                "Example App",
                60L * 60_000L,
                setOf("com.example.monitored"),
                120L * 60_000L,
            )
        }
    }

    @Test
    fun `future date is rejected without database mutation`() = runBlocking {
        val thrown = runCatching {
            repository.updateAppScreenTime("com.example", "Example App", today.plusDays(1), 60_000L)
        }.exceptionOrNull()

        assertTrue(thrown is IllegalArgumentException)
        coVerify(exactly = 0) { dao.updateAppUsageAndSummary(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `current date is rejected without database mutation`() = runBlocking {
        val thrown = runCatching {
            repository.updateAppScreenTime("com.example", "Example App", today, 60_000L)
        }.exceptionOrNull()

        assertTrue(thrown is IllegalArgumentException)
        coVerify(exactly = 0) { dao.updateAppUsageAndSummary(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `duration over 24 hours is rejected without database mutation`() = runBlocking {
        val thrown = runCatching {
            repository.updateAppScreenTime(
                "com.example",
                "Example App",
                today.minusDays(1),
                24L * 60L * 60L * 1000L + 1L,
            )
        }.exceptionOrNull()

        assertTrue(thrown is IllegalArgumentException)
        coVerify(exactly = 0) { dao.updateAppUsageAndSummary(any(), any(), any(), any(), any(), any()) }
    }
}
