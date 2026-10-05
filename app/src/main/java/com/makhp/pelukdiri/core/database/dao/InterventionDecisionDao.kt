package com.makhp.pelukdiri.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.makhp.pelukdiri.core.database.entity.InterventionDecisionEntity

data class PackageDecisionCount(
    val packageName: String,
    val count: Int
)

@Dao
interface InterventionDecisionDao {
    @Insert
    suspend fun insert(decision: InterventionDecisionEntity)

    @Query("SELECT * FROM intervention_decisions ORDER BY timestamp ASC, id ASC")
    suspend fun getAllList(): List<InterventionDecisionEntity>

    @Query("SELECT packageName, COUNT(*) as count FROM intervention_decisions WHERE reason = :reason AND timestamp BETWEEN :startMillis AND :endMillis GROUP BY packageName")
    suspend fun getDecisionCountsByReasonAndTimestampRange(
        reason: String,
        startMillis: Long,
        endMillis: Long
    ): List<PackageDecisionCount>
}
