package com.makhp.pelukdiri.core.data.mapper

import com.makhp.pelukdiri.core.database.entity.AppUsageEntity
import com.makhp.pelukdiri.core.database.entity.DailySummaryEntity
import com.makhp.pelukdiri.core.database.entity.InterventionNotificationEntity
import com.makhp.pelukdiri.core.database.entity.DailyAdaptiveLimitEntity
import com.makhp.pelukdiri.core.database.entity.InterventionLogEntity
import com.makhp.pelukdiri.core.database.entity.UsageSensorLogEntity
import com.makhp.pelukdiri.core.domain.model.AppUsage
import com.makhp.pelukdiri.core.domain.model.DailyAdaptiveLimit
import com.makhp.pelukdiri.core.domain.model.DailySummary
import com.makhp.pelukdiri.core.domain.model.Intervention
import com.makhp.pelukdiri.core.domain.model.InterventionLog
import com.makhp.pelukdiri.core.domain.model.InterventionType
import com.makhp.pelukdiri.core.domain.model.UsageSensorLog
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

fun AppUsageEntity.toDomainModel(): AppUsage {
    return AppUsage(
        packageName = packageName,
        appName = appName,
        usageDurationMillis = usageDurationMillis,
        lastUsedTimestamp = lastUsedTimestamp
    )
}

fun AppUsage.toEntity(date: String): AppUsageEntity {
    return AppUsageEntity(
        packageName = packageName,
        appName = appName,
        usageDurationMillis = usageDurationMillis,
        lastUsedTimestamp = lastUsedTimestamp,
        date = date
    )
}

fun DailySummaryEntity.toDomainModel(): DailySummary {
    return DailySummary(
        date = LocalDate.parse(date),
        totalScreenTimeMillis = totalScreenTimeMillis,
        totalScreenOnMillis = totalScreenOnMillis,
        unlockCount = unlockCount,
        mostUsedApp = mostUsedApp,
        wellbeingScore = wellbeingScore
    )
}

fun InterventionNotificationEntity.toDomainModel(): Intervention {
    return Intervention(
        id = id,
        title = title,
        message = message,
        type = InterventionType.valueOf(type),
        timestamp = LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC),
        isAcknowledged = isAcknowledged
    )
}

fun Intervention.toEntity(): InterventionNotificationEntity {
    return InterventionNotificationEntity(
        id = id,
        title = title,
        message = message,
        type = type.name,
        timestamp = timestamp.toEpochSecond(ZoneOffset.UTC),
        isAcknowledged = isAcknowledged
    )
}

fun DailyAdaptiveLimitEntity.toDomainModel(): DailyAdaptiveLimit {
    return DailyAdaptiveLimit(
        dateString = dateString,
        calculatedLimitMinutes = calculatedLimitMinutes,
        actualScreenTimeMinutes = actualScreenTimeMinutes,
        reclaimedTimeMinutes = reclaimedTimeMinutes
    )
}

fun DailyAdaptiveLimit.toEntity(): DailyAdaptiveLimitEntity {
    return DailyAdaptiveLimitEntity(
        dateString = dateString,
        calculatedLimitMinutes = calculatedLimitMinutes,
        actualScreenTimeMinutes = actualScreenTimeMinutes,
        reclaimedTimeMinutes = reclaimedTimeMinutes
    )
}

fun InterventionLogEntity.toDomainModel(): InterventionLog {
    return InterventionLog(
        id = id,
        timestamp = timestamp,
        riskScore = riskScore,
        difficultyLevel = difficultyLevel,
        responseTimeMs = responseTimeMs,
        isSuccess = isSuccess,
        isBypassed = isBypassed,
        penaltyAppliedMinutes = penaltyAppliedMinutes
    )
}

fun InterventionLog.toEntity(): InterventionLogEntity {
    return InterventionLogEntity(
        id = id,
        timestamp = timestamp,
        riskScore = riskScore,
        difficultyLevel = difficultyLevel,
        responseTimeMs = responseTimeMs,
        isSuccess = isSuccess,
        isBypassed = isBypassed,
        penaltyAppliedMinutes = penaltyAppliedMinutes
    )
}

fun UsageSensorLogEntity.toDomainModel(): UsageSensorLog {
    return UsageSensorLog(
        id = id,
        timestamp = timestamp,
        packageName = packageName,
        rawScreenTimeMs = rawScreenTimeMs,
        appOpeningFrequency = appOpeningFrequency,
        ambientLightLux = ambientLightLux
    )
}

fun UsageSensorLog.toEntity(): UsageSensorLogEntity {
    return UsageSensorLogEntity(
        id = id,
        timestamp = timestamp,
        packageName = packageName,
        rawScreenTimeMs = rawScreenTimeMs,
        appOpeningFrequency = appOpeningFrequency,
        ambientLightLux = ambientLightLux
    )
}
