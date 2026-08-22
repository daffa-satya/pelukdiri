package com.makhp.pelukdiri.features.intervention

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makhp.pelukdiri.core.domain.model.MathQuestion
import com.makhp.pelukdiri.core.domain.model.RiskAssessmentResult
import com.makhp.pelukdiri.core.domain.model.PatternShape
import com.makhp.pelukdiri.core.domain.model.PatternQuestion
import com.makhp.pelukdiri.R
import com.makhp.pelukdiri.ui.theme.PELUKDIRITheme
import com.makhp.pelukdiri.ui.theme.PatternAmber
import com.makhp.pelukdiri.ui.theme.PatternBlue
import com.makhp.pelukdiri.ui.theme.PatternGreen
import com.makhp.pelukdiri.ui.theme.PatternPink
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val ScreenScrim = Color(0xB3000000)

@Composable
fun MindfulPauseScreen(
    state: InterventionUiState,
    onAnswerChanged: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onEmergencyClick: () -> Unit,
    onReset: () -> Unit,
    onRetry: () -> Unit,
    onPatternSelected: (PatternShape) -> Unit,
    onReplayPattern: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (state) {
                    is InterventionUiState.Idle -> LoadingContent()
                    is InterventionUiState.Loading -> LoadingContent()
                    is InterventionUiState.Error -> ErrorContent(state.operation, onRetry)
                    is InterventionUiState.QuestionActive -> if (isLandscape) {
                        LandscapeQuestionContent(state, onAnswerChanged, onSubmitAnswer, onEmergencyClick)
                    } else {
                        QuestionContent(state, onAnswerChanged, onSubmitAnswer, onEmergencyClick)
                    }
                    is InterventionUiState.PatternActive -> PatternContent(
                        state = state,
                        isLandscape = isLandscape,
                        onPatternSelected = onPatternSelected,
                        onReplayPattern = onReplayPattern,
                        onEmergencyClick = onEmergencyClick,
                    )
                    is InterventionUiState.MaxPenalized -> {
                        val questionState = InterventionUiState.QuestionActive(
                            question = state.question,
                            assessment = state.assessment,
                            answerInput = state.answerInput,
                            remainingBypasses = state.remainingBypasses,
                            bypassDenied = state.bypassDenied
                        )
                        if (isLandscape) {
                            LandscapeQuestionContent(
                                questionState, onAnswerChanged, onSubmitAnswer,
                                onEmergencyClick, isMaxPenalized = true
                            )
                        } else {
                            QuestionContent(
                                questionState, onAnswerChanged, onSubmitAnswer,
                                onEmergencyClick, isMaxPenalized = true
                            )
                        }
                    }
                    is InterventionUiState.CorrectAnswer -> ResultContent(isSuccess = true, state = state, onReset = onReset)
                    is InterventionUiState.IncorrectAnswer -> ResultContent(isSuccess = false, state = state, onReset = onReset)
                    is InterventionUiState.PatternCorrectAnswer -> ResultContent(isSuccess = true, state = state, onReset = onReset)
                    is InterventionUiState.PatternIncorrectAnswer -> ResultContent(isSuccess = false, state = state, onReset = onReset)
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(
    operation: FailedInterventionOperation,
    onRetry: () -> Unit,
) {
    Text(
        text = stringResource(R.string.intervention_error_title),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(
            when (operation) {
                FailedInterventionOperation.START -> R.string.intervention_error_start
                FailedInterventionOperation.RESTORE -> R.string.intervention_error_restore
                FailedInterventionOperation.SUBMIT_ANSWER -> R.string.intervention_error_submit
                FailedInterventionOperation.EMERGENCY_BYPASS -> R.string.intervention_error_bypass
                FailedInterventionOperation.COMPLETE -> R.string.intervention_error_complete
            }
        ),
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
    Button(
        onClick = onRetry,
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        Text(stringResource(R.string.intervention_retry))
    }
}

@Composable
private fun LoadingContent() {
    CircularProgressIndicator()
}

@Composable
private fun LandscapeQuestionContent(
    state: InterventionUiState.QuestionActive,
    onAnswerChanged: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onEmergencyClick: () -> Unit,
    isMaxPenalized: Boolean = false,
) {
    val displayExpr = remember(state.question.expression) {
        state.question.expression.replace("*", "×").replace("/", "÷")
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(0.9f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.intervention_mindful_pause_title),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            if (isMaxPenalized) {
                Text(
                    text = stringResource(R.string.intervention_max_penalty),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = stringResource(R.string.intervention_difficulty_level, state.assessment.level),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayExpr,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(10.dp))
                OutlinedTextField(
                    value = state.answerInput,
                    onValueChange = {},
                    modifier = Modifier.width(86.dp),
                    readOnly = true,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    shape = RoundedCornerShape(10.dp),
                )
            }
            Text(
                text = stringResource(R.string.intervention_emergency_remaining, state.remainingBypasses),
                color = if (state.remainingBypasses > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (state.bypassDenied) {
                Text(
                    text = stringResource(R.string.intervention_bypass_exhausted),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onEmergencyClick,
                    modifier = Modifier.weight(1f).height(48.dp),
                    enabled = state.remainingBypasses > 0,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                        disabledContentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.38f),
                    ),
                ) { Text(stringResource(R.string.intervention_emergency_button_short)) }
                Button(
                    onClick = onSubmitAnswer,
                    modifier = Modifier.weight(1f).height(48.dp),
                    enabled = state.answerInput.isNotEmpty(),
                ) { Text(stringResource(R.string.intervention_submit_button_short)) }
            }
        }

        NumericKeypad(
            onNumberClick = { number -> onAnswerChanged(state.answerInput + number) },
            onClearClick = { onAnswerChanged("") },
            onBackspaceClick = {
                if (state.answerInput.isNotEmpty()) onAnswerChanged(state.answerInput.dropLast(1))
            },
            modifier = Modifier.weight(1.1f),
        )
    }
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
        text = stringResource(R.string.intervention_mindful_pause_title),
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
    )

    Spacer(Modifier.height(12.dp))
    Box(Modifier.width(36.dp).height(2.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)))
    Spacer(Modifier.height(18.dp))

    if (isMaxPenalized) {
        Text(
            text = stringResource(R.string.intervention_max_penalty),
            color = MaterialTheme.colorScheme.error,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }

    Text(
        text = stringResource(R.string.intervention_difficulty_level, state.assessment.level),
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
        text = stringResource(R.string.intervention_emergency_remaining, state.remainingBypasses),
        color = if (state.remainingBypasses > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
    )

    if (state.bypassDenied) {
        Text(
            text = stringResource(R.string.intervention_bypass_exhausted),
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
            Text(stringResource(R.string.intervention_emergency_button_short), fontSize = 18.sp)
        }

        Button(
            onClick = onSubmitAnswer,
            modifier = Modifier.weight(1f).height(56.dp),
            enabled = state.answerInput.isNotEmpty(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text(stringResource(R.string.intervention_submit_button_short), fontSize = 18.sp)
        }
    }
}

@Composable
private fun PatternContent(
    state: InterventionUiState.PatternActive,
    isLandscape: Boolean,
    onPatternSelected: (PatternShape) -> Unit,
    onReplayPattern: () -> Unit,
    onEmergencyClick: () -> Unit,
) {
    if (isLandscape) {
        LandscapePatternContent(state, onPatternSelected, onReplayPattern, onEmergencyClick)
        return
    }
    val highlightedShape = state.playbackIndex?.let(state.question.sequence::getOrNull)
    val gridSize = 310.dp
    val progressDescription = stringResource(
        R.string.intervention_pattern_progress,
        state.answerInput.size,
        state.question.sequence.size,
    )

    Text(
        text = stringResource(R.string.intervention_pattern_title),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.intervention_pattern_hint),
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(if (isLandscape) 10.dp else 24.dp))

    PatternGrid(
        highlightedShape = highlightedShape,
        enabled = !state.isPlaying && !state.bypassDenied,
        onPatternSelected = onPatternSelected,
        modifier = Modifier.size(gridSize),
    )

    Spacer(Modifier.height(if (isLandscape) 8.dp else 20.dp))
    Row(
        modifier = Modifier.semantics { contentDescription = progressDescription },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.question.sequence.indices.forEach { index ->
            val filled = index < state.answerInput.size
            val playing = index == state.playbackIndex
            Box(
                Modifier
                    .size(if (playing) 12.dp else 10.dp)
                    .background(
                        color = when {
                            playing -> MaterialTheme.colorScheme.primary
                            filled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            else -> MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = RoundedCornerShape(50),
                    )
            )
        }
    }

    Spacer(Modifier.height(if (isLandscape) 8.dp else 18.dp))
    OutlinedButton(
        onClick = onReplayPattern,
        enabled = !state.isPlaying,
    ) {
        Text(
            text = if (state.isPlaying) {
                stringResource(R.string.intervention_playing_pattern)
            } else {
                stringResource(R.string.intervention_replay_pattern)
            }
        )
    }

    Spacer(Modifier.height(if (isLandscape) 6.dp else 14.dp))
    OutlinedButton(
        onClick = onEmergencyClick,
        enabled = state.remainingBypasses > 0,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
    ) {
        Text(stringResource(R.string.intervention_emergency_button))
    }
    if (state.bypassDenied) {
        Text(
            text = stringResource(R.string.intervention_bypass_exhausted),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun LandscapePatternContent(
    state: InterventionUiState.PatternActive,
    onPatternSelected: (PatternShape) -> Unit,
    onReplayPattern: () -> Unit,
    onEmergencyClick: () -> Unit,
) {
    val progressDescription = stringResource(
        R.string.intervention_pattern_progress,
        state.answerInput.size,
        state.question.sequence.size,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(0.9f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.intervention_pattern_title),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.intervention_pattern_hint),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier.semantics { contentDescription = progressDescription },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                state.question.sequence.indices.forEach { index ->
                    Box(
                        Modifier
                            .size(9.dp)
                            .background(
                                if (index < state.answerInput.size) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(50),
                            )
                    )
                }
            }
            OutlinedButton(
                onClick = onReplayPattern,
                enabled = !state.isPlaying,
            ) {
                Text(
                    if (state.isPlaying) stringResource(R.string.intervention_playing_pattern)
                    else stringResource(R.string.intervention_replay_pattern)
                )
            }
            OutlinedButton(
                onClick = onEmergencyClick,
                enabled = state.remainingBypasses > 0,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(R.string.intervention_emergency_button))
            }
        }
        PatternGrid(
            highlightedShape = state.playbackIndex?.let(state.question.sequence::getOrNull),
            enabled = !state.isPlaying && !state.bypassDenied,
            onPatternSelected = onPatternSelected,
            modifier = Modifier.weight(1.1f).aspectRatio(1f),
        )
    }
}

@Composable
private fun PatternGrid(
    highlightedShape: PatternShape?,
    enabled: Boolean,
    onPatternSelected: (PatternShape) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PatternShape.entries.chunked(2).forEach { shapes ->
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                shapes.forEach { shape ->
                    PatternTile(
                        shape = shape,
                        label = stringResource(
                            when (shape) {
                                PatternShape.CIRCLE -> R.string.intervention_shape_circle
                                PatternShape.SQUARE -> R.string.intervention_shape_square
                                PatternShape.TRIANGLE -> R.string.intervention_shape_triangle
                                PatternShape.PENTAGON -> R.string.intervention_shape_pentagon
                            }
                        ),
                        highlighted = highlightedShape == shape,
                        enabled = enabled,
                        onClick = { onPatternSelected(shape) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PatternTile(
    shape: PatternShape,
    label: String,
    highlighted: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = when (shape) {
        PatternShape.CIRCLE -> PatternGreen
        PatternShape.SQUARE -> PatternBlue
        PatternShape.TRIANGLE -> PatternAmber
        PatternShape.PENTAGON -> PatternPink
    }
    val symbolSize = when (shape) {
        PatternShape.CIRCLE, PatternShape.SQUARE -> 52.dp
        PatternShape.TRIANGLE -> 62.dp
        PatternShape.PENTAGON -> 60.dp
    }
    val scale by animateFloatAsState(if (highlighted) 1.08f else 1f, label = "patternTileScale")
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled || highlighted) 1f else 0.72f
            }
            .background(
                if (highlighted) color.copy(alpha = 0.22f) else Color.Transparent,
                RoundedCornerShape(22.dp),
            )
            .border(3.dp, color.copy(alpha = if (highlighted) 1f else 0.55f), RoundedCornerShape(22.dp))
            .semantics { contentDescription = label }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(symbolSize)) {
            when (shape) {
                PatternShape.CIRCLE -> drawCircle(color)
                PatternShape.SQUARE -> drawRect(color)
                PatternShape.TRIANGLE -> {
                    val path = Path().apply {
                        moveTo(size.width / 2f, 0f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(path, color)
                }
                PatternShape.PENTAGON -> {
                    val radius = size.minDimension / 2f
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val path = Path()
                    repeat(5) { index ->
                        val angle = -PI / 2 + index * 2 * PI / 5
                        val x = centerX + radius * cos(angle).toFloat()
                        val y = centerY + radius * sin(angle).toFloat()
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                    drawPath(path, color)
                }
            }
        }
    }
}

@Composable
private fun ResultContent(
    isSuccess: Boolean,
    state: InterventionUiState,
    onReset: () -> Unit
) {
    val title = if (isSuccess) stringResource(R.string.intervention_result_success) else stringResource(R.string.intervention_result_failed)
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
                text = stringResource(R.string.intervention_response_time, state.responseTimeMs),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.intervention_difficulty_level, state.assessment.level),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        is InterventionUiState.IncorrectAnswer -> {
            Text(
                text = stringResource(R.string.intervention_result_user_answer, state.enteredAnswer),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.intervention_result_correct_answer, state.question.correctAnswer),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        is InterventionUiState.PatternCorrectAnswer -> {
            Text(
                text = stringResource(R.string.intervention_pattern_correct),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.intervention_response_time, state.responseTimeMs),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        is InterventionUiState.PatternIncorrectAnswer -> {
            Text(
                text = stringResource(R.string.intervention_pattern_incorrect),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
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
        Text(
            if (isSuccess) stringResource(R.string.intervention_result_finish) else stringResource(R.string.intervention_result_close),
            fontSize = 18.sp
        )
    }
}


@Composable
private fun NumericKeypad(
    onNumberClick: (Int) -> Unit,
    onClearClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Text(stringResource(R.string.keypad_clear), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            KeypadButton(Modifier.weight(1f), { onNumberClick(0) }) {
                Text(stringResource(R.string.keypad_zero), fontSize = 24.sp, fontWeight = FontWeight.Medium)
            }
            KeypadButton(Modifier.weight(1f), onBackspaceClick) {
                Text(stringResource(R.string.keypad_backspace), fontSize = 28.sp, fontWeight = FontWeight.Medium)
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
            onRetry = {},
            onPatternSelected = {},
            onReplayPattern = {},
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
            onRetry = {},
            onPatternSelected = {},
            onReplayPattern = {},
        )
    }
}

@Preview(
    name = "Pattern Light",
    showBackground = true,
    widthDp = 412,
    heightDp = 915,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Preview(
    name = "Pattern Dark",
    showBackground = true,
    widthDp = 412,
    heightDp = 915,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun MindfulPausePatternPreview() {
    PELUKDIRITheme {
        MindfulPauseScreen(
            state = InterventionUiState.PatternActive(
                question = PatternQuestion(
                    sequence = listOf(
                        PatternShape.CIRCLE,
                        PatternShape.SQUARE,
                        PatternShape.TRIANGLE,
                        PatternShape.PENTAGON,
                        PatternShape.CIRCLE,
                        PatternShape.TRIANGLE,
                        PatternShape.SQUARE,
                    ),
                    level = 5,
                ),
                assessment = RiskAssessmentResult(0.7, 5, 0, 120),
                isPlaying = false,
                remainingBypasses = 5,
            ),
            onAnswerChanged = {},
            onSubmitAnswer = {},
            onEmergencyClick = {},
            onReset = {},
            onRetry = {},
            onPatternSelected = {},
            onReplayPattern = {},
        )
    }
}
