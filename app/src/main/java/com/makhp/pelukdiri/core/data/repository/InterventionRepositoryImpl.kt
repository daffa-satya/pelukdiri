package com.makhp.pelukdiri.core.data.repository

import com.makhp.pelukdiri.core.data.database.InterventionDao
import com.makhp.pelukdiri.core.data.mapper.toDomainModel
import com.makhp.pelukdiri.core.data.mapper.toEntity
import com.makhp.pelukdiri.core.domain.model.Intervention
import com.makhp.pelukdiri.core.domain.repository.InterventionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class InterventionRepositoryImpl @Inject constructor(
    private val dao: InterventionDao
) : InterventionRepository {
    override fun getInterventions(): Flow<List<Intervention>> {
        return dao.getAllInterventions().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun saveIntervention(intervention: Intervention) {
        dao.insertIntervention(intervention.toEntity())
    }

    override suspend fun markAsAcknowledged(id: String) {
        dao.markAsAcknowledged(id)
    }
}
