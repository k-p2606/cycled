package com.example.cycled.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDb = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        CycledDatabase::class.java,
    )

    @Test
    fun migrate1To2_keepsPeriodDayAndSetsLoggedAtToZero() {
        val date = LocalDate.of(2026, 11, 5)

        helper.createDatabase(testDb, 1).apply {
            execSQL(
                "INSERT INTO period_days (date, flow) VALUES (?, ?)",
                arrayOf<Any>(date.toEpochDay(), FlowLevel.MEDIUM.name),
            )
            close()
        }

        // Runs the AutoMigration and validates the result against schemas/2.json.
        val db = helper.runMigrationsAndValidate(testDb, 2, true)

        db.query("SELECT date, flow, loggedAt FROM period_days").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(date.toEpochDay(), cursor.getLong(0))
            assertEquals(FlowLevel.MEDIUM.name, cursor.getString(1))
            assertEquals(0L, cursor.getLong(2))
            assertEquals(1, cursor.count)
        }
        db.close()
    }

    @Test
    fun migrate2To3_keepsDailyLogAndSetsNoFlowToFalse() {
        val date = LocalDate.of(2026, 11, 8)

        helper.createDatabase(testDb, 2).apply {
            execSQL(
                "INSERT INTO daily_logs (date, symptoms, mood, note) VALUES (?, ?, ?, ?)",
                arrayOf<Any>(date.toEpochDay(), "CRAMPS,FATIGUE", Mood.LOW.name, "note"),
            )
            execSQL(
                "INSERT INTO period_days (date, flow, loggedAt) VALUES (?, ?, ?)",
                arrayOf<Any>(date.toEpochDay(), FlowLevel.HEAVY.name, 1_234L),
            )
            close()
        }

        // Runs the AutoMigration and validates the result against schemas/3.json.
        val db = helper.runMigrationsAndValidate(testDb, 3, true)

        db.query("SELECT date, symptoms, mood, note, noFlow FROM daily_logs").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(date.toEpochDay(), cursor.getLong(0))
            assertEquals("CRAMPS,FATIGUE", cursor.getString(1))
            assertEquals(Mood.LOW.name, cursor.getString(2))
            assertEquals("note", cursor.getString(3))
            assertEquals(0, cursor.getInt(4))
            assertEquals(1, cursor.count)
        }
        db.query("SELECT date, flow, loggedAt FROM period_days").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(FlowLevel.HEAVY.name, cursor.getString(1))
            assertEquals(1_234L, cursor.getLong(2))
        }
        db.close()
    }
}
