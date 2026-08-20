package com.makhp.pelukdiri.features.intervention

import com.makhp.pelukdiri.core.domain.InterventionLockManager
import com.makhp.pelukdiri.core.domain.model.MathQuestion
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
    val launchFrequency: Int,
    val ambientLightLux: Float,
    val deviation: Double,
    val difficultyControlSignal: Double,
    val difficulty: Int,
    val questionStartTimeMs: Long,
    val createdAtMs: Long,
    val expiresAtMs: Long
)

object ActiveInterventionCodec {
    private const val VERSION = "1"

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
            else -> null
        }
        val values = listOf(
            VERSION, state.javaClass.simpleName,
            snapshot.monitoredUsageMinutes.toString(), snapshot.launchFrequency.toString(),
            snapshot.ambientLightLux.toString(), snapshot.deviation.toString(),
            snapshot.difficultyControlSignal.toString(), snapshot.difficulty.toString(),
            snapshot.questionStartTimeMs.toString(), snapshot.createdAtMs.toString(), snapshot.expiresAtMs.toString(),
            question?.expression.orEmpty(), question?.correctAnswer?.toString().orEmpty(), question?.level?.toString().orEmpty(),
            assessment?.riskScore?.toString().orEmpty(), assessment?.level?.toString().orEmpty(),
            assessment?.penaltyMinutes?.toString().orEmpty(), assessment?.calculatedLimitMinutes?.toString().orEmpty(),
            when (state) {
                is InterventionUiState.QuestionActive -> state.answerInput
                is InterventionUiState.MaxPenalized -> state.answerInput
                is InterventionUiState.IncorrectAnswer -> state.enteredAnswer
                else -> ""
            },
            when (state) {
                is InterventionUiState.QuestionActive -> state.remainingBypasses
                is InterventionUiState.MaxPenalized -> state.remainingBypasses
                is InterventionUiState.IncorrectAnswer -> state.remainingBypasses
                else -> 0
            }.toString(),
            when (state) {
                is InterventionUiState.QuestionActive -> state.bypassDenied
                is InterventionUiState.MaxPenalized -> state.bypassDenied
                else -> false
            }.toString(),
            when (state) {
                is InterventionUiState.CorrectAnswer -> state.responseTimeMs
                is InterventionUiState.IncorrectAnswer -> state.responseTimeMs
                else -> 0L
            }.toString()
        )
        return values.joinToString(".") {
            Base64.getUrlEncoder().withoutPadding().encodeToString(it.toByteArray(StandardCharsets.UTF_8))
        }
    }

    fun decode(encoded: String): ActiveInterventionSnapshot? = runCatching {
        val values = encoded.split('.').map { String(Base64.getUrlDecoder().decode(it), StandardCharsets.UTF_8) }
        require(values.size == 22 && values[0] == VERSION)
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
            else -> error("Unknown intervention state")
        }
        ActiveInterventionSnapshot(
            state, values[2].toDouble(), values[3].toInt(), values[4].toFloat(), values[5].toDouble(),
            values[6].toDouble(), values[7].toInt(), values[8].toLong(), values[9].toLong(), values[10].toLong()
        )
    }.getOrNull()
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
