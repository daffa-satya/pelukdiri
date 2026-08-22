package com.makhp.pelukdiri.core.data.repository

import com.makhp.pelukdiri.collector.AppUsageCollector
import com.makhp.pelukdiri.collector.UsageEventCollector
import com.makhp.pelukdiri.core.database.dao.UsageDao
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageRepositoryImplTest {
    private val dao: UsageDao = mockk(relaxed = true)
    private val preferences: UserPreferencesRepository = mockk {
        every { monitoredPackages } returns flowOf(setOf("com.example.monitored"))
    }
    private val repository = UsageRepositoryImpl(
        dao = dao,
        appUsageCollector = mockk(relaxed = true),
        usageEventCollector = mockk(relaxed = true),
        userPreferencesRepository = preferences,
    )

    @Test
    fun `valid per-app edit updates app row and summary transaction`() = runBlocking {
        val date = LocalDate.now().minusDays(1)

        repository.updateAppScreenTime("com.example.monitored", date, 90L * 60_000L)

        coVerify(exactly = 1) {
            dao.updateAppUsageAndSummary(
                date.toString(),
                "com.example.monitored",
                90L * 60_000L,
                setOf("com.example.monitored"),
            )
        }
    }

    @Test
    fun `future date is rejected without database mutation`() = runBlocking {
        val thrown = runCatching {
            repository.updateAppScreenTime("com.example", LocalDate.now().plusDays(1), 60_000L)
        }.exceptionOrNull()

        assertTrue(thrown is IllegalArgumentException)
        coVerify(exactly = 0) { dao.updateAppUsageAndSummary(any(), any(), any(), any()) }
    }

    @Test
    fun `duration over 24 hours is rejected without database mutation`() = runBlocking {
        val thrown = runCatching {
            repository.updateAppScreenTime(
                "com.example",
                LocalDate.now(),
                24L * 60L * 60L * 1000L + 1L,
            )
        }.exceptionOrNull()

        assertTrue(thrown is IllegalArgumentException)
        coVerify(exactly = 0) { dao.updateAppUsageAndSummary(any(), any(), any(), any()) }
    }
}
