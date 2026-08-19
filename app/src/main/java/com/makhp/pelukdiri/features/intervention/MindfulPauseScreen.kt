package com.makhp.pelukdiri.features.intervention

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makhp.pelukdiri.core.domain.model.MathQuestion
import com.makhp.pelukdiri.core.domain.model.RiskAssessmentResult
import com.makhp.pelukdiri.ui.theme.PELUKDIRITheme

private val ScreenScrim = Color(0xB3000000)

@Composable
fun MindfulPauseScreen(
    state: InterventionUiState,
    onAnswerChanged: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onEmergencyClick: () -> Unit,
    onReset: () -> Unit,
    onStartSample: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenScrim)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (state) {
                    is InterventionUiState.Idle -> IdleContent(onStartSample)
                    is InterventionUiState.Loading -> LoadingContent()
                    is InterventionUiState.QuestionActive -> QuestionContent(state, onAnswerChanged, onSubmitAnswer, onEmergencyClick)
                    is InterventionUiState.MaxPenalized -> QuestionContent(
                        state = InterventionUiState.QuestionActive(
                            question = state.question,
                            assessment = state.assessment,
                            answerInput = state.answerInput,
                            remainingBypasses = state.remainingBypasses,
                            bypassDenied = state.bypassDenied
                        ),
                        onAnswerChanged = onAnswerChanged,
                        onSubmitAnswer = onSubmitAnswer,
                        onEmergencyClick = onEmergencyClick,
                        isMaxPenalized = true
                    )
                    is InterventionUiState.CorrectAnswer -> ResultContent(isSuccess = true, state = state, onReset = onReset)
                    is InterventionUiState.IncorrectAnswer -> ResultContent(isSuccess = false, state = state, onReset = onReset)
                }
            }
        }
    }
}

@Composable
private fun IdleContent(onStartSample: () -> Unit) {
    Text(
        text = "Siap untuk Jeda?",
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(24.dp))
    Button(
        onClick = onStartSample,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text("Mulai Tantangan", fontSize = 18.sp)
    }
}

@Composable
private fun LoadingContent() {
    Text(
        text = "Menyiapkan...",
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun QuestionContent(
    state: InterventionUiState.QuestionActive,
    onAnswerChanged: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onEmergencyClick: () -> Unit,
    isMaxPenalized: Boolean = false
) {
    val displayExpr = remember(state.question.expression) {
        state.question.expression.replace("*", "×").replace("/", "÷")
    }

    Text(
        text = "Ambil Jeda Sejenak",
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
    )

    Spacer(Modifier.height(12.dp))
    Box(Modifier.width(36.dp).height(2.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)))
    Spacer(Modifier.height(18.dp))

    if (isMaxPenalized) {
        Text(
            text = "Penalti Maksimum Diterapkan",
            color = MaterialTheme.colorScheme.error,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }

    Text(
        text = "Tingkat Kesulitan: ${state.assessment.level}",
        color = MaterialTheme.colorScheme.primary,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
    )

    Spacer(Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = displayExpr,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(10.dp))
        OutlinedTextField(
            value = state.answerInput,
            onValueChange = {},
            modifier = Modifier.width(80.dp),
            readOnly = true,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(10.dp),
        )
    }

    Spacer(Modifier.height(18.dp))
    NumericKeypad(
        onNumberClick = { num -> onAnswerChanged(state.answerInput + num) },
        onClearClick = { onAnswerChanged("") },
        onBackspaceClick = { 
            if (state.answerInput.isNotEmpty()) {
                onAnswerChanged(state.answerInput.dropLast(1))
            }
        },
    )

    Spacer(Modifier.height(16.dp))
    Text(
        text = "Sisa Darurat: ${state.remainingBypasses}",
        color = if (state.remainingBypasses > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
    )

    if (state.bypassDenied) {
        Text(
            text = "Kuota darurat telah habis hari ini.",
            color = MaterialTheme.colorScheme.error,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }

    Spacer(Modifier.height(20.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = onEmergencyClick,
            modifier = Modifier.weight(1f).height(56.dp),
            enabled = state.remainingBypasses > 0,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
                disabledContentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.38f)
            ),
        ) {
            Text("Darurat", fontSize = 18.sp)
        }

        Button(
            onClick = onSubmitAnswer,
            modifier = Modifier.weight(1f).height(56.dp),
            enabled = state.answerInput.isNotEmpty(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text("Kirim", fontSize = 18.sp)
        }
    }
}

@Composable
private fun ResultContent(
    isSuccess: Boolean,
    state: InterventionUiState,
    onReset: () -> Unit
) {
    val title = if (isSuccess) "Berhasil!" else "Belum Tepat"
    val color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    
    Text(
        text = title,
        color = color,
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
    )
    
    Spacer(Modifier.height(16.dp))
    
    when (state) {
        is InterventionUiState.CorrectAnswer -> {
            Text(
                text = "Waktu Respon: ${state.responseTimeMs}ms",
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Level: ${state.assessment.level}",
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        is InterventionUiState.IncorrectAnswer -> {
            Text(
                text = "Jawaban Anda: ${state.enteredAnswer}",
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Jawaban Benar: ${state.question.correctAnswer}",
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        else -> {}
    }
    
    Spacer(Modifier.height(24.dp))
    
    Button(
        onClick = onReset,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(if (isSuccess) "Selesai" else "Tutup", fontSize = 18.sp)
    }
}


@Composable
private fun NumericKeypad(
    onNumberClick: (Int) -> Unit,
    onClearClick: () -> Unit,
    onBackspaceClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9),
        ).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { number ->
                    KeypadButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onNumberClick(number) },
                    ) {
                        Text(number.toString(), fontSize = 24.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KeypadButton(Modifier.weight(1f), onClearClick) {
                Text("C", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            KeypadButton(Modifier.weight(1f), { onNumberClick(0) }) {
                Text("0", fontSize = 24.sp, fontWeight = FontWeight.Medium)
            }
            KeypadButton(Modifier.weight(1f), onBackspaceClick) {
                Text("⌫", fontSize = 28.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun KeypadButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.aspectRatio(1.85f),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
    ) {
        content()
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun MindfulPauseScreenPreview() {
    PELUKDIRITheme {
        MindfulPauseScreen(
            state = InterventionUiState.QuestionActive(
                question = MathQuestion("47 + 28 - 19", 56, 3),
                assessment = RiskAssessmentResult(0.6, 3, 0, 120),
                remainingBypasses = 5
            ),
            onAnswerChanged = {},
            onSubmitAnswer = {},
            onEmergencyClick = {},
            onReset = {},
            onStartSample = {}
        )
    }
}

@Preview(
    name = "Failure Light",
    showBackground = true,
    widthDp = 412,
    heightDp = 915,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Preview(
    name = "Failure Dark",
    showBackground = true,
    widthDp = 412,
    heightDp = 915,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun MindfulPauseFailurePreview() {
    PELUKDIRITheme {
        MindfulPauseScreen(
            state = InterventionUiState.IncorrectAnswer(
                question = MathQuestion("43 + 47", 90, 1),
                assessment = RiskAssessmentResult(0.07, 1, 0, 120),
                enteredAnswer = "91",
                responseTimeMs = 2_500,
                remainingBypasses = 5,
            ),
            onAnswerChanged = {},
            onSubmitAnswer = {},
            onEmergencyClick = {},
            onReset = {},
            onStartSample = {},
        )
    }
}
