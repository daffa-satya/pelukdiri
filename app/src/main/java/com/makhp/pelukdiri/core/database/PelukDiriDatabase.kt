package com.makhp.pelukdiri.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.makhp.pelukdiri.core.database.dao.AdaptiveLimitDao
import com.makhp.pelukdiri.core.database.dao.InterventionDao
import com.makhp.pelukdiri.core.database.dao.InterventionDecisionDao
import com.makhp.pelukdiri.core.database.dao.InterventionNotificationDao
import com.makhp.pelukdiri.core.database.dao.UsageDao
import com.makhp.pelukdiri.core.database.dao.UsageSensorDao
import com.makhp.pelukdiri.core.database.entity.AppUsageEntity
import com.makhp.pelukdiri.core.database.entity.DailyAdaptiveLimitEntity
import com.makhp.pelukdiri.core.database.entity.DailySummaryEntity
import com.makhp.pelukdiri.core.database.entity.InterventionLogEntity
import com.makhp.pelukdiri.core.database.entity.InterventionDecisionEntity
import com.makhp.pelukdiri.core.database.entity.InterventionNotificationEntity
import com.makhp.pelukdiri.core.database.entity.UsageSensorLogEntity

@Database(
    entities = [
        UsageSensorLogEntity::class,
        InterventionLogEntity::class,
        DailyAdaptiveLimitEntity::class,
        AppUsageEntity::class,
        DailySummaryEntity::class,
        InterventionNotificationEntity::class,
        InterventionDecisionEntity::class,
    ],
    version = 7,
    exportSchema = false
)
abstract class PelukDiriDatabase : RoomDatabase() {
    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_summary ADD COLUMN totalScreenOnMillis INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE intervention_logs ADD COLUMN deviation REAL NOT NULL DEFAULT 0.0")
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE intervention_logs ADD COLUMN challengeType TEXT NOT NULL DEFAULT 'MATH'"
                )
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS intervention_decisions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        packageName TEXT NOT NULL,
                        monitoredUsageMinutes REAL NOT NULL,
                        totalUsageMinutes REAL NOT NULL,
                        ambientLux REAL NOT NULL,
                        historyCount INTEGER NOT NULL,
                        baselineMedianMinutes REAL,
                        madMinutes REAL,
                        deviationSignal REAL,
                        relativeDeviation REAL,
                        relativeMagnitude REAL,
                        deviation REAL,
                        performance REAL,
                        qLux REAL,
                        qTime REAL,
                        sensitivity REAL,
                        difficultyControl REAL,
                        difficultyControlSignal REAL,
                        difficultyTarget REAL,
                        currentDifficulty INTEGER NOT NULL,
                        nextDifficulty INTEGER,
                        challengeType TEXT,
                        frequencyControl REAL,
                        normalizedFrequencyControl REAL,
                        proposedIntervalMinutes REAL,
                        nextEligibleAt INTEGER,
                        shouldTrigger INTEGER NOT NULL,
                        reason TEXT NOT NULL,
                        controlMode TEXT,
                        errorType TEXT
                    )""".trimIndent()
                )
            }
        }
    }
    abstract fun usageSensorDao(): UsageSensorDao
    abstract fun interventionDao(): InterventionDao
    abstract fun interventionDecisionDao(): InterventionDecisionDao
    abstract fun interventionNotificationDao(): InterventionNotificationDao
    abstract fun adaptiveLimitDao(): AdaptiveLimitDao
    abstract fun usageDao(): UsageDao
}
