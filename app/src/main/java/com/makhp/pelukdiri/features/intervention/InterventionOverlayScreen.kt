package com.makhp.pelukdiri.features.intervention

import android.content.res.Configuration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.makhp.pelukdiri.core.domain.model.MathQuestion
import com.makhp.pelukdiri.core.domain.model.RiskAssessmentResult
import com.makhp.pelukdiri.ui.theme.PELUKDIRITheme

@Composable
fun InterventionOverlayScreen(
    viewModel: InterventionViewModel = hiltViewModel(),
    onDismiss: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hapticFeedback = LocalHapticFeedback.current
    var showBypassDialog by remember { mutableStateOf(false) }
    var hasStarted by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState !is InterventionUiState.Idle) {
            hasStarted = true
        }

        if (uiState is InterventionUiState.IncorrectAnswer) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        
        // Only dismiss if we have already started the intervention flow and then returned to Idle
        if (hasStarted && uiState is InterventionUiState.Idle && !showBypassDialog) {
            onDismiss()
        }
    }

    if (showBypassDialog) {
        AlertDialog(
            onDismissRequest = { showBypassDialog = false },
            title = { Text("Konfirmasi Darurat") },
            text = { Text("Bypass akan memberikan akses selama 3 menit, namun tindakan ini akan mencatat penalti pada limit harian Anda. Lanjutkan?") },
            confirmButton = {
                TextButton(onClick = {
                    showBypassDialog = false
                    viewModel.emergencyBypass()
                }) {
                    Text("Lanjutkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBypassDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    val onAnswerChanged = remember(viewModel) { { input: String -> viewModel.onAnswerChanged(input) } }
    val onSubmitAnswer = remember(viewModel) { { viewModel.submitAnswer() } }
    val onBypassClick = remember { { showBypassDialog = true } }
    val onStartSample = remember(viewModel) {
        {
            viewModel.startIntervention(
                monitoredUsageMinutes = 95.0,
                launchFrequency = 14,
                ambientLightLux = 12.5f,
                deviation = 0.5,
                difficultyControlSignal = 0.6,
                difficulty = 3
            )
        }
    }
    val onReset = remember(viewModel, onDismiss) {
        {
            viewModel.resetToIdle()
            onDismiss()
        }
    }

    MindfulPauseScreen(
        state = uiState,
        onAnswerChanged = onAnswerChanged,
        onSubmitAnswer = onSubmitAnswer,
        onEmergencyClick = onBypassClick,
        onReset = onReset,
        onStartSample = onStartSample
    )
}

@Preview(name = "Intervention Light", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Intervention Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InterventionOverlayScreenPreview() {
    PELUKDIRITheme {
        MindfulPauseScreen(
            state = InterventionUiState.QuestionActive(
                question = MathQuestion(
                    expression = "(14 * 12) - (22 + 30)",
                    correctAnswer = 116,
                    level = 5
                ),
                assessment = RiskAssessmentResult(
                    riskScore = 2.15,
                    level = 5,
                    penaltyMinutes = 20,
                    calculatedLimitMinutes = 40
                ),
                answerInput = "116"
            ),
            onAnswerChanged = {},
            onSubmitAnswer = {},
            onEmergencyClick = {},
            onReset = {},
            onStartSample = {}
        )
    }
}
