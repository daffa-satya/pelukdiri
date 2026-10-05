package com.makhp.pelukdiri.collector

import java.time.LocalDate
import java.time.ZoneId

internal data class UsageDayBounds(
    val startMillis: Long,
    val endExclusiveMillis: Long,
) {
    fun queryEnd(nowMillis: Long): Long = nowMillis.coerceIn(startMillis, endExclusiveMillis)
}

internal fun usageDayBounds(date: LocalDate, zoneId: ZoneId) = UsageDayBounds(
    startMillis = date.atStartOfDay(zoneId).toInstant().toEpochMilli(),
    endExclusiveMillis = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli(),
)
