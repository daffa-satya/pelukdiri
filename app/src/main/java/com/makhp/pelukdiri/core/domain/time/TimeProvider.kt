package com.makhp.pelukdiri.core.domain.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

interface TimeProvider {
    fun nowMillis(): Long
    fun zoneId(): ZoneId
    fun today(): LocalDate = Instant.ofEpochMilli(nowMillis()).atZone(zoneId()).toLocalDate()
}
