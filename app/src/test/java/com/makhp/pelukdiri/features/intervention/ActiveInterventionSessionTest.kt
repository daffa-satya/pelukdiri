package com.makhp.pelukdiri.features.intervention

import com.makhp.pelukdiri.core.domain.InterventionLockManager
import com.makhp.pelukdiri.core.domain.model.MathQuestion
import com.makhp.pelukdiri.core.domain.model.PatternQuestion
import com.makhp.pelukdiri.core.domain.model.PatternShape
import com.makhp.pelukdiri.core.domain.model.RiskAssessmentResult
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import com.makhp.pelukdiri.core.domain.time.TimeProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveInterventionSessionTest {
    private class FakeTime(var now: Long) : TimeProvider {
        override fun nowMillis() = now
        override fun zoneId() = ZoneId.of("UTC")
    }

    private val snapshot = ActiveInterventionSnapshot(
        uiState = InterventionUiState.QuestionActive(
            MathQuestion("43 + 47", 90, 1),
            RiskAssessmentResult(0.12, 1, 0, 120),
            answerInput = "9",
            remainingBypasses = 4
        ),
        monitoredUsageMinutes = 81.5,
        intervalMinutesAtLaunch = 3.0,
        ambientLightLuxAtLaunch = 12f,
        deviation = 0.12,
        difficultyControlSignal = 0.3,
        difficulty = 1,
        questionStartTimeMs = 1_000L,
        createdAtMs = 1_000L,
        expiresAtMs = 1_000L + ActiveInterventionSession.TTL_MS
    )

    @Test fun `stale session timeout is two minutes`() {
        assertEquals(120_000L, ActiveInterventionSession.TTL_MS)
    }

    @Test fun `codec round trips exact question input and metadata`() {
        assertEquals(snapshot, ActiveInterventionCodec.decode(ActiveInterventionCodec.encode(snapshot)))
    }

    @Test fun `saved session acknowledges a pending launch`() = runTest {
        val preferences = mockk<UserPreferencesRepository>()
        every { preferences.activeInterventionSession } returns flowOf(null)
        coEvery { preferences.setActiveInterventionSession(any()) } returns Unit
        val session = ActiveInterventionSession(preferences, FakeTime(1_000L), InterventionLockManager())

        val acknowledgement = async { session.awaitLaunchAcknowledgement(3_000L) }
        runCurrent()
        assertFalse(acknowledgement.isCompleted)

        session.save(snapshot)
        assertTrue(acknowledgement.await())
    }

    @Test fun `missing session fails launch acknowledgement within timeout`() = runTest {
        val preferences = mockk<UserPreferencesRepository>()
        every { preferences.activeInterventionSession } returns flowOf(null)
        val session = ActiveInterventionSession(preferences, FakeTime(1_000L), InterventionLockManager())

        assertFalse(session.awaitLaunchAcknowledgement(3_000L))
    }

    @Test fun `codec round trips exact pattern playback and input`() {
        val patternSnapshot = snapshot.copy(
            uiState = InterventionUiState.PatternActive(
                question = PatternQuestion(
                    listOf(PatternShape.CIRCLE, PatternShape.PENTAGON, PatternShape.SQUARE),
                    level = 1,
                ),
                assessment = RiskAssessmentResult(0.12, 1, 0, 120),
                answerInput = listOf(PatternShape.CIRCLE),
                isPlaying = false,
                playbackIndex = null,
                replaysRemaining = 0,
                remainingBypasses = 4,
            )
        )
        assertEquals(
            patternSnapshot,
            ActiveInterventionCodec.decode(ActiveInterventionCodec.encode(patternSnapshot)),
        )
    }

    @Test fun `session remains valid immediately before ttl`() = runTest {
        val preferences = mockk<UserPreferencesRepository>()
        every { preferences.activeInterventionSession } returns flowOf(ActiveInterventionCodec.encode(snapshot))
        val session = ActiveInterventionSession(preferences, FakeTime(snapshot.expiresAtMs - 1), InterventionLockManager())
        assertEquals(snapshot, session.restore())
    }

    @Test fun `exact ttl expires and clears session cooldown and lock`() = runTest {
        val preferences = mockk<UserPreferencesRepository>()
        every { preferences.activeInterventionSession } returns flowOf(ActiveInterventionCodec.encode(snapshot))
        coEvery { preferences.setActiveInterventionSession(null) } returns Unit
        coEvery { preferences.setNextEligibleInterventionAt(0L) } returns Unit
        val lock = InterventionLockManager().also { it.acquireLock() }
        val session = ActiveInterventionSession(preferences, FakeTime(snapshot.expiresAtMs), lock)

        assertNull(session.restore())
        coVerify(exactly = 1) { preferences.setActiveInterventionSession(null) }
        coVerify(exactly = 1) { preferences.setNextEligibleInterventionAt(0L) }
        assertEquals(false, lock.isLocked.value)
    }

    @Test fun `after ttl also expires and clears session cooldown and lock`() = runTest {
        val preferences = mockk<UserPreferencesRepository>()
        every { preferences.activeInterventionSession } returns flowOf(ActiveInterventionCodec.encode(snapshot))
        coEvery { preferences.setActiveInterventionSession(null) } returns Unit
        coEvery { preferences.setNextEligibleInterventionAt(0L) } returns Unit
        val lock = InterventionLockManager().also { it.acquireLock() }
        val session = ActiveInterventionSession(preferences, FakeTime(snapshot.expiresAtMs + 1), lock)

        assertNull(session.restore())
        coVerify(exactly = 1) { preferences.setActiveInterventionSession(null) }
        coVerify(exactly = 1) { preferences.setNextEligibleInterventionAt(0L) }
        assertEquals(false, lock.isLocked.value)
    }
}
