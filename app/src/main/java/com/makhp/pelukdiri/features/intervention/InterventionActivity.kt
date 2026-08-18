package com.makhp.pelukdiri.features.intervention

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.makhp.pelukdiri.ui.theme.PELUKDIRITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InterventionActivity : ComponentActivity() {
    
    private val viewModel: InterventionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("InterventionActivity", ">>> onCreate reached in InterventionActivity")

        // Use standard background transparency
        window.setBackgroundDrawableResource(android.R.color.transparent)

        // Ensure activity shows over everything
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        val screenTime = intent.getDoubleExtra("EXTRA_SCREEN_TIME", -1.0)
        val launchFreq = intent.getDoubleExtra("EXTRA_LAUNCH_FREQ", 0.0)
        val ambientLux = intent.getFloatExtra("EXTRA_AMBIENT_LUX", 0f)
        val baselineLimit = intent.getDoubleExtra("EXTRA_BASELINE_LIMIT", 60.0)

        if (screenTime >= 0.0) {
            viewModel.startIntervention(
                screenTimeMinutes = screenTime,
                launchFrequency = launchFreq.toInt(),
                ambientLightLux = ambientLux,
                baselineLimitMinutes = baselineLimit
            )
        } else {
            android.util.Log.w("InterventionActivity", ">>> No valid screen time data in intent")
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
}
