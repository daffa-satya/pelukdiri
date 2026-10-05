package com.makhp.pelukdiri.features.dashboard

import android.content.Context
import com.makhp.pelukdiri.R
import com.makhp.pelukdiri.collector.AppUsageCollector
import com.makhp.pelukdiri.collector.UsageEventCollector
import com.makhp.pelukdiri.core.database.export.CsvExporter
import com.makhp.pelukdiri.core.domain.repository.AdaptiveLimitRepository
import com.makhp.pelukdiri.core.domain.repository.InterventionDecisionRepository
import com.makhp.pelukdiri.core.domain.repository.UsageRepository
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import com.makhp.pelukdiri.core.domain.time.TimeProvider
import com.makhp.pelukdiri.core.domain.usecase.InitializeDailyAdaptiveLimitUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load failure exposes localized error and can be retried`() = runTest {
        val usageRepository: UsageRepository = mockk()
        coEvery { usageRepository.syncRecentEventsOnly() } throws IllegalStateException("database unavailable")
        val context: Context = mockk {
            every { getString(R.string.dashboard_load_failed) } returns "Dashboard tidak dapat dimuat."
        }
        val viewModel = DashboardViewModel(
            usageRepository = usageRepository,
            adaptiveLimitRepository = mockk(),
            appUsageCollector = mockk<AppUsageCollector>(),
            usageEventCollector = mockk<UsageEventCollector>(),
            interventionDecisionRepository = mockk<InterventionDecisionRepository>(),
            csvExporter = mockk<CsvExporter>(),
            userPreferencesRepository = mockk<UserPreferencesRepository>(),
            initializeDailyAdaptiveLimitUseCase = mockk<InitializeDailyAdaptiveLimitUseCase>(),
            timeProvider = mockk<TimeProvider>(),
            context = context,
        )

        val firstError = viewModel.uiState.filterIsInstance<DashboardUiState.Error>().first()
        assertEquals("Dashboard tidak dapat dimuat.", firstError.message)

        viewModel.loadData()
        viewModel.uiState.filterIsInstance<DashboardUiState.Error>().first()
        coVerify(exactly = 2) { usageRepository.syncRecentEventsOnly() }
    }
}
