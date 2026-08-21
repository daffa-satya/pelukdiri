package com.makhp.pelukdiri.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.makhp.pelukdiri.core.database.dao.AdaptiveLimitDao
import com.makhp.pelukdiri.core.database.dao.InterventionDao
import com.makhp.pelukdiri.core.database.dao.InterventionNotificationDao
import com.makhp.pelukdiri.core.database.dao.UsageDao
import com.makhp.pelukdiri.core.database.dao.UsageSensorDao
import com.makhp.pelukdiri.core.database.entity.AppUsageEntity
import com.makhp.pelukdiri.core.database.entity.DailyAdaptiveLimitEntity
import com.makhp.pelukdiri.core.database.entity.DailySummaryEntity
import com.makhp.pelukdiri.core.database.entity.InterventionLogEntity
import com.makhp.pelukdiri.core.database.entity.InterventionNotificationEntity
import com.makhp.pelukdiri.core.database.entity.UsageSensorLogEntity

@Database(
    entities = [
        UsageSensorLogEntity::class,
        InterventionLogEntity::class,
        DailyAdaptiveLimitEntity::class,
        AppUsageEntity::class,
        DailySummaryEntity::class,
        InterventionNotificationEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class PelukDiriDatabase : RoomDatabase() {
    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_summary ADD COLUMN totalScreenOnMillis INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE intervention_logs ADD COLUMN challengeType TEXT NOT NULL DEFAULT 'MATH'"
                )
            }
        }
    }
    abstract fun usageSensorDao(): UsageSensorDao
    abstract fun interventionDao(): InterventionDao
    abstract fun interventionNotificationDao(): InterventionNotificationDao
    abstract fun adaptiveLimitDao(): AdaptiveLimitDao
    abstract fun usageDao(): UsageDao
}
