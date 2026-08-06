package com.makhp.pelukdiri.features.intervention

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

    LaunchedEffect(uiState) {
        if (uiState is InterventionUiState.IncorrectAnswer) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    InterventionOverlayContent(
        state = uiState,
        onAnswerChanged = viewModel::onAnswerChanged,
        onSubmitAnswer = viewModel::submitAnswer,
        onStartSample = {
            viewModel.startIntervention(
                screenTimeMinutes = 95.0,
                launchFrequency = 14,
                ambientLightLux = 12.5f
            )
        },
        onReset = {
            viewModel.resetToIdle()
            onDismiss()
        }
    )
}

@Composable
fun InterventionOverlayContent(
    state: InterventionUiState,
    onAnswerChanged: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
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
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                when (state) {
                    InterventionUiState.Idle -> {
                        Text(
                            text = "Ready for adaptive intervention.",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
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
                            onSubmitAnswer = onSubmitAnswer
                        )
                    }

                    is InterventionUiState.MaxPenalized -> {
                        Text(
                            text = "Maximum penalty applied today.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ActiveQuestionContent(
                            state = InterventionUiState.QuestionActive(
                                question = state.question,
                                assessment = state.assessment,
                                answerInput = state.answerInput
                            ),
                            onAnswerChanged = onAnswerChanged,
                            onSubmitAnswer = onSubmitAnswer
                        )
                    }

                    is InterventionUiState.CorrectAnswer -> {
                        Text(
                            text = "Correct answer.",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Question: ${state.question.expression.toDisplayExpression()}")
                        Text("Time: ${state.responseTimeMs} ms")
                        Text("Level: ${state.assessment.level}")
                        Spacer(modifier = Modifier.height(16.dp))
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
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Question: ${state.question.expression.toDisplayExpression()}")
                        Text("Your answer: ${state.enteredAnswer}")
                        Text("Correct answer: ${state.question.correctAnswer}")
                        Spacer(modifier = Modifier.height(16.dp))
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
    onSubmitAnswer: () -> Unit
) {
    Text(
        text = "Solve this challenge",
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "${state.question.expression.toDisplayExpression()} = ?",
        style = MaterialTheme.typography.headlineSmall
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text("Risk score: ${"%.2f".format(state.assessment.riskScore)}")
    Text("Difficulty: Level ${state.assessment.level}")
    Text("Penalty: ${state.assessment.penaltyMinutes} minutes")
    Text("Calculated limit: ${state.assessment.calculatedLimitMinutes} minutes")
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = state.answerInput,
        onValueChange = onAnswerChanged,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Your answer") }
    )
    Spacer(modifier = Modifier.height(12.dp))
    Button(
        onClick = onSubmitAnswer,
        enabled = state.answerInput.isNotBlank()
    ) {
        Text("Submit")
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
            onStartSample = {},
            onReset = {}
        )
    }
}
