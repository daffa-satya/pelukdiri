package com.makhp.pelukdiri.core.domain.repository

import com.makhp.pelukdiri.core.domain.model.Intervention
import kotlinx.coroutines.flow.Flow

interface InterventionRepository {
    fun getInterventions(): Flow<List<Intervention>>
    suspend fun saveIntervention(intervention: Intervention)
    suspend fun markAsAcknowledged(id: String)
}
