package com.example.cycled.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CycledDatabaseTest {

    private lateinit var db: CycledDatabase
    private lateinit var periodDayDao: PeriodDayDao
    private lateinit var dailyLogDao: DailyLogDao

    private val date = LocalDate.of(2026, 11, 9)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CycledDatabase::class.java,
        ).allowMainThreadQueries().build()
        periodDayDao = db.periodDayDao()
        dailyLogDao = db.dailyLogDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun savingPeriodDay_returnsExactlyThatRow() = runBlocking {
        val day = PeriodDay(date, FlowLevel.MEDIUM)

        periodDayDao.upsert(day)

        assertEquals(listOf(day), periodDayDao.observeAll().first())
    }

    @Test
    fun upsertingSameDate_replacesFlowAndKeepsOneRow() = runBlocking {
        periodDayDao.upsert(PeriodDay(date, FlowLevel.MEDIUM))

        periodDayDao.upsert(PeriodDay(date, FlowLevel.HEAVY))

        val all = periodDayDao.observeAll().first()
        assertEquals(1, all.size)
        assertEquals(PeriodDay(date, FlowLevel.HEAVY), all.single())
    }

    @Test
    fun deletingPeriodDay_leavesListEmpty() = runBlocking {
        periodDayDao.upsert(PeriodDay(date, FlowLevel.MEDIUM))

        periodDayDao.delete(date)

        assertTrue(periodDayDao.observeAll().first().isEmpty())
    }

    @Test
    fun savingDailyLog_returnsEqualObjectWithBothSymptoms() = runBlocking {
        val log = DailyLog(
            date = date,
            symptoms = setOf(Symptom.CRAMPS, Symptom.FATIGUE),
            mood = Mood.CALM,
            note = "test",
        )

        dailyLogDao.upsert(log)

        val loaded = dailyLogDao.observe(date).first()
        assertEquals(log, loaded)
        assertEquals(setOf(Symptom.CRAMPS, Symptom.FATIGUE), loaded!!.symptoms)
    }

    @Test
    fun dailyLogWithNoSymptoms_comesBackWithEmptySet() = runBlocking {
        dailyLogDao.upsert(DailyLog(date = date, symptoms = emptySet()))

        val loaded = dailyLogDao.observe(date).first()
        assertNotNull(loaded)
        assertNotNull(loaded!!.symptoms)
        assertTrue(loaded.symptoms.isEmpty())
        assertEquals(emptySet<Symptom>(), loaded.symptoms)
    }

    @Test
    fun observeRange_returnsOnlyLogsInRange_orderedByDate() = runBlocking {
        val before = DailyLog(date = date.minusDays(5), note = "before")
        val start = DailyLog(date = date.minusDays(2), note = "start")
        val middle = DailyLog(date = date, note = "middle")
        val end = DailyLog(date = date.plusDays(2), note = "end")
        val after = DailyLog(date = date.plusDays(5), note = "after")

        // Insert out of order so ordering comes from the query, not insertion.
        listOf(after, middle, before, end, start).forEach { dailyLogDao.upsert(it) }

        val range = dailyLogDao.observeRange(date.minusDays(2), date.plusDays(2)).first()

        assertEquals(listOf(start, middle, end), range)
    }
}
