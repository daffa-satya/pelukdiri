package com.makhp.pelukdiri.debug

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.makhp.pelukdiri.R
import com.makhp.pelukdiri.core.domain.engine.ControlEngine
import com.makhp.pelukdiri.core.domain.engine.DeviationEngine
import com.makhp.pelukdiri.core.domain.engine.InterventionChallengeType
import com.makhp.pelukdiri.core.domain.model.InterventionLog
import com.makhp.pelukdiri.core.domain.model.PerformanceMetrics
import com.makhp.pelukdiri.core.domain.repository.InterventionLogRepository
import com.makhp.pelukdiri.core.domain.repository.InterventionDecisionRepository
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import com.makhp.pelukdiri.core.database.dao.UsageDao
import com.makhp.pelukdiri.core.database.entity.DailySummaryEntity
import com.makhp.pelukdiri.features.intervention.ActiveInterventionSession
import com.makhp.pelukdiri.features.intervention.InterventionActivity
import com.makhp.pelukdiri.ui.theme.PELUKDIRITheme
import com.makhp.pelukdiri.core.domain.InterventionLockManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

@AndroidEntryPoint
class DebugTestLabActivity : ComponentActivity() {
    @Inject lateinit var controls: DebugRuntimeControls
    @Inject lateinit var preferences: UserPreferencesRepository
    @Inject lateinit var logs: InterventionLogRepository
    @Inject lateinit var decisions: InterventionDecisionRepository
    @Inject lateinit var deviationEngine: DeviationEngine
    @Inject lateinit var controlEngine: ControlEngine
    @Inject lateinit var activeSession: ActiveInterventionSession
    @Inject lateinit var usageDao: UsageDao
    @Inject lateinit var lockManager: InterventionLockManager

    private var status by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.action == ACTION_LAUNCH_INTERVENTION) {
            val level = intent.getIntExtra(EXTRA_DIFFICULTY, 3)
            val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE) ?: DEFAULT_TARGET_PACKAGE
            packageManager.getLaunchIntentForPackage(targetPackage)?.let { targetIntent ->
                startActivity(targetIntent.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                })
            }
            Handler(Looper.getMainLooper()).postDelayed({
                launchIntervention(level, ::finishAndRemoveTask)
            }, TARGET_SETTLE_DELAY_MS)
            return
        }
        refresh()
        setContent {
            PELUKDIRITheme {
                DebugTestLabScreen(
                    status = status,
                    onRefresh = ::refresh,
                    onSystemTime = { controls.useSystemTime(); refresh() },
                    onBoundary = ::setBoundary,
                    onAdvanceTtl = { controls.advanceBy(ActiveInterventionSession.TTL_MS); refresh() },
                    onFailLaunch = { controls.forceNextLaunchFailure(); status = "Next accessibility-service launch will fail once." },
                    onClearTiming = { mutate { preferences.setNextEligibleInterventionAt(0L); preferences.setEmergencyBypassUntil(0L) } },
                    onClearSession = { mutate { activeSession.clear(); lockManager.releaseLock() } },
                    onSetDifficulty = { level -> mutate { preferences.setCurrentDifficulty(level) } },
                    onPerformance = ::insertPerformance,
                    onSeedHistory = ::seedHistory,
                    onScenario = ::runScenario,
                    onNormalIntervention = ::launchNormalIntervention,
                    onWatchedAppIntervention = ::armWatchedAppIntervention,
                    onLaunch = { level -> launchIntervention(level) },
                    onLaunchPattern = { level ->
                        launchIntervention(level, challengeType = InterventionChallengeType.PATTERN)
                    },
                )
            }
        }
    }

    private fun refresh() {
        lifecycleScope.launch { loadStatus() }
    }

    private suspend fun loadStatus() {
        val next = preferences.nextEligibleInterventionAt.first()
        val bypass = preferences.emergencyBypassUntil.first()
        val difficulty = preferences.currentDifficulty.first()
        val session = preferences.activeInterventionSession.first()
        val count = logs.getAllLogsList().size
        val decisionCount = decisions.getAllList().size
        status = "now=${controls.nowMillis()}\ndifficulty=$difficulty\nnextEligible=$next\nbypassUntil=$bypass\nactiveSession=${session != null}\narmedWatchedApp=${controls.pendingForcedChallenge()?.name ?: "none"}\ninterventionLogs=$count\ndecisionAudits=$decisionCount"
    }

    private fun setBoundary(offset: Long) = mutate {
        val target = preferences.nextEligibleInterventionAt.first()
        controls.setTime(target + offset)
    }

    private fun insertPerformance(success: Boolean) = mutate {
        val difficulty = preferences.currentDifficulty.first()
        logs.insertLog(
            InterventionLog(
                timestamp = controls.nowMillis(), deviation = 0.2, difficultyControlSignal = if (success) 0.8 else -0.8,
                difficultyLevel = difficulty, responseTimeMs = if (success) 1_500 else 18_000,
                isSuccess = success, penaltyAppliedMinutes = 0
            )
        )
    }

    private fun seedHistory() = mutate {
        val today = controls.today()
        (1L..7L).forEach { daysAgo ->
            usageDao.insertDailySummary(
                DailySummaryEntity(
                    date = today.minusDays(daysAgo).toString(),
                    totalScreenTimeMillis = 60L * 60L * 1000L,
                    totalScreenOnMillis = 60L * 60L * 1000L,
                    monitoredUsageMillis = 30L * 60L * 1000L,
                    unlockCount = 10,
                    mostUsedApp = "com.instagram.android",
                    wellbeingScore = null
                )
            )
        }
    }

    private fun runScenario(high: Boolean) {
        val history = listOf(42.0, 45.0, 44.0, 46.0, 43.0, 47.0, 45.0)
        val usage = if (high) 90.0 else 46.0
        val deviation = deviationEngine.calculate(usage, history)
        val result = controlEngine.calculateNextIntervention(
            deviation.deviation, PerformanceMetrics(if (high) 18_000 else 1_500, !high, 2),
            List(5) { if (high) 15_000L else 1_800L }, if (high) 1f else 150f,
            LocalTime.of(23, 0), LocalTime.of(7, 0), 2, LocalTime.of(23, 30), controls.nowMillis()
        )
        status = "usage=$usage history=$history\nbaseline=${deviation.baseline} MAD=${deviation.mad} D=${deviation.deviation}\nP=${result.performance} Q=${result.sensitivity} nextDifficulty=${result.nextDifficulty} interval=${result.intervalMinutes}m"
    }

    private fun launchIntervention(
        level: Int,
        afterLaunch: () -> Unit = {},
        challengeType: InterventionChallengeType = InterventionChallengeType.MATH,
    ) {
        lifecycleScope.launch {
            launchInterventionNow(level, challengeType)
            afterLaunch()
        }
    }

    private fun launchNormalIntervention(challengeType: InterventionChallengeType) = mutate {
        controls.useSystemTime()
        activeSession.clear()
        lockManager.releaseLock()
        preferences.setNextEligibleInterventionAt(0L)
        preferences.setEmergencyBypassUntil(0L)
        launchInterventionNow(preferences.currentDifficulty.first(), challengeType)
    }

    private fun armWatchedAppIntervention(challengeType: InterventionChallengeType) = mutate {
        controls.useSystemTime()
        activeSession.clear()
        lockManager.releaseLock()
        preferences.setNextEligibleInterventionAt(0L)
        preferences.setEmergencyBypassUntil(0L)
        controls.armForcedChallenge(challengeType)
    }

    private suspend fun launchInterventionNow(level: Int, challengeType: InterventionChallengeType) {
        val launchedAt = controls.nowMillis()
        preferences.setNextEligibleInterventionAt(
            launchedAt + TEST_LAUNCH_INTERVAL_MINUTES * 60_000L
        )
        preferences.setCurrentDifficulty(level)
        lockManager.acquireLock()
        startActivity(Intent(this@DebugTestLabActivity, InterventionActivity::class.java).apply {
            putExtra(InterventionActivity.EXTRA_MONITORED_USAGE, 90.0)
            putExtra(InterventionActivity.EXTRA_LAUNCH_FREQ, TEST_LAUNCH_INTERVAL_MINUTES.toDouble())
            putExtra(InterventionActivity.EXTRA_AMBIENT_LUX, 25f)
            putExtra(InterventionActivity.EXTRA_DEVIATION, 0.4)
            putExtra(InterventionActivity.EXTRA_DIFFICULTY_CONTROL_SIGNAL, 0.5)
            putExtra(InterventionActivity.EXTRA_DIFFICULTY, level)
            putExtra(InterventionActivity.EXTRA_CHALLENGE_TYPE, challengeType.name)
        })
    }

    private fun mutate(block: suspend () -> Unit) {
        lifecycleScope.launch { block(); loadStatus() }
    }

    companion object {
        const val ACTION_LAUNCH_INTERVENTION =
            "com.makhp.pelukdiri.debug.action.LAUNCH_INTERVENTION"
        const val EXTRA_DIFFICULTY = "difficulty"
        const val EXTRA_TARGET_PACKAGE = "target_package"
        private const val DEFAULT_TARGET_PACKAGE = "com.google.android.youtube"
        private const val TARGET_SETTLE_DELAY_MS = 750L
        private const val TEST_LAUNCH_INTERVAL_MINUTES = 5L
    }
}

@Composable
private fun DebugTestLabScreen(
    status: String,
    onRefresh: () -> Unit,
    onSystemTime: () -> Unit,
    onBoundary: (Long) -> Unit,
    onAdvanceTtl: () -> Unit,
    onFailLaunch: () -> Unit,
    onClearTiming: () -> Unit,
    onClearSession: () -> Unit,
    onSetDifficulty: (Int) -> Unit,
    onPerformance: (Boolean) -> Unit,
    onSeedHistory: () -> Unit,
    onScenario: (Boolean) -> Unit,
    onNormalIntervention: (InterventionChallengeType) -> Unit,
    onWatchedAppIntervention: (InterventionChallengeType) -> Unit,
    onLaunch: (Int) -> Unit,
    onLaunchPattern: (Int) -> Unit,
) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(stringResource(R.string.test_lab_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.test_lab_runtime), style = MaterialTheme.typography.titleMedium)
            Text(status, style = MaterialTheme.typography.bodySmall)
            Action(stringResource(R.string.test_lab_refresh), onRefresh)
            Text(stringResource(R.string.test_lab_engine), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Action(stringResource(R.string.test_lab_low_deviation), { onScenario(false) }, Modifier.weight(1f))
                Action(stringResource(R.string.test_lab_high_deviation), { onScenario(true) }, Modifier.weight(1f))
            }
            (1..3).forEach { level -> Action(stringResource(R.string.test_lab_difficulty, level), { onSetDifficulty(level) }) }
            Action(stringResource(R.string.test_lab_good_performance), { onPerformance(true) })
            Action(stringResource(R.string.test_lab_poor_performance), { onPerformance(false) })
            Action(stringResource(R.string.test_lab_seed_history), onSeedHistory)
            Text(stringResource(R.string.test_lab_boundaries), style = MaterialTheme.typography.titleMedium)
            Action(stringResource(R.string.test_lab_system_time), onSystemTime)
            Action(stringResource(R.string.test_lab_before), { onBoundary(-1) })
            Action(stringResource(R.string.test_lab_exact), { onBoundary(0) })
            Action(stringResource(R.string.test_lab_after), { onBoundary(1) })
            Action(stringResource(R.string.test_lab_advance_ttl), onAdvanceTtl)
            Action(stringResource(R.string.test_lab_fail_launch), onFailLaunch)
            Action(stringResource(R.string.test_lab_clear_timing), onClearTiming)
            Action(stringResource(R.string.test_lab_clear_session), onClearSession)
            Text(stringResource(R.string.test_lab_intervention), style = MaterialTheme.typography.titleMedium)
            Action(stringResource(R.string.test_lab_normal_math), {
                onNormalIntervention(InterventionChallengeType.MATH)
            })
            Action(stringResource(R.string.test_lab_normal_pattern), {
                onNormalIntervention(InterventionChallengeType.PATTERN)
            })
            Action(stringResource(R.string.test_lab_watched_app_math), {
                onWatchedAppIntervention(InterventionChallengeType.MATH)
            })
            Action(stringResource(R.string.test_lab_watched_app_pattern), {
                onWatchedAppIntervention(InterventionChallengeType.PATTERN)
            })
            (1..3).forEach { level -> Action(stringResource(R.string.test_lab_launch_level, level), { onLaunch(level) }) }
            (1..5).forEach { level ->
                Action(stringResource(R.string.test_lab_launch_pattern_level, level), { onLaunchPattern(level) })
            }
        }
    }
}

@Composable private fun Action(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth()) { Text(label) }
}

@Preview(showBackground = true) @Composable private fun LabLightPreview() {
    PELUKDIRITheme { DebugTestLabScreen("now=0", {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}) }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable private fun LabDarkPreview() {
    PELUKDIRITheme { DebugTestLabScreen("now=0", {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}) }
}
