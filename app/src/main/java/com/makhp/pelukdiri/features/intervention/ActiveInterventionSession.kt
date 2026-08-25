package com.makhp.pelukdiri.features.intervention

import com.makhp.pelukdiri.core.domain.InterventionLockManager
import com.makhp.pelukdiri.core.domain.model.MathQuestion
import com.makhp.pelukdiri.core.domain.model.PatternQuestion
import com.makhp.pelukdiri.core.domain.model.PatternShape
import com.makhp.pelukdiri.core.domain.model.RiskAssessmentResult
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import com.makhp.pelukdiri.core.domain.time.TimeProvider
import kotlinx.coroutines.flow.first
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

data class ActiveInterventionSnapshot(
    val uiState: InterventionUiState,
    val monitoredUsageMinutes: Double,
    val intervalMinutesAtLaunch: Double,
    val ambientLightLuxAtLaunch: Float,
    val deviation: Double,
    val difficultyControlSignal: Double,
    val difficulty: Int,
    val questionStartTimeMs: Long,
    val createdAtMs: Long,
    val expiresAtMs: Long
)

object ActiveInterventionCodec {
    private const val VERSION = "2"

    fun encode(snapshot: ActiveInterventionSnapshot): String {
        val state = snapshot.uiState
        val question = when (state) {
            is InterventionUiState.QuestionActive -> state.question
            is InterventionUiState.CorrectAnswer -> state.question
            is InterventionUiState.IncorrectAnswer -> state.question
            is InterventionUiState.MaxPenalized -> state.question
            else -> null
        }
        val assessment = when (state) {
            is InterventionUiState.QuestionActive -> state.assessment
            is InterventionUiState.CorrectAnswer -> state.assessment
            is InterventionUiState.IncorrectAnswer -> state.assessment
            is InterventionUiState.MaxPenalized -> state.assessment
            is InterventionUiState.PatternActive -> state.assessment
            is InterventionUiState.PatternCorrectAnswer -> state.assessment
            is InterventionUiState.PatternIncorrectAnswer -> state.assessment
            else -> null
        }
        val values = listOf(
            VERSION, state.javaClass.simpleName,
            snapshot.monitoredUsageMinutes.toString(), snapshot.intervalMinutesAtLaunch.toString(),
            snapshot.ambientLightLuxAtLaunch.toString(), snapshot.deviation.toString(),
            snapshot.difficultyControlSignal.toString(), snapshot.difficulty.toString(),
            snapshot.questionStartTimeMs.toString(), snapshot.createdAtMs.toString(), snapshot.expiresAtMs.toString(),
            question?.expression.orEmpty(), question?.correctAnswer?.toString().orEmpty(), question?.level?.toString().orEmpty(),
            assessment?.riskScore?.toString().orEmpty(), assessment?.level?.toString().orEmpty(),
            assessment?.penaltyMinutes?.toString().orEmpty(), assessment?.calculatedLimitMinutes?.toString().orEmpty(),
            when (state) {
                is InterventionUiState.QuestionActive -> state.answerInput
                is InterventionUiState.MaxPenalized -> state.answerInput
                is InterventionUiState.IncorrectAnswer -> state.enteredAnswer
                is InterventionUiState.PatternActive -> state.replaysRemaining.toString()
                else -> ""
            },
            when (state) {
                is InterventionUiState.QuestionActive -> state.remainingBypasses
                is InterventionUiState.MaxPenalized -> state.remainingBypasses
                is InterventionUiState.IncorrectAnswer -> state.remainingBypasses
                is InterventionUiState.PatternActive -> state.remainingBypasses
                is InterventionUiState.PatternIncorrectAnswer -> state.remainingBypasses
                else -> 0
            }.toString(),
            when (state) {
                is InterventionUiState.QuestionActive -> state.bypassDenied
                is InterventionUiState.MaxPenalized -> state.bypassDenied
                is InterventionUiState.PatternActive -> state.bypassDenied
                else -> false
            }.toString(),
            when (state) {
                is InterventionUiState.CorrectAnswer -> state.responseTimeMs
                is InterventionUiState.IncorrectAnswer -> state.responseTimeMs
                is InterventionUiState.PatternCorrectAnswer -> state.responseTimeMs
                is InterventionUiState.PatternIncorrectAnswer -> state.responseTimeMs
                else -> 0L
            }.toString(),
            when (state) {
                is InterventionUiState.PatternActive -> state.question.sequence
                is InterventionUiState.PatternCorrectAnswer -> state.question.sequence
                is InterventionUiState.PatternIncorrectAnswer -> state.question.sequence
                else -> emptyList()
            }.joinToString(",") { it.name },
            when (state) {
                is InterventionUiState.PatternActive -> state.answerInput
                is InterventionUiState.PatternIncorrectAnswer -> state.enteredSequence
                else -> emptyList()
            }.joinToString(",") { it.name },
            (state as? InterventionUiState.PatternActive)?.isPlaying?.toString().orEmpty(),
            (state as? InterventionUiState.PatternActive)?.playbackIndex?.toString().orEmpty(),
        )
        return values.joinToString(".") {
            Base64.getUrlEncoder().withoutPadding().encodeToString(it.toByteArray(StandardCharsets.UTF_8))
        }
    }

    fun decode(encoded: String): ActiveInterventionSnapshot? = runCatching {
        val values = encoded.split('.').map { String(Base64.getUrlDecoder().decode(it), StandardCharsets.UTF_8) }
        require((values.size == 22 && values[0] == "1") || (values.size == 26 && values[0] == VERSION))
        val question = if (values[11].isEmpty()) null else MathQuestion(values[11], values[12].toInt(), values[13].toInt())
        val assessment = if (values[14].isEmpty()) null else RiskAssessmentResult(
            values[14].toDouble(), values[15].toInt(), values[16].toInt(), values[17].toInt()
        )
        val state = when (values[1]) {
            "Loading" -> InterventionUiState.Loading
            "QuestionActive" -> InterventionUiState.QuestionActive(question!!, assessment!!, values[18], values[19].toInt(), values[20].toBoolean())
            "CorrectAnswer" -> InterventionUiState.CorrectAnswer(question!!, assessment!!, values[21].toLong())
            "IncorrectAnswer" -> InterventionUiState.IncorrectAnswer(question!!, assessment!!, values[18], values[21].toLong(), values[19].toInt())
            "MaxPenalized" -> InterventionUiState.MaxPenalized(question!!, assessment!!, values[18], values[19].toInt(), values[20].toBoolean())
            "PatternActive" -> InterventionUiState.PatternActive(
                question = PatternQuestion(decodeShapes(values[22]), values[7].toInt()),
                assessment = assessment!!,
                answerInput = decodeShapes(values[23]),
                isPlaying = values[24].toBoolean(),
                playbackIndex = values[25].toIntOrNull(),
                replaysRemaining = values[18].toIntOrNull() ?: 1,
                remainingBypasses = values[19].toInt(),
                bypassDenied = values[20].toBoolean(),
            )
            "PatternCorrectAnswer" -> InterventionUiState.PatternCorrectAnswer(
                PatternQuestion(decodeShapes(values[22]), values[7].toInt()), assessment!!, values[21].toLong()
            )
            "PatternIncorrectAnswer" -> InterventionUiState.PatternIncorrectAnswer(
                PatternQuestion(decodeShapes(values[22]), values[7].toInt()), assessment!!,
                decodeShapes(values[23]), values[21].toLong(), values[19].toInt()
            )
            else -> error("Unknown intervention state")
        }
        ActiveInterventionSnapshot(
            uiState = state,
            monitoredUsageMinutes = values[2].toDouble(),
            intervalMinutesAtLaunch = values[3].toDouble(),
            ambientLightLuxAtLaunch = values[4].toFloat(),
            deviation = values[5].toDouble(),
            difficultyControlSignal = values[6].toDouble(),
            difficulty = values[7].toInt(),
            questionStartTimeMs = values[8].toLong(),
            createdAtMs = values[9].toLong(),
            expiresAtMs = values[10].toLong(),
        )
    }.getOrNull()

    private fun decodeShapes(value: String): List<PatternShape> =
        value.takeIf(String::isNotEmpty)
            ?.split(',')
            ?.map { PatternShape.valueOf(it) }
            .orEmpty()
}

@Singleton
class ActiveInterventionSession @Inject constructor(
    private val preferences: UserPreferencesRepository,
    private val timeProvider: TimeProvider,
    private val lockManager: InterventionLockManager
) {
    @Volatile private var activeSnapshot: ActiveInterventionSnapshot? = null

    suspend fun restore(): ActiveInterventionSnapshot? {
        val snapshot = activeSnapshot ?: preferences.activeInterventionSession.first()?.let(ActiveInterventionCodec::decode)
        if (snapshot == null) return null
        if (timeProvider.nowMillis() >= snapshot.expiresAtMs) {
            clearExpired()
            return null
        }
        activeSnapshot = snapshot
        return snapshot
    }

    suspend fun save(snapshot: ActiveInterventionSnapshot) {
        activeSnapshot = snapshot
        preferences.setActiveInterventionSession(ActiveInterventionCodec.encode(snapshot))
    }

    suspend fun clear() {
        activeSnapshot = null
        preferences.setActiveInterventionSession(null)
    }

    suspend fun clearExpired() {
        clear()
        preferences.setNextEligibleInterventionAt(0L)
        lockManager.releaseLock()
    }

    companion object { const val TTL_MS = 20L * 60L * 1000L }
}
