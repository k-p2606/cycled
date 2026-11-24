package com.example.cycled.domain

import com.example.cycled.data.FlowLevel
import com.example.cycled.data.Mood
import com.example.cycled.data.Symptom
import com.example.cycled.ui.SampleData
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CycleLogsTest {

    private fun d(month: Int, day: Int) = LocalDate.of(2026, month, day)

    private fun days(from: LocalDate, to: LocalDate, flow: FlowLevel = FlowLevel.MEDIUM) =
        generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.associateWith { flow }

    private fun logs(
        periodDays: Map<LocalDate, FlowLevel>,
        notes: Map<LocalDate, DayNotes> = emptyMap(),
        today: LocalDate,
    ) = cycleLogs(groupIntoPeriods(periodDays.keys.toList()), periodDays, notes, today)

    @Test
    fun noData_isEmpty() {
        assertTrue(cycleLogs(emptyList(), emptyMap(), emptyMap(), d(11, 8)).isEmpty())
    }

    @Test
    fun notesButNoPeriod_isEmpty() {
        val notes = mapOf(d(11, 2) to DayNotes(mood = Mood.LOW))
        assertTrue(logs(emptyMap(), notes, d(11, 8)).isEmpty())
    }

    @Test
    fun onePeriod_isOneCurrentCycle() {
        val cycles = logs(days(d(11, 1), d(11, 5)), today = d(11, 8))

        val c = cycles.single()
        assertTrue(c.isCurrent)
        assertEquals(d(11, 1), c.start)
        assertNull(c.end)
        assertEquals(8, c.lengthDays)
        assertEquals(5, c.periodDays)
        assertEquals(5, c.daysLogged)
        assertEquals((5 downTo 1).map { d(11, it) }, c.days.map { it.date })
        assertEquals((5 downTo 1).toList(), c.days.map { it.cycleDay })
    }

    @Test
    fun currentCycle_onItsFirstDay_isDayOne() {
        val c = logs(days(d(10, 3), d(10, 7)) + days(d(11, 8), d(11, 8)), today = d(11, 8)).first()
        assertTrue(c.isCurrent)
        assertEquals(1, c.lengthDays)
    }

    @Test
    fun completedCycle_endsTheDayBeforeTheNextStart() {
        val cycles = logs(days(d(10, 3), d(10, 7)) + days(d(10, 31), d(11, 4)), today = d(11, 8))

        assertEquals(2, cycles.size)
        val (current, previous) = cycles
        assertTrue(current.isCurrent)
        assertEquals(9, current.lengthDays)
        assertFalse(previous.isCurrent)
        assertEquals(d(10, 3), previous.start)
        assertEquals(d(10, 30), previous.end)
        assertEquals(28, previous.lengthDays)
    }

    @Test
    fun spotting_countsAsAPeriodDayAndKeepsItsFlow() {
        val periodDays = days(d(11, 1), d(11, 4)) + (d(11, 5) to FlowLevel.SPOTTING)
        val c = logs(periodDays, today = d(11, 8)).single()

        assertEquals(5, c.periodDays)
        assertEquals(FlowLevel.SPOTTING, c.days.first().flow)
    }

    @Test
    fun noFlowDays_areListedButNotPeriodDays() {
        val notes = mapOf(d(11, 6) to DayNotes(noFlow = true))
        val c = logs(days(d(11, 1), d(11, 5)), notes, d(11, 8)).single()

        assertEquals(5, c.periodDays)
        assertEquals(6, c.daysLogged)
        val nov6 = c.days.first()
        assertEquals(d(11, 6), nov6.date)
        assertNull(nov6.flow)
        assertTrue(nov6.noFlow)
    }

    @Test
    fun noFlowIsIgnoredOnADayWithFlow() {
        val notes = mapOf(d(11, 2) to DayNotes(noFlow = true))
        val day = logs(days(d(11, 1), d(11, 5)), notes, d(11, 8)).single().days.first { it.date == d(11, 2) }
        assertFalse(day.noFlow)
        assertEquals(FlowLevel.MEDIUM, day.flow)
    }

    @Test
    fun dayWithOnlyAMood_isListed() {
        val notes = mapOf(d(11, 7) to DayNotes(mood = Mood.CALM))
        val nov7 = logs(days(d(11, 1), d(11, 5)), notes, d(11, 8)).single().days.first()

        assertEquals(d(11, 7), nov7.date)
        assertEquals(7, nov7.cycleDay)
        assertNull(nov7.flow)
        assertFalse(nov7.noFlow)
        assertTrue(nov7.symptoms.isEmpty())
        assertEquals(Mood.CALM, nov7.mood)
    }

    @Test
    fun emptyNotes_andDaysOutsideAnyCycle_areLeftOut() {
        val notes = mapOf(
            d(11, 6) to DayNotes(),                       // nothing in it
            d(10, 20) to DayNotes(mood = Mood.LOW),       // before the first start
            d(11, 9) to DayNotes(mood = Mood.LOW),        // after today
        )
        val c = logs(days(d(11, 1), d(11, 5)), notes, d(11, 8)).single()
        assertEquals((5 downTo 1).map { d(11, it) }, c.days.map { it.date })
    }

    @Test
    fun cycleWithNothingLoggedButItsStart_stillHasTheStart() {
        val c = logs(days(d(11, 1), d(11, 1)), today = d(11, 20)).single()
        assertEquals(1, c.daysLogged)
        assertEquals(20, c.lengthDays)
    }

    // ---- The handoff's sample data (D12) ----

    private val sample = cycleLogs(
        SampleData.periods,
        SampleData.periodDays,
        SampleData.dailyLogs.mapValues { (_, log) -> DayNotes(log.noFlow, log.symptoms, log.mood) },
        SampleData.today,
    )

    @Test
    fun sample_matchesTheHandoffsCycleList() {
        // "31 Oct (current, day 9); 3 Oct – 30 Oct (28 days); 7 Sep – 2 Oct (26 days); 9 Aug – 6 Sep (29 days)"
        assertEquals(listOf(d(10, 31), d(10, 3), d(9, 7), d(8, 9)), sample.map { it.start })
        assertEquals(listOf(null, d(10, 30), d(10, 2), d(9, 6)), sample.map { it.end })
        assertEquals(listOf(9, 28, 26, 29), sample.map { it.lengthDays })
        assertEquals(listOf(true, false, false, false), sample.map { it.isCurrent })
    }

    @Test
    fun sample_currentCycleMatchesTheScreenshot() {
        // "Day 9 so far · period 5 days · 5 days logged"
        val current = sample.first()
        assertEquals(5, current.periodDays)
        assertEquals(5, current.daysLogged)

        val nov4 = current.days.first()
        assertEquals(d(11, 4), nov4.date)
        assertEquals(5, nov4.cycleDay)
        assertEquals(FlowLevel.LIGHT, nov4.flow)
        assertEquals(Mood.CALM, nov4.mood)

        val nov1 = current.days.first { it.date == d(11, 1) }
        assertEquals(FlowLevel.HEAVY, nov1.flow)
        assertEquals(setOf(Symptom.CRAMPS, Symptom.FATIGUE), nov1.symptoms)
        assertEquals(2, nov1.cycleDay)
    }

    @Test
    fun sample_olderCyclesCountTheirDays() {
        val (_, oct, sep, aug) = sample
        assertEquals(5, oct.periodDays)
        assertEquals(8, oct.daysLogged)       // 3-7 Oct, 15, 22, 27 Oct
        assertEquals(5, sep.periodDays)
        assertEquals(7, sep.daysLogged)       // 7-11 Sep, 20, 28 Sep
        assertEquals(5, aug.periodDays)
        assertEquals(6, aug.daysLogged)       // 9-13 Aug, 30 Aug
        assertEquals(listOf(d(10, 27), d(10, 22), d(10, 15)), oct.days.take(3).map { it.date })
    }
}
