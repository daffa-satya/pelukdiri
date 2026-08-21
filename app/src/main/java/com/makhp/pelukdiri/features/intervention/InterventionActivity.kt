package com.makhp.pelukdiri.features.intervention

import android.app.ActivityManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import javax.inject.Inject
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
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
        android.util.Log.d("InterventionActivity", ">>> onCreate reached in InterventionActivity")

        // The service normally acquires this before launch. Acquiring here as well makes the
        // activity the authoritative owner when restored by Android or opened by a debug path.
        lockManager.acquireLock()
        excludeCurrentTaskFromRecents()

        // Use standard background transparency
        window.setBackgroundDrawableResource(android.R.color.transparent)

        // Ensure activity shows over everything
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

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
                        onDismiss = { 
                            android.util.Log.d("InterventionActivity", ">>> Finishing activity")
                            finishAndRemoveTask() 
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        android.util.Log.d("InterventionActivity", ">>> onNewIntent reached")
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
        val launchFreq = intent.getDoubleExtra(EXTRA_LAUNCH_FREQ, 0.0)
        val ambientLux = intent.getFloatExtra(EXTRA_AMBIENT_LUX, 0f)
        val deviation = intent.getDoubleExtra(EXTRA_DEVIATION, 0.0)
        val difficultyControlSignal = intent.getDoubleExtra(EXTRA_DIFFICULTY_CONTROL_SIGNAL, 0.0)
        val difficulty = intent.getIntExtra(EXTRA_DIFFICULTY, 2)
        val challengeType = intent.getStringExtra(EXTRA_CHALLENGE_TYPE)
            ?.let { runCatching { InterventionChallengeType.valueOf(it) }.getOrNull() }
            ?: InterventionChallengeType.AUTO

        if (monitoredUsage >= 0.0) {
            viewModel.startIntervention(
                monitoredUsageMinutes = monitoredUsage,
                launchFrequency = launchFreq.toInt(),
                ambientLightLux = ambientLux,
                deviation = deviation,
                difficultyControlSignal = difficultyControlSignal,
                difficulty = difficulty,
                challengeType = challengeType,
            )
        } else {
            android.util.Log.w("InterventionActivity", ">>> No valid monitored usage data in intent. MonitoredUsage: $monitoredUsage")
        }
    }

    private fun restoreActiveInterventionOrFinish() {
        viewModel.restoreActiveIntervention { restored ->
            if (restored) {
                android.util.Log.d("InterventionActivity", ">>> Restored active intervention")
            } else {
                android.util.Log.w("InterventionActivity", ">>> Active intervention snapshot unavailable")
                lockManager.releaseLock()
                finishAndRemoveTask()
            }
        }
    }

    override fun onStop() {
        abandonTransientAudioFocus()
        super.onStop()
        android.util.Log.d(
            "InterventionActivity",
            ">>> onStop; finishing=$isFinishing changingConfigurations=$isChangingConfigurations"
        )
        // The lock belongs to the unanswered intervention, not to the visible Activity
        // lifecycle. It is released only by resetToIdle after an answer or bypass.
    }

    private fun requestTransientAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    audioFocusListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
                )
            }
        } catch (error: RuntimeException) {
            android.util.Log.w("InterventionActivity", ">>> Unable to pause active media", error)
        }
    }

    private fun abandonTransientAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let(audioManager::abandonAudioFocusRequest)
                audioFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(audioFocusListener)
            }
        } catch (error: RuntimeException) {
            android.util.Log.w("InterventionActivity", ">>> Unable to release media pause", error)
        }
    }

    override fun onDestroy() {
        android.util.Log.d(
            "InterventionActivity",
            ">>> onDestroy; finishing=$isFinishing changingConfigurations=$isChangingConfigurations"
        )
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "EXTRA_PACKAGE_NAME"
        const val EXTRA_MONITORED_USAGE = "EXTRA_MONITORED_USAGE"
        const val EXTRA_LAUNCH_FREQ = "EXTRA_LAUNCH_FREQ"
        const val EXTRA_AMBIENT_LUX = "EXTRA_AMBIENT_LUX"
        const val EXTRA_DEVIATION = "EXTRA_DEVIATION"
        const val EXTRA_DIFFICULTY_CONTROL_SIGNAL = "EXTRA_DIFFICULTY_CONTROL_SIGNAL"
        const val EXTRA_DIFFICULTY = "EXTRA_DIFFICULTY"
        const val EXTRA_CHALLENGE_TYPE = "EXTRA_CHALLENGE_TYPE"
        const val EXTRA_RESTORE_ACTIVE = "EXTRA_RESTORE_ACTIVE"
    }
}
