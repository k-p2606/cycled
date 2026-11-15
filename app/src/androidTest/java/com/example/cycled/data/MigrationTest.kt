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
}
