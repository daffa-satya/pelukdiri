package com.makhp.pelukdiri.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.makhp.pelukdiri.core.database.entity.InterventionDecisionEntity

@Dao
interface InterventionDecisionDao {
    @Insert
    suspend fun insert(decision: InterventionDecisionEntity)

    @Query("SELECT * FROM intervention_decisions ORDER BY timestamp ASC, id ASC")
    suspend fun getAllList(): List<InterventionDecisionEntity>
}
