package com.example.cycled.data

import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The D7 flow rules and the symptom/mood edits, against in-memory fake DAOs. */
class CycleRepositoryTest {

    private class FakePeriodDayDao : PeriodDayDao {
        val rows = MutableStateFlow<Map<LocalDate, PeriodDay>>(emptyMap())
        override suspend fun upsert(day: PeriodDay) { rows.value = rows.value + (day.date to day) }
        override suspend fun delete(date: LocalDate) { rows.value = rows.value - date }
        override fun observeAll(): Flow<List<PeriodDay>> = rows.map { it.values.sortedBy(PeriodDay::date) }
    }

    private class FakeDailyLogDao : DailyLogDao {
        val rows = MutableStateFlow<Map<LocalDate, DailyLog>>(emptyMap())
        override suspend fun upsert(log: DailyLog) { rows.value = rows.value + (log.date to log) }
        override suspend fun delete(date: LocalDate) { rows.value = rows.value - date }
        override fun observe(date: LocalDate): Flow<DailyLog?> = rows.map { it[date] }
        override suspend fun get(date: LocalDate): DailyLog? = rows.value[date]
        override fun observeAll(): Flow<List<DailyLog>> = rows.map { it.values.sortedBy(DailyLog::date) }
        override fun observeRange(from: LocalDate, to: LocalDate): Flow<List<DailyLog>> =
            rows.map { m -> m.values.filter { !it.date.isBefore(from) && !it.date.isAfter(to) }.sortedBy(DailyLog::date) }
    }

    private val periodDays = FakePeriodDayDao()
    private val logs = FakeDailyLogDao()
    private val repository = CycleRepository(periodDays, logs, now = { Instant.ofEpochMilli(42) })
    private val day = LocalDate.of(2026, 11, 8)

    private fun flow() = periodDays.rows.value[day]?.flow
    private fun log() = logs.rows.value[day]

    @Test
    fun lightMediumHeavy_saveAPeriodDay() = runBlocking {
        repository.setFlow(day, FlowChoice.HEAVY)
        assertEquals(FlowLevel.HEAVY, flow())
        assertEquals(42L, periodDays.rows.value.getValue(day).loggedAt)
        assertNull("no empty log row", log())

        repository.setFlow(day, FlowChoice.LIGHT)
        assertEquals(FlowLevel.LIGHT, flow())
    }

    @Test
    fun none_deletesThePeriodDayAndSetsNoFlow() = runBlocking {
        repository.setFlow(day, FlowChoice.MEDIUM)
        repository.setFlow(day, FlowChoice.NONE)
        assertNull(flow())
        assertTrue(log()!!.noFlow)
    }

    @Test
    fun flowAfterNone_clearsNoFlow() = runBlocking {
        repository.setFlow(day, FlowChoice.NONE)
        repository.setFlow(day, FlowChoice.MEDIUM)
        assertEquals(FlowLevel.MEDIUM, flow())
        assertNull("the now-empty log row is removed", log())
    }

    @Test
    fun clear_removesBoth() = runBlocking {
        repository.setFlow(day, FlowChoice.NONE)
        repository.setFlow(day, null)
        assertNull(flow())
        assertNull(log())

        repository.setFlow(day, FlowChoice.HEAVY)
        repository.setFlow(day, null)
        assertNull(flow())
        assertNull(log())
    }

    @Test
    fun anyChip_replacesSpotting() = runBlocking {
        repository.logPeriodDay(day, FlowLevel.SPOTTING)
        repository.setFlow(day, FlowChoice.LIGHT)
        assertEquals(FlowLevel.LIGHT, flow())

        repository.logPeriodDay(day, FlowLevel.SPOTTING)
        repository.setFlow(day, FlowChoice.NONE)
        assertNull(flow())
        assertTrue(log()!!.noFlow)
    }

    @Test
    fun flowChanges_keepSymptomsAndMood() = runBlocking {
        repository.toggleSymptom(day, Symptom.CRAMPS)
        repository.setMood(day, Mood.LOW)
        repository.setFlow(day, FlowChoice.NONE)
        repository.setFlow(day, FlowChoice.HEAVY)
        repository.setFlow(day, null)

        assertEquals(DailyLog(day, symptoms = setOf(Symptom.CRAMPS), mood = Mood.LOW, noFlow = false), log())
    }

    @Test
    fun toggleSymptom_addsThenRemoves_andDeletesEmptyRow() = runBlocking {
        repository.toggleSymptom(day, Symptom.CRAMPS)
        repository.toggleSymptom(day, Symptom.FATIGUE)
        assertEquals(setOf(Symptom.CRAMPS, Symptom.FATIGUE), log()!!.symptoms)

        repository.toggleSymptom(day, Symptom.CRAMPS)
        assertEquals(setOf(Symptom.FATIGUE), log()!!.symptoms)

        repository.toggleSymptom(day, Symptom.FATIGUE)
        assertNull(log())
    }

    @Test
    fun setMood_setsReplacesAndClears() = runBlocking {
        repository.setMood(day, Mood.CALM)
        assertEquals(Mood.CALM, log()!!.mood)
        repository.setMood(day, Mood.HAPPY)
        assertEquals(Mood.HAPPY, log()!!.mood)
        repository.setMood(day, null)
        assertNull(log())
    }

    @Test
    fun clearingKeepsARowWithANote() = runBlocking {
        logs.upsert(DailyLog(day, note = "kept"))
        repository.setMood(day, Mood.LOW)
        repository.setMood(day, null)
        assertEquals(DailyLog(day, note = "kept"), log())
        assertFalse(log()!!.isEmpty)
    }

    @Test
    fun flowChoiceOf_mapsStoredValuesToChips() {
        assertNull(FlowChoice.of(null, noFlow = false))
        assertEquals(FlowChoice.NONE, FlowChoice.of(null, noFlow = true))
        assertNull(FlowChoice.of(FlowLevel.SPOTTING, noFlow = false))
        assertEquals(FlowChoice.HEAVY, FlowChoice.of(FlowLevel.HEAVY, noFlow = true))
    }
}
