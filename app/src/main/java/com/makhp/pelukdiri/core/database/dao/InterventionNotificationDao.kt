package com.makhp.pelukdiri.core.database.dao

import androidx.room.*
import com.makhp.pelukdiri.core.database.entity.InterventionNotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InterventionNotificationDao {
    @Query("SELECT * FROM interventions ORDER BY timestamp DESC")
    fun getAllInterventions(): Flow<List<InterventionNotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntervention(intervention: InterventionNotificationEntity)

    @Query("UPDATE interventions SET isAcknowledged = 1 WHERE id = :id")
    suspend fun markAsAcknowledged(id: String)

    @Query("SELECT * FROM interventions ORDER BY timestamp DESC")
    suspend fun getAllInterventionsList(): List<InterventionNotificationEntity>
}
