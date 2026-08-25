package com.makhp.pelukdiri.features.intervention

import android.app.ActivityManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import javax.inject.Inject
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.makhp.pelukdiri.ui.theme.PELUKDIRITheme
import com.makhp.pelukdiri.core.domain.engine.InterventionChallengeType
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InterventionActivity : ComponentActivity() {
    
    private val viewModel: InterventionViewModel by viewModels()

    @Inject
    lateinit var lockManager: com.makhp.pelukdiri.core.domain.InterventionLockManager

    private val audioManager by lazy { getSystemService(AudioManager::class.java) }
    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { }
    private var audioFocusRequest: AudioFocusRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The service normally acquires this before launch. Acquiring here as well makes the
        // activity the authoritative owner when restored by Android or opened by a debug path.
        lockManager.acquireLock()
        excludeCurrentTaskFromRecents()

        // Use standard background transparency
        window.setBackgroundDrawableResource(android.R.color.transparent)

        // Ensure activity shows over everything
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        // An intervention must be answered or explicitly bypassed. System Back must not
        // silently dismiss it and leave the already-committed cooldown in place.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })

        if (savedInstanceState == null) {
            processIntent(intent)
        } else if (viewModel.uiState.value == InterventionUiState.Idle) {
            // A retained ViewModel already owns the exact session during a normal
            // configuration change. Only restore from storage when Android also
            // had to recreate the process and therefore supplied a fresh ViewModel.
            restoreActiveInterventionOrFinish()
        }

        setContent {
            PELUKDIRITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent // Truly transparent, dim is handled by theme
                ) {
                    InterventionOverlayScreen(
                        viewModel = viewModel,
                        onDismiss = { finishAndRemoveTask() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        excludeCurrentTaskFromRecents()
        processIntent(intent)
    }

    override fun onPostResume() {
        super.onPostResume()
        excludeCurrentTaskFromRecents()
    }

    override fun onStart() {
        super.onStart()
        requestTransientAudioFocus()
    }

    override fun onPause() {
        // We removed the auto-finish logic here because it was causing the activity
        // to exit unexpectedly on some devices when system overlays or other
        // transient focus losses occurred. The active session is still preserved
        // by the service if the user manually leaves.
        super.onPause()
    }

    private fun excludeCurrentTaskFromRecents() {
        val activityManager = getSystemService(ActivityManager::class.java)
        activityManager.appTasks
            .firstOrNull { it.taskInfo?.taskId == taskId }
            ?.setExcludeFromRecents(true)
    }

    private fun processIntent(intent: android.content.Intent?) {
        if (intent == null) return

        if (intent.getBooleanExtra(EXTRA_RESTORE_ACTIVE, false)) {
            restoreActiveInterventionOrFinish()
            return
        }

        val monitoredUsage = intent.getDoubleExtra(EXTRA_MONITORED_USAGE, -1.0)
        val intervalMinutesAtLaunch = intent.getDoubleExtra(EXTRA_INTERVAL_MINUTES_AT_LAUNCH, 0.0)
        val ambientLightLuxAtLaunch = intent.getFloatExtra(EXTRA_AMBIENT_LIGHT_LUX_AT_LAUNCH, 0f)
        val deviation = intent.getDoubleExtra(EXTRA_DEVIATION, 0.0)
        val difficultyControlSignal = intent.getDoubleExtra(EXTRA_DIFFICULTY_CONTROL_SIGNAL, 0.0)
        val difficulty = intent.getIntExtra(EXTRA_DIFFICULTY, 2)
        val challengeType = intent.getStringExtra(EXTRA_CHALLENGE_TYPE)
            ?.let { runCatching { InterventionChallengeType.valueOf(it) }.getOrNull() }
            ?: InterventionChallengeType.AUTO

        if (monitoredUsage >= 0.0) {
            viewModel.startIntervention(
                monitoredUsageMinutes = monitoredUsage,
                intervalMinutesAtLaunch = intervalMinutesAtLaunch,
                ambientLightLuxAtLaunch = ambientLightLuxAtLaunch,
                deviation = deviation,
                difficultyControlSignal = difficultyControlSignal,
                difficulty = difficulty,
                challengeType = challengeType,
            )
        }
    }

    private fun restoreActiveInterventionOrFinish() {
        viewModel.restoreActiveIntervention { restored ->
            if (!restored) {
                lockManager.releaseLock()
                finishAndRemoveTask()
            }
        }
    }

    override fun onStop() {
        abandonTransientAudioFocus()
        super.onStop()
        // The lock belongs to the unanswered intervention, not to the visible Activity
        // lifecycle. It is released only by resetToIdle after an answer or bypass.
    }

    private fun requestTransientAudioFocus() {
        try {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setOnAudioFocusChangeListener(audioFocusListener)
                .setWillPauseWhenDucked(true)
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request)
        } catch (_: RuntimeException) {}
    }

    private fun abandonTransientAudioFocus() {
        try {
            audioFocusRequest?.let(audioManager::abandonAudioFocusRequest)
            audioFocusRequest = null
        } catch (_: RuntimeException) {}
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "EXTRA_PACKAGE_NAME"
        const val EXTRA_MONITORED_USAGE = "EXTRA_MONITORED_USAGE"
        // Keep the serialized keys stable so an in-flight intent from an older build remains readable.
        const val EXTRA_INTERVAL_MINUTES_AT_LAUNCH = "EXTRA_LAUNCH_FREQ"
        const val EXTRA_AMBIENT_LIGHT_LUX_AT_LAUNCH = "EXTRA_AMBIENT_LUX"
        const val EXTRA_DEVIATION = "EXTRA_DEVIATION"
        const val EXTRA_DIFFICULTY_CONTROL_SIGNAL = "EXTRA_DIFFICULTY_CONTROL_SIGNAL"
        const val EXTRA_DIFFICULTY = "EXTRA_DIFFICULTY"
        const val EXTRA_CHALLENGE_TYPE = "EXTRA_CHALLENGE_TYPE"
        const val EXTRA_RESTORE_ACTIVE = "EXTRA_RESTORE_ACTIVE"
    }
}
