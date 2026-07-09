package com.makhp.pelukdiri.core.data.mapper

import com.makhp.pelukdiri.core.data.database.AppUsageEntity
import com.makhp.pelukdiri.core.data.database.DailySummaryEntity
import com.makhp.pelukdiri.core.data.database.InterventionEntity
import com.makhp.pelukdiri.core.domain.model.AppUsage
import com.makhp.pelukdiri.core.domain.model.DailySummary
import com.makhp.pelukdiri.core.domain.model.Intervention
import com.makhp.pelukdiri.core.domain.model.InterventionType
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
        unlockCount = unlockCount,
        mostUsedApp = mostUsedApp,
        wellbeingScore = wellbeingScore
    )
}

fun InterventionEntity.toDomainModel(): Intervention {
    return Intervention(
        id = id,
        title = title,
        message = message,
        type = InterventionType.valueOf(type),
        timestamp = LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC),
        isAcknowledged = isAcknowledged
    )
}

fun Intervention.toEntity(): InterventionEntity {
    return InterventionEntity(
        id = id,
        title = title,
        message = message,
        type = type.name,
        timestamp = timestamp.toEpochSecond(ZoneOffset.UTC),
        isAcknowledged = isAcknowledged
    )
}
