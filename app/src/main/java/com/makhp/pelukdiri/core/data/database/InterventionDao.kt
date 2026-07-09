package com.makhp.pelukdiri.core.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InterventionDao {
    @Query("SELECT * FROM interventions ORDER BY timestamp DESC")
    fun getAllInterventions(): Flow<List<InterventionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntervention(intervention: InterventionEntity)

    @Query("UPDATE interventions SET isAcknowledged = 1 WHERE id = :id")
    suspend fun markAsAcknowledged(id: String)
}
