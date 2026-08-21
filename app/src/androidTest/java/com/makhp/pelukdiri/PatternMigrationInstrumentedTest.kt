package com.makhp.pelukdiri

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.makhp.pelukdiri.core.database.PelukDiriDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PatternMigrationInstrumentedTest {
    @Test fun versionFiveLogsMigrateToMathWithoutDataLoss() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "pattern-migration-test.db"
        context.deleteDatabase(databaseName)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(object : SupportSQLiteOpenHelper.Callback(5) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """CREATE TABLE intervention_logs (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                timestamp INTEGER NOT NULL,
                                deviation REAL NOT NULL,
                                riskScore REAL NOT NULL,
                                difficultyLevel INTEGER NOT NULL,
                                responseTimeMs INTEGER NOT NULL,
                                isSuccess INTEGER NOT NULL,
                                isBypassed INTEGER NOT NULL,
                                penaltyAppliedMinutes INTEGER NOT NULL
                            )""".trimIndent()
                        )
                        db.execSQL(
                            """INSERT INTO intervention_logs
                                (timestamp,deviation,riskScore,difficultyLevel,responseTimeMs,isSuccess,isBypassed,penaltyAppliedMinutes)
                                VALUES (1,0.2,0.5,2,1000,1,0,0)""".trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )

        try {
            val db = helper.writableDatabase
            PelukDiriDatabase.MIGRATION_5_6.migrate(db)
            db.query("PRAGMA table_info(intervention_logs)").use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                val names = buildSet {
                    while (cursor.moveToNext()) add(cursor.getString(nameIndex))
                }
                assertTrue("challengeType column missing", "challengeType" in names)
            }
            db.query("SELECT challengeType, COUNT(*) FROM intervention_logs").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("MATH", cursor.getString(0))
                assertEquals(1, cursor.getInt(1))
            }
        } finally {
            helper.close()
            context.deleteDatabase(databaseName)
        }
    }
}
