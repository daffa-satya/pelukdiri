package com.makhp.pelukdiri.core.data.repository

import com.makhp.pelukdiri.core.data.mapper.toDomainModel
import com.makhp.pelukdiri.core.data.mapper.toEntity
import com.makhp.pelukdiri.core.database.dao.InterventionDecisionDao
import com.makhp.pelukdiri.core.domain.model.InterventionDecisionAudit
import com.makhp.pelukdiri.core.domain.repository.InterventionDecisionRepository
import javax.inject.Inject

import com.makhp.pelukdiri.core.domain.model.InterventionDecisionReason

class InterventionDecisionRepositoryImpl @Inject constructor(
    private val dao: InterventionDecisionDao,
) : InterventionDecisionRepository {
    override suspend fun insert(decision: InterventionDecisionAudit) = dao.insert(decision.toEntity())

    override suspend fun getAllList(): List<InterventionDecisionAudit> =
        dao.getAllList().map { it.toDomainModel() }

    override suspend fun getTriggeredCountsInRange(startMillis: Long, endMillis: Long): Map<String, Int> {
        return dao.getDecisionCountsByReasonAndTimestampRange(
            reason = InterventionDecisionReason.TRIGGERED.name,
            startMillis = startMillis,
            endMillis = endMillis
        ).associate { it.packageName to it.count }
    }
}
