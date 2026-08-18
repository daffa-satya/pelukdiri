package com.makhp.pelukdiri.features.intervention

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.makhp.pelukdiri.core.domain.model.MathQuestion
import com.makhp.pelukdiri.core.domain.model.RiskAssessmentResult
import com.makhp.pelukdiri.ui.theme.Dimens
import com.makhp.pelukdiri.ui.theme.PELUKDIRITheme

@Composable
fun InterventionOverlayScreen(
    viewModel: InterventionViewModel = hiltViewModel(),
    onDismiss: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hapticFeedback = LocalHapticFeedback.current
    var showBypassDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is InterventionUiState.IncorrectAnswer) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        if (uiState is InterventionUiState.Idle && !showBypassDialog) {
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
                screenTimeMinutes = 95.0,
                launchFrequency = 14,
                ambientLightLux = 12.5f
            )
        }
    }
    val onReset = remember(viewModel, onDismiss) {
        {
            viewModel.resetToIdle()
            onDismiss()
        }
    }

    InterventionOverlayContent(
        state = uiState,
        onAnswerChanged = onAnswerChanged,
        onSubmitAnswer = onSubmitAnswer,
        onBypass = onBypassClick,
        onStartSample = onStartSample,
        onReset = onReset
    )
}

@Composable
fun InterventionOverlayContent(
    state: InterventionUiState,
    onAnswerChanged: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onBypass: () -> Unit,
    onStartSample: () -> Unit,
    onReset: () -> Unit
) {
    val cardColor = when (state) {
        is InterventionUiState.IncorrectAnswer -> MaterialTheme.colorScheme.errorContainer
        is InterventionUiState.CorrectAnswer -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.spaceMedium),
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            Column(modifier = Modifier.padding(Dimens.spaceMedium)) {
                when (state) {
                    InterventionUiState.Idle -> {
                        Text(
                            text = "Ready for adaptive intervention.",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(Dimens.spaceMedium))
                        Button(onClick = onStartSample) {
                            Text("Start Challenge")
                        }
                    }

                    InterventionUiState.Loading -> {
                        Text(
                            text = "Preparing your challenge...",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    is InterventionUiState.QuestionActive -> {
                        ActiveQuestionContent(
                            state = state,
                            onAnswerChanged = onAnswerChanged,
                            onSubmitAnswer = onSubmitAnswer,
                            onBypass = onBypass
                        )
                    }

                    is InterventionUiState.MaxPenalized -> {
                        Text(
                            text = "Maximum penalty applied today.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(Dimens.spaceSmall))
                        ActiveQuestionContent(
                            state = InterventionUiState.QuestionActive(
                                question = state.question,
                                assessment = state.assessment,
                                answerInput = state.answerInput,
                                remainingBypasses = state.remainingBypasses,
                                bypassDenied = state.bypassDenied
                            ),
                            onAnswerChanged = onAnswerChanged,
                            onSubmitAnswer = onSubmitAnswer,
                            onBypass = onBypass
                        )
                    }

                    is InterventionUiState.CorrectAnswer -> {
                        Text(
                            text = "Correct answer.",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(Dimens.spaceSmall))
                        Text("Question: ${state.question.expression.toDisplayExpression()}")
                        Text("Time: ${state.responseTimeMs} ms")
                        Text("Level: ${state.assessment.level}")
                        Spacer(modifier = Modifier.height(Dimens.spaceMedium))
                        Button(onClick = onReset) {
                            Text("Done")
                        }
                    }

                    is InterventionUiState.IncorrectAnswer -> {
                        Text(
                            text = "Incorrect answer.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(Dimens.spaceSmall))
                        Text("Question: ${state.question.expression.toDisplayExpression()}")
                        Text("Your answer: ${state.enteredAnswer}")
                        Text("Correct answer: ${state.question.correctAnswer}")
                        Spacer(modifier = Modifier.height(Dimens.spaceMedium))
                        Button(onClick = onReset) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveQuestionContent(
    state: InterventionUiState.QuestionActive,
    onAnswerChanged: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onBypass: () -> Unit
) {
    val displayExpr = remember(state.question.expression) {
        state.question.expression.toDisplayExpression()
    }
    val riskScoreTxt = remember(state.assessment.riskScore) {
        "Risk score: ${"%.2f".format(state.assessment.riskScore)}"
    }

    Text(
        text = "Solve this challenge",
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(modifier = Modifier.height(Dimens.spaceSmall))
    Text(
        text = "$displayExpr = ?",
        style = MaterialTheme.typography.headlineSmall
    )
    Spacer(modifier = Modifier.height(Dimens.spaceSmall + Dimens.spaceExtraSmall))
    Text(riskScoreTxt)
    Text("Difficulty: Level ${state.assessment.level}")
    Text("Penalty: ${state.assessment.penaltyMinutes} minutes")
    Text("Calculated limit: ${state.assessment.calculatedLimitMinutes} minutes")
    
    val bypassColor = if (state.remainingBypasses == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = "Sisa Darurat Hari Ini: ${state.remainingBypasses}",
        style = MaterialTheme.typography.bodySmall,
        color = bypassColor
    )
    
    if (state.bypassDenied) {
        Text(
            text = "Kuota darurat hari ini telah habis (Maks 5).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }

    Spacer(modifier = Modifier.height(Dimens.spaceSmall + Dimens.spaceExtraSmall))
    OutlinedTextField(
        value = state.answerInput,
        onValueChange = onAnswerChanged,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Your answer") }
    )
    Spacer(modifier = Modifier.height(Dimens.spaceSmall + Dimens.spaceExtraSmall))
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSmall)) {
        Button(
            onClick = onSubmitAnswer,
            enabled = state.answerInput.isNotBlank(),
            modifier = Modifier.weight(1f)
        ) {
            Text("Submit")
        }
        
        OutlinedButton(
            onClick = onBypass,
            modifier = Modifier.weight(1f),
            enabled = state.remainingBypasses > 0
        ) {
            Text("Darurat (3m)")
        }
    }
}

private fun String.toDisplayExpression(): String {
    return replace("*", "×").replace("/", "÷")
}

@Preview(name = "Intervention Light", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Intervention Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InterventionOverlayContentPreview() {
    PELUKDIRITheme {
        InterventionOverlayContent(
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
            onBypass = {},
            onStartSample = {},
            onReset = {}
        )
    }
}
