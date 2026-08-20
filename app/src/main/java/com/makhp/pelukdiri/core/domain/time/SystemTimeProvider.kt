package com.makhp.pelukdiri.core.domain.time

import java.time.ZoneId
import javax.inject.Inject

class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun nowMillis(): Long = System.currentTimeMillis()
    override fun zoneId(): ZoneId = ZoneId.systemDefault()
}
