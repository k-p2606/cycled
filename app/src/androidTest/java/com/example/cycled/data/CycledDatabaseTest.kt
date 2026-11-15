package com.example.cycled.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
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
    fun recentlyLogged_ordersByLoggedAtNotDate() = runBlocking {
        val nov5 = PeriodDay(LocalDate.of(2026, 11, 5), FlowLevel.MEDIUM, loggedAt = 1_000L)
        val sep3 = PeriodDay(LocalDate.of(2026, 9, 3), FlowLevel.LIGHT, loggedAt = 2_000L)

        // Later date logged first, earlier date logged second.
        periodDayDao.upsert(nov5)
        periodDayDao.upsert(sep3)

        assertEquals(listOf(sep3, nov5), periodDayDao.observeRecentlyLogged(7).first())
        // Date order is unchanged for everything else.
        assertEquals(listOf(sep3, nov5), periodDayDao.observeAll().first().sortedBy { it.date })
        assertEquals(listOf(sep3.date, nov5.date), periodDayDao.observeAll().first().map { it.date })
    }

    @Test
    fun recentlyLogged_tiesFallBackToLaterDateFirst_andRespectsLimit() = runBlocking {
        (1..9).forEach { periodDayDao.upsert(PeriodDay(LocalDate.of(2026, 11, it), FlowLevel.LIGHT)) }

        val recent = periodDayDao.observeRecentlyLogged(7).first()

        assertEquals((9 downTo 3).map { LocalDate.of(2026, 11, it) }, recent.map { it.date })
    }

    @Test
    fun repository_updatingDay_movesItToTopOfRecentlyLogged() = runBlocking {
        var clock = 1_000L
        val repository = CycleRepository(periodDayDao, dailyLogDao, now = { Instant.ofEpochMilli(clock) })
        val nov5 = LocalDate.of(2026, 11, 5)
        val sep3 = LocalDate.of(2026, 9, 3)

        repository.logPeriodDay(sep3, FlowLevel.LIGHT)
        clock = 2_000L
        repository.logPeriodDay(nov5, FlowLevel.MEDIUM)
        clock = 3_000L
        repository.logPeriodDay(sep3, FlowLevel.HEAVY)

        val recent = repository.observeRecentlyLoggedDays(7).first()
        assertEquals(listOf(sep3, nov5), recent.map { it.date })
        assertEquals(3_000L, recent.first().loggedAt)

        repository.removePeriodDay(sep3)
        assertEquals(listOf(nov5), repository.observeRecentlyLoggedDays(7).first().map { it.date })
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
