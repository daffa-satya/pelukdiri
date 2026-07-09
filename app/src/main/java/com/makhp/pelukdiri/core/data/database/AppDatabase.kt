package com.makhp.pelukdiri.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        AppUsageEntity::class,
        DailySummaryEntity::class,
        InterventionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usageDao(): UsageDao
    abstract fun interventionDao(): InterventionDao

    companion object {
        const val DATABASE_NAME = "pelukdiri_db"
    }
}
