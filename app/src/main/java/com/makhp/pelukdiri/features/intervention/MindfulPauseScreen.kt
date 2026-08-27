package com.makhp.pelukdiri.features.intervention

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.makhp.pelukdiri.core.domain.model.MathQuestion
import com.makhp.pelukdiri.core.domain.model.RiskAssessmentResult
import com.makhp.pelukdiri.core.domain.model.PatternShape
import com.makhp.pelukdiri.core.domain.model.PatternQuestion
import com.makhp.pelukdiri.R
import com.makhp.pelukdiri.ui.theme.PELUKDIRITheme
import com.makhp.pelukdiri.ui.theme.Dimens
import com.makhp.pelukdiri.ui.theme.InterventionDimens
import com.makhp.pelukdiri.ui.theme.InterventionScrim
import com.makhp.pelukdiri.ui.theme.PatternAmber
import com.makhp.pelukdiri.ui.theme.PatternBlue
import com.makhp.pelukdiri.ui.theme.PatternGreen
import com.makhp.pelukdiri.ui.theme.PatternPink
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal fun formatResponseTimeSeconds(responseTimeMs: Long): String =
    String.format(Locale.ROOT, "%.1f", responseTimeMs / 1_000.0)

internal fun formatInterventionTimer(elapsedTimeMs: Long): String {
    val totalSeconds = elapsedTimeMs.coerceAtLeast(0L) / 1_000L
    return String.format(Locale.ROOT, "%02d.%02d", totalSeconds / 60L, totalSeconds % 60L)
}

@Composable
private fun DifficultyTimerRow(level: Int, elapsedTimeMs: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.intervention_difficulty_level, level),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(
                R.string.intervention_timer,
                formatInterventionTimer(elapsedTimeMs),
            ),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun MindfulPauseScreen(
    state: InterventionUiState,
    elapsedResponseTimeMs: Long = 0L,
    onAnswerChanged: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onEmergencyClick: () -> Unit,
    onReset: () -> Unit,
    onRetry: () -> Unit,
    onPatternSelected: (PatternShape) -> Unit,
    onReplayPattern: () -> Unit,
    onRetryIncorrect: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val colors = MaterialTheme.colorScheme
    val isMathState = state is InterventionUiState.QuestionActive ||
        state is InterventionUiState.MaxPenalized ||
        state is InterventionUiState.CorrectAnswer ||
        state is InterventionUiState.IncorrectAnswer
    val interventionColors = if (isMathState && !isSystemInDarkTheme()) {
        colors.copy(
            primary = colors.onSurface,
            onPrimary = colors.surface,
            primaryContainer = colors.outlineVariant,
            onPrimaryContainer = colors.onSurface,
            surfaceVariant = colors.background,
        )
    } else {
        colors
    }

    MaterialTheme(colorScheme = interventionColors) {
        BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(InterventionScrim)
            .padding(
                horizontal = InterventionDimens.screenHorizontalPadding,
                vertical = InterventionDimens.screenVerticalPadding,
            ),
        contentAlignment = Alignment.Center,
        ) {
            Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = InterventionDimens.cardMaxWidth)
                .heightIn(max = maxHeight),
            shape = RoundedCornerShape(InterventionDimens.cardCornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = InterventionDimens.cardElevation),
            ) {
                Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = InterventionDimens.cardHorizontalPadding,
                        vertical = InterventionDimens.cardVerticalPadding,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    when (state) {
                    is InterventionUiState.Idle -> LoadingContent()
                    is InterventionUiState.Loading -> LoadingContent()
                    is InterventionUiState.Error -> ErrorContent(state.operation, onRetry)
                    is InterventionUiState.QuestionActive -> if (isLandscape) {
                        LandscapeQuestionContent(state, elapsedResponseTimeMs, onAnswerChanged, onSubmitAnswer, onEmergencyClick)
                    } else {
                        QuestionContent(state, elapsedResponseTimeMs, onAnswerChanged, onSubmitAnswer, onEmergencyClick)
                    }
                    is InterventionUiState.PatternActive -> PatternContent(
                        state = state,
                        elapsedResponseTimeMs = elapsedResponseTimeMs,
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
                                questionState, elapsedResponseTimeMs, onAnswerChanged, onSubmitAnswer,
                                onEmergencyClick, isMaxPenalized = true
                            )
                        } else {
                            QuestionContent(
                                questionState, elapsedResponseTimeMs, onAnswerChanged, onSubmitAnswer,
                                onEmergencyClick, isMaxPenalized = true
                            )
                        }
                    }
                    is InterventionUiState.CorrectAnswer -> ResultContent(true, state, onReset)
                    is InterventionUiState.IncorrectAnswer -> ResultContent(false, state, onRetryIncorrect)
                    is InterventionUiState.PatternCorrectAnswer -> ResultContent(true, state, onReset)
                    is InterventionUiState.PatternIncorrectAnswer -> ResultContent(false, state, onRetryIncorrect)
                    }
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
    Spacer(Modifier.height(InterventionDimens.contentGap))
    Text(
        text = stringResource(
            when (operation) {
                FailedInterventionOperation.START -> R.string.intervention_error_start
                FailedInterventionOperation.RESTORE -> R.string.intervention_error_restore
                FailedInterventionOperation.SUBMIT_ANSWER -> R.string.intervention_error_submit
                FailedInterventionOperation.RETRY_CHALLENGE -> R.string.intervention_error_retry_challenge
                FailedInterventionOperation.EMERGENCY_BYPASS -> R.string.intervention_error_bypass
                FailedInterventionOperation.COMPLETE -> R.string.intervention_error_complete
            }
        ),
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(Dimens.spaceLarge))
    Button(
        onClick = onRetry,
        modifier = Modifier.fillMaxWidth().height(Dimens.buttonHeight),
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
    elapsedResponseTimeMs: Long,
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
        horizontalArrangement = Arrangement.spacedBy(InterventionDimens.sectionGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(0.9f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(InterventionDimens.compactGap),
        ) {
            Text(
                text = stringResource(R.string.intervention_mindful_pause_title),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            if (isMaxPenalized) {
                Text(
                    text = stringResource(R.string.intervention_max_penalty),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            DifficultyTimerRow(state.assessment.level, elapsedResponseTimeMs)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayExpr,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(InterventionDimens.compactGap))
                OutlinedTextField(
                    value = state.answerInput,
                    onValueChange = {},
                    modifier = Modifier.width(InterventionDimens.landscapeAnswerWidth),
                    readOnly = true,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    shape = RoundedCornerShape(InterventionDimens.compactGap),
                )
            }
            Text(
                text = stringResource(R.string.intervention_emergency_remaining, state.remainingBypasses),
                color = if (state.remainingBypasses > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (state.bypassDenied) {
                Text(
                    text = stringResource(R.string.intervention_bypass_exhausted),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(InterventionDimens.compactGap),
            ) {
                OutlinedButton(
                    onClick = onEmergencyClick,
                    modifier = Modifier.weight(1f).height(Dimens.minTouchTarget),
                    enabled = state.remainingBypasses > 0,
                    border = BorderStroke(InterventionDimens.thinBorder, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                        disabledContentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.38f),
                    ),
                ) { Text(stringResource(R.string.intervention_emergency_button_short)) }
                Button(
                    onClick = onSubmitAnswer,
                    modifier = Modifier.weight(1f).height(Dimens.minTouchTarget),
                    enabled = state.answerInput.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PatternGreen,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
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
    elapsedResponseTimeMs: Long,
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
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
    )

    Spacer(Modifier.height(InterventionDimens.contentGap))
    Box(
        Modifier
            .width(Dimens.iconSizeLarge)
            .height(Dimens.dividerThickness)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    )
    Spacer(Modifier.height(Dimens.iconSizeSmall))

    if (isMaxPenalized) {
        Text(
            text = stringResource(R.string.intervention_max_penalty),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = Dimens.spaceSmall)
        )
    }

    DifficultyTimerRow(state.assessment.level, elapsedResponseTimeMs)

    Spacer(Modifier.height(Dimens.spaceMedium))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = displayExpr,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(InterventionDimens.compactGap))
        OutlinedTextField(
            value = state.answerInput,
            onValueChange = {},
            modifier = Modifier.width(InterventionDimens.answerWidth),
            readOnly = true,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(InterventionDimens.compactGap),
        )
    }

    Spacer(Modifier.height(Dimens.iconSizeSmall))
    NumericKeypad(
        onNumberClick = { num -> onAnswerChanged(state.answerInput + num) },
        onClearClick = { onAnswerChanged("") },
        onBackspaceClick = { 
            if (state.answerInput.isNotEmpty()) {
                onAnswerChanged(state.answerInput.dropLast(1))
            }
        },
    )

    Spacer(Modifier.height(Dimens.spaceMedium))
    Text(
        text = stringResource(R.string.intervention_emergency_remaining, state.remainingBypasses),
        color = if (state.remainingBypasses > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )

    if (state.bypassDenied) {
        Text(
            text = stringResource(R.string.intervention_bypass_exhausted),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = Dimens.spaceExtraSmall)
        )
    }

    Spacer(Modifier.height(InterventionDimens.sectionGap))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(InterventionDimens.contentGap),
    ) {
        OutlinedButton(
            onClick = onEmergencyClick,
            modifier = Modifier.weight(1f).height(Dimens.buttonHeight),
            enabled = state.remainingBypasses > 0,
            shape = RoundedCornerShape(InterventionDimens.compactGap),
            border = BorderStroke(InterventionDimens.thinBorder, MaterialTheme.colorScheme.error),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
                disabledContentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.38f)
            ),
        ) {
            Text(
                stringResource(R.string.intervention_emergency_button_short),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Button(
            onClick = onSubmitAnswer,
            modifier = Modifier.weight(1f).height(Dimens.buttonHeight),
            enabled = state.answerInput.isNotEmpty(),
            shape = RoundedCornerShape(InterventionDimens.compactGap),
            colors = ButtonDefaults.buttonColors(
                containerColor = PatternGreen,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
        ) {
            Text(
                stringResource(R.string.intervention_submit_button_short),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun PatternContent(
    state: InterventionUiState.PatternActive,
    elapsedResponseTimeMs: Long,
    isLandscape: Boolean,
    onPatternSelected: (PatternShape) -> Unit,
    onReplayPattern: () -> Unit,
    onEmergencyClick: () -> Unit,
) {
    if (isLandscape) {
        LandscapePatternContent(state, elapsedResponseTimeMs, onPatternSelected, onReplayPattern, onEmergencyClick)
        return
    }
    val highlightedShape = state.playbackIndex?.let(state.question.sequence::getOrNull)
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
    Spacer(Modifier.height(Dimens.spaceSmall))
    Text(
        text = stringResource(R.string.intervention_pattern_hint),
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(Dimens.spaceSmall))
    DifficultyTimerRow(state.assessment.level, elapsedResponseTimeMs)
    Spacer(Modifier.height(Dimens.spaceLarge))

    PatternGrid(
        highlightedShape = highlightedShape,
        enabled = !state.isPlaying && !state.bypassDenied,
        onPatternSelected = onPatternSelected,
        modifier = Modifier.size(InterventionDimens.patternGridSize),
    )

    Spacer(Modifier.height(InterventionDimens.sectionGap))
    Row(
        modifier = Modifier.semantics { contentDescription = progressDescription },
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSmall),
    ) {
        state.question.sequence.indices.forEach { index ->
            val filled = index < state.answerInput.size
            val playing = index == state.playbackIndex
            Box(
                Modifier
                    .size(
                        if (playing) InterventionDimens.contentGap
                        else InterventionDimens.compactGap
                    )
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

    Spacer(Modifier.height(Dimens.spaceSmall))
    Spacer(Modifier.height(Dimens.spaceSmall))
    Text(
        text = stringResource(R.string.intervention_emergency_remaining, state.remainingBypasses),
        color = if (state.remainingBypasses > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )

    Spacer(Modifier.height(InterventionDimens.sectionGap))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(InterventionDimens.contentGap)
    ) {
        OutlinedButton(
            onClick = onEmergencyClick,
            modifier = Modifier.weight(1f).height(Dimens.buttonHeight),
            enabled = state.remainingBypasses > 0,
            shape = RoundedCornerShape(InterventionDimens.compactGap),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(InterventionDimens.thinBorder, MaterialTheme.colorScheme.error),
        ) {
            Text(
                stringResource(R.string.intervention_emergency_button_short),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        OutlinedButton(
            onClick = onReplayPattern,
            modifier = Modifier.weight(1f).height(Dimens.buttonHeight),
            enabled = !state.isPlaying,
            shape = RoundedCornerShape(InterventionDimens.compactGap),
        ) {
            Text(
                text = if (state.isPlaying) {
                    stringResource(R.string.intervention_playing_pattern)
                } else {
                    stringResource(R.string.intervention_replay_pattern)
                },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
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
    elapsedResponseTimeMs: Long,
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
        horizontalArrangement = Arrangement.spacedBy(InterventionDimens.sectionGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(0.9f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(InterventionDimens.compactGap),
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
            DifficultyTimerRow(state.assessment.level, elapsedResponseTimeMs)
            Row(
                modifier = Modifier.semantics { contentDescription = progressDescription },
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSmall),
            ) {
                state.question.sequence.indices.forEach { index ->
                    Box(
                        Modifier
                            .size(InterventionDimens.compactGap)
                            .background(
                                if (index < state.answerInput.size) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(50),
                            )
                    )
                }
            }
            Text(
                text = stringResource(R.string.intervention_emergency_remaining, state.remainingBypasses),
                color = if (state.remainingBypasses > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(InterventionDimens.compactGap)
            ) {
                OutlinedButton(
                    onClick = onEmergencyClick,
                    modifier = Modifier.weight(1f).height(Dimens.minTouchTarget),
                    enabled = state.remainingBypasses > 0,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(InterventionDimens.thinBorder, MaterialTheme.colorScheme.error),
                ) {
                    Text(
                        stringResource(R.string.intervention_emergency_button_short),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                OutlinedButton(
                    onClick = onReplayPattern,
                    modifier = Modifier.weight(1f).height(Dimens.minTouchTarget),
                    enabled = !state.isPlaying,
                ) {
                    Text(
                        if (state.isPlaying) stringResource(R.string.intervention_playing_pattern)
                        else stringResource(R.string.intervention_replay_pattern),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        PatternGrid(
            highlightedShape = state.playbackIndex?.let(state.question.sequence::getOrNull),
            enabled = !state.isPlaying && !state.bypassDenied,
            onPatternSelected = onPatternSelected,
            compactShapes = true,
            modifier = Modifier
                .weight(1.1f)
                .aspectRatio(1f)
                .padding(InterventionDimens.sectionGap),
        )
    }
}

@Composable
private fun PatternGrid(
    highlightedShape: PatternShape?,
    enabled: Boolean,
    onPatternSelected: (PatternShape) -> Unit,
    modifier: Modifier = Modifier,
    compactShapes: Boolean = false,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(InterventionDimens.contentGap)) {
        PatternShape.entries.chunked(2).forEach { shapes ->
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(InterventionDimens.contentGap),
            ) {
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
                        compact = compactShapes,
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
    compact: Boolean = false,
) {
    val color = when (shape) {
        PatternShape.CIRCLE -> PatternGreen
        PatternShape.SQUARE -> PatternBlue
        PatternShape.TRIANGLE -> PatternAmber
        PatternShape.PENTAGON -> PatternPink
    }
    val symbolSize = when (shape) {
        PatternShape.CIRCLE, PatternShape.SQUARE -> if (compact) {
            InterventionDimens.patternCircleCompact
        } else {
            InterventionDimens.patternCircle
        }
        PatternShape.TRIANGLE -> if (compact) {
            InterventionDimens.patternTriangleCompact
        } else {
            InterventionDimens.patternTriangle
        }
        PatternShape.PENTAGON -> if (compact) {
            InterventionDimens.patternPentagonCompact
        } else {
            InterventionDimens.patternPentagon
        }
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
                RoundedCornerShape(InterventionDimens.patternTileRadius),
            )
            .border(
                InterventionDimens.patternTileBorder,
                color.copy(alpha = if (highlighted) 1f else 0.55f),
                RoundedCornerShape(InterventionDimens.patternTileRadius),
            )
            .semantics {
                contentDescription = label
                role = Role.Button
            }
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
    val color = if (isSuccess) PatternGreen else MaterialTheme.colorScheme.error
    
    Text(
        text = title,
        color = color,
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
    )
    
    Spacer(Modifier.height(Dimens.spaceMedium))
    
    when (state) {
        is InterventionUiState.CorrectAnswer -> {
            Text(
                text = stringResource(R.string.intervention_response_time, formatResponseTimeSeconds(state.responseTimeMs)),
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
                text = stringResource(R.string.intervention_response_time, formatResponseTimeSeconds(state.responseTimeMs)),
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
    
    Spacer(Modifier.height(Dimens.spaceLarge))
    
    Button(
        onClick = onReset,
        modifier = Modifier.fillMaxWidth().height(Dimens.buttonHeight),
        shape = RoundedCornerShape(InterventionDimens.compactGap),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(
            if (isSuccess) stringResource(R.string.intervention_result_finish) else stringResource(R.string.intervention_result_retry),
            style = MaterialTheme.typography.titleMedium,
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
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.spaceSmall)) {
        listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9),
        ).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSmall),
            ) {
                row.forEach { number ->
                    KeypadButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onNumberClick(number) },
                        contentDescription = number.toString(),
                    ) {
                        Text(
                            number.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSmall),
        ) {
            KeypadButton(
                modifier = Modifier.weight(1f),
                onClick = onClearClick,
                contentDescription = stringResource(R.string.keypad_clear_description),
            ) {
                Text(
                    stringResource(R.string.keypad_clear),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            KeypadButton(
                modifier = Modifier.weight(1f),
                onClick = { onNumberClick(0) },
                contentDescription = stringResource(R.string.keypad_zero),
            ) {
                Text(
                    stringResource(R.string.keypad_zero),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
            KeypadButton(
                modifier = Modifier.weight(1f),
                onClick = onBackspaceClick,
                contentDescription = stringResource(R.string.keypad_backspace_description),
            ) {
                Text(
                    stringResource(R.string.keypad_backspace),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun KeypadButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    contentDescription: String,
    content: @Composable () -> Unit,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(InterventionDimens.compactGap),
        modifier = modifier
            .aspectRatio(1.85f)
            .semantics { this.contentDescription = contentDescription },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = InterventionDimens.keypadElevation),
    ) {
        content()
    }
}

@Preview(name = "Math Portrait", showBackground = true, widthDp = 412, heightDp = 915)
@Preview(name = "Math Landscape", showBackground = true, widthDp = 915, heightDp = 412)
@Preview(
    name = "Math Large Font",
    showBackground = true,
    widthDp = 412,
    heightDp = 915,
    fontScale = 1.5f,
)
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

@Preview(name = "Retry Error", showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun MindfulPauseErrorPreview() {
    PELUKDIRITheme {
        MindfulPauseScreen(
            state = InterventionUiState.Error(FailedInterventionOperation.START),
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

@Preview(name = "Loading", showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun MindfulPauseLoadingPreview() {
    PELUKDIRITheme {
        MindfulPauseScreen(
            state = InterventionUiState.Loading,
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
@Preview(
    name = "Pattern Landscape",
    showBackground = true,
    widthDp = 915,
    heightDp = 412,
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
