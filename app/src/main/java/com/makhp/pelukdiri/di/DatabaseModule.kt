package com.makhp.pelukdiri.di

import android.content.Context
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.makhp.pelukdiri.core.database.PelukDiriDatabase
import com.makhp.pelukdiri.core.database.dao.AdaptiveLimitDao
import com.makhp.pelukdiri.core.database.dao.InterventionDao
import com.makhp.pelukdiri.core.database.dao.InterventionNotificationDao
import com.makhp.pelukdiri.core.database.dao.UsageDao
import com.makhp.pelukdiri.core.database.dao.UsageSensorDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private val migration1To2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS intervention_logs_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    riskScore REAL NOT NULL,
                    difficultyLevel INTEGER NOT NULL,
                    responseTimeMs INTEGER NOT NULL,
                    isSuccess INTEGER NOT NULL,
                    penaltyAppliedMinutes INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO intervention_logs_new (id, timestamp, riskScore, difficultyLevel, responseTimeMs, isSuccess, penaltyAppliedMinutes)
                SELECT
                    id,
                    timestamp,
                    0.0,
                    CASE difficultyLevel
                        WHEN 'EASY' THEN 1
                        WHEN 'MEDIUM' THEN 3
                        WHEN 'HARD' THEN 5
                        ELSE 1
                    END,
                    responseTimeMs,
                    isCorrect,
                    CASE difficultyLevel
                        WHEN 'EASY' THEN 0
                        WHEN 'MEDIUM' THEN 10
                        WHEN 'HARD' THEN 20
                        ELSE 0
                    END
                FROM intervention_logs
                """.trimIndent()
            )
            db.execSQL("DROP TABLE intervention_logs")
            db.execSQL("ALTER TABLE intervention_logs_new RENAME TO intervention_logs")
        }
    }

    private val migration2To3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE intervention_logs ADD COLUMN isBypassed INTEGER NOT NULL DEFAULT 0")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PelukDiriDatabase {
        return Room.databaseBuilder(
            context,
            PelukDiriDatabase::class.java,
            "pelukdiri_db"
        )
            .addMigrations(
                migration1To2,
                migration2To3,
                PelukDiriDatabase.MIGRATION_3_4,
                PelukDiriDatabase.MIGRATION_5_6,
            )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideUsageSensorDao(db: PelukDiriDatabase): UsageSensorDao = db.usageSensorDao()

    @Provides
    @Singleton
    fun provideInterventionDao(db: PelukDiriDatabase): InterventionDao = db.interventionDao()

    @Provides
    @Singleton
    fun provideInterventionNotificationDao(db: PelukDiriDatabase): InterventionNotificationDao = db.interventionNotificationDao()

    @Provides
    @Singleton
    fun provideAdaptiveLimitDao(db: PelukDiriDatabase): AdaptiveLimitDao = db.adaptiveLimitDao()

    @Provides
    @Singleton
    fun provideUsageDao(db: PelukDiriDatabase): UsageDao = db.usageDao()
}
