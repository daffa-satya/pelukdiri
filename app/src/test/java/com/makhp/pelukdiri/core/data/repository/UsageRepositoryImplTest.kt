package com.makhp.pelukdiri.core.data.repository

import com.makhp.pelukdiri.core.database.dao.UsageDao
import com.makhp.pelukdiri.core.database.entity.DailySummaryEntity
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import com.makhp.pelukdiri.core.domain.time.TimeProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageRepositoryImplTest {
    private val today = LocalDate.of(2026, 8, 26)
    private val dao: UsageDao = mockk(relaxed = true)
    private val usageEventCollector: com.makhp.pelukdiri.collector.UsageEventCollector = mockk(relaxed = true)
    private val preferences: UserPreferencesRepository = mockk {
        every { monitoredPackages } returns flowOf(setOf("com.example.monitored"))
    }
    private val timeProvider: TimeProvider = mockk {
        every { today() } returns this@UsageRepositoryImplTest.today
    }
    private val repository = UsageRepositoryImpl(
        dao = dao,
        appUsageCollector = mockk(relaxed = true),
        usageEventCollector = usageEventCollector,
        userPreferencesRepository = preferences,
        timeProvider = timeProvider,
    )

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
