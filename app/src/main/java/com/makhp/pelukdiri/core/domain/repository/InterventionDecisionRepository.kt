package com.makhp.pelukdiri.core.domain.repository

import com.makhp.pelukdiri.core.domain.model.InterventionDecisionAudit

interface InterventionDecisionRepository {
    suspend fun insert(decision: InterventionDecisionAudit)
    suspend fun getAllList(): List<InterventionDecisionAudit>
    suspend fun getTriggeredCountsInRange(startMillis: Long, endMillis: Long): Map<String, Int>
}
