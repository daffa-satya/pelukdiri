package com.makhp.pelukdiri

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.makhp.pelukdiri.core.domain.InterventionLockManager
import com.makhp.pelukdiri.core.domain.model.MathQuestion
import com.makhp.pelukdiri.core.domain.model.PatternQuestion
import com.makhp.pelukdiri.core.domain.model.PatternShape
import com.makhp.pelukdiri.core.domain.model.RiskAssessmentResult
import com.makhp.pelukdiri.debug.DebugRuntimeControls
import com.makhp.pelukdiri.debug.DebugTestEntryPoint
import com.makhp.pelukdiri.features.intervention.ActiveInterventionSession
import com.makhp.pelukdiri.features.intervention.ActiveInterventionSnapshot
import com.makhp.pelukdiri.features.intervention.InterventionActivity
import com.makhp.pelukdiri.features.intervention.InterventionUiState
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InterventionLifecycleInstrumentedTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val dependencies get() = EntryPointAccessors.fromApplication(context, DebugTestEntryPoint::class.java)

    @After fun cleanup() = runBlocking {
        dependencies.session().clear()
        dependencies.lock().releaseLock()
        dependencies.controls().useSystemTime()
    }

    @Test fun exactQuestionAndInputSurviveActivityRecreation() = runBlocking {
        val now = System.currentTimeMillis()
        dependencies.controls().useSystemTime()
        dependencies.session().save(
            ActiveInterventionSnapshot(
                InterventionUiState.QuestionActive(
                    MathQuestion("43 + 47", 90, 1),
                    RiskAssessmentResult(0.4, 1, 0, 120),
                    answerInput = "9",
                    remainingBypasses = 4
                ),
                90.0, 5, 25f, 0.4, 0.5, 1, now, now,
                now + ActiveInterventionSession.TTL_MS
            )
        )

        val scenario = ActivityScenario.launch<InterventionActivity>(
            Intent(context, InterventionActivity::class.java)
                .putExtra(InterventionActivity.EXTRA_RESTORE_ACTIVE, true)
        )
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("43 + 47")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText("43 + 47")).assertExists()
        compose.onAllNodes(hasText("9")).assertCountEquals(2)

        scenario.recreate()
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("43 + 47")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText("43 + 47")).assertExists()
        compose.onAllNodes(hasText("9")).assertCountEquals(2)
        scenario.close()
    }

    @Test fun interventionTaskIsExcludedAndDebugLabExistsOnlyInDebugManifest() {
        val packageManager = context.packageManager
        val interventionInfo = packageManager.getActivityInfo(
            ComponentName(context, InterventionActivity::class.java), 0
        )
        assertTrue(interventionInfo.flags and ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS != 0)
        assertNotNull(packageManager.getActivityInfo(ComponentName(context, "com.makhp.pelukdiri.debug.DebugTestLabActivity"), 0))
    }

    @Test fun exactPatternAndInputSurviveActivityRecreation() = runBlocking {
        val now = System.currentTimeMillis()
        dependencies.session().save(
            ActiveInterventionSnapshot(
                InterventionUiState.PatternActive(
                    question = PatternQuestion(
                        listOf(PatternShape.CIRCLE, PatternShape.PENTAGON, PatternShape.SQUARE),
                        1,
                    ),
                    assessment = RiskAssessmentResult(0.4, 1, 0, 120),
                    answerInput = listOf(PatternShape.CIRCLE),
                    isPlaying = false,
                    replaysRemaining = 0,
                    remainingBypasses = 4,
                ),
                90.0, 5, 25f, 0.4, 0.5, 1, now, now,
                now + ActiveInterventionSession.TTL_MS,
            )
        )
        val scenario = ActivityScenario.launch<InterventionActivity>(
            Intent(context, InterventionActivity::class.java)
                .putExtra(InterventionActivity.EXTRA_RESTORE_ACTIVE, true)
        )
        val progress = hasContentDescription("1 dari 3 bentuk dipilih")
        compose.waitUntil(5_000) { compose.onAllNodes(progress).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(progress).assertExists()

        scenario.recreate()
        compose.waitUntil(5_000) { compose.onAllNodes(progress).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(progress).assertExists()
        scenario.close()
    }

    @Test fun researchDataIsExcludedFromAndroidBackup() {
        val applicationInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
        assertEquals(0, applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
    }

    @Test fun debugClockOverrideSurvivesProviderRecreation() {
        val first = DebugRuntimeControls(context)
        val expected = System.currentTimeMillis() + 1_234_567L

        try {
            first.setTime(expected)
            val recreated = DebugRuntimeControls(context)
            assertEquals(expected, recreated.nowMillis())
        } finally {
            first.useSystemTime()
        }
    }
}
