package com.makhp.pelukdiri.core.domain.model

import com.makhp.pelukdiri.core.domain.engine.InterventionChallengeType

enum class InterventionDecisionReason {
    ACTIVE_LOCK,
    PACKAGE_NOT_MONITORED,
    COOLDOWN_ACTIVE,
    BYPASS_ACTIVE,
    INSUFFICIENT_HISTORY,
    BELOW_DEVIATION_THRESHOLD,
    TRIGGERED,
    EVALUATION_ERROR,
}

data class InterventionDecisionAudit(
    val id: Long = 0,
    val timestamp: Long,
    val packageName: String,
    val monitoredUsageMinutes: Double,
    val totalUsageMinutes: Double,
    val ambientLux: Float,
    val historyCount: Int,
    val baselineMedianMinutes: Double?,
    val madMinutes: Double?,
    val deviationSignal: Double?,
    val relativeDeviation: Double?,
    val relativeMagnitude: Double?,
    val deviation: Double?,
    val performance: Double?,
    val qLux: Double?,
    val qTime: Double?,
    val sensitivity: Double?,
    val difficultyControl: Double?,
    val difficultyControlSignal: Double?,
    val difficultyTarget: Double?,
    val currentDifficulty: Int,
    val nextDifficulty: Int?,
    val challengeType: InterventionChallengeType?,
    val frequencyControl: Double?,
    val normalizedFrequencyControl: Double?,
    val proposedIntervalMinutes: Double?,
    val nextEligibleAt: Long?,
    val shouldTrigger: Boolean,
    val reason: InterventionDecisionReason,
    val controlMode: ControlMode?,
    val errorType: String?,
)
