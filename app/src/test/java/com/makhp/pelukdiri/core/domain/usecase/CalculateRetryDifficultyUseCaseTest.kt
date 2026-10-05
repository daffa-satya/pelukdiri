package com.makhp.pelukdiri.core.domain.usecase

import com.makhp.pelukdiri.core.domain.engine.DifficultyController
import com.makhp.pelukdiri.core.domain.engine.InterventionChallengeType
import com.makhp.pelukdiri.core.domain.model.ControlConfig
import com.makhp.pelukdiri.core.domain.model.InterventionLog
import com.makhp.pelukdiri.core.domain.repository.InterventionLogRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateRetryDifficultyUseCaseTest {
    private val repository = mockk<InterventionLogRepository>()
    private val config = ControlConfig.CANDIDATE_3
    private val useCase = CalculateRetryDifficultyUseCase(
        repository,
        DifficultyController(config),
        config,
    )

    @Test
    fun `ordinary decrease uses the existing two failure gate`() = runTest {
        coEvery { repository.getRecentLogs(any()) } returns listOf(failure(4))
        assertEquals(4, useCase(4, InterventionChallengeType.MATH, 0.8))

        coEvery { repository.getRecentLogs(any()) } returns listOf(failure(4), failure(4))
        assertEquals(3, useCase(4, InterventionChallengeType.MATH, 0.8))
    }

    @Test
    fun `level two uses the existing three failure recovery gate`() = runTest {
        coEvery { repository.getRecentLogs(any()) } returns listOf(failure(2), failure(2))
        assertEquals(2, useCase(2, InterventionChallengeType.MATH, 0.8))

        coEvery { repository.getRecentLogs(any()) } returns
            listOf(failure(2), failure(2), failure(2))
        assertEquals(1, useCase(2, InterventionChallengeType.MATH, 0.8))
    }

    @Test
    fun `failure evidence remains isolated by challenge type`() = runTest {
        coEvery { repository.getRecentLogs(any()) } returns listOf(
            failure(4, InterventionChallengeType.PATTERN),
            failure(4, InterventionChallengeType.MATH),
        )

        assertEquals(4, useCase(4, InterventionChallengeType.MATH, 0.8))
    }

    private fun failure(
        level: Int,
        type: InterventionChallengeType = InterventionChallengeType.MATH,
    ) = InterventionLog(
        timestamp = 1L,
        deviation = 0.8,
        difficultyControlSignal = 0.0,
        difficultyLevel = level,
        responseTimeMs = 1_000L,
        isSuccess = false,
        penaltyAppliedMinutes = 0,
        challengeType = type,
    )
}
