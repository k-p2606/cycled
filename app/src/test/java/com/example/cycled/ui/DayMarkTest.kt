package com.example.cycled.ui

import com.example.cycled.data.FlowLevel
import com.example.cycled.domain.AverageCyclePredictor
import com.example.cycled.domain.CycleDefaults
import com.example.cycled.domain.Phase
import com.example.cycled.domain.groupIntoPeriods
import com.example.cycled.domain.phaseOn
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DayMarkTest {

    private fun d(month: Int, day: Int) = LocalDate.of(2026, month, day)

    private val predictor = AverageCyclePredictor()

    private fun days(from: LocalDate, to: LocalDate, flow: FlowLevel = FlowLevel.MEDIUM) =
        generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.associateWith { flow }

    private fun state(logged: Map<LocalDate, FlowLevel>, today: LocalDate) =
        buildCalendarState(logged, groupIntoPeriods(logged.keys.toList()), today, predictor)

    // Oct 3-6 (4 days) and Nov 1-5, today Nov 13.
    // Cycle length 29 -> next start Nov 30; typical period length 4 -> predicted Nov 30 - Dec 3.
    // Ovulation Nov 16; fertile window Nov 11 - Nov 17.
    private val twoPeriods = days(d(10, 3), d(10, 6), FlowLevel.LIGHT) + days(d(11, 1), d(11, 5), FlowLevel.HEAVY)
    private val calendar = state(twoPeriods, d(11, 13))

    private fun mark(date: LocalDate) = calendar.markOn(date)

    /** Next start for Nov 1-5 alone, from the placeholder cycle length (Nov 30). */
    private val placeholderNextStart = d(11, 1).plusDays(CycleDefaults.CYCLE_LENGTH_DAYS.toLong())

    private fun assertNoForecastMarks(m: DayMark) {
        assertNull(m.phase)
        assertFalse(m.isPredictedPeriod)
        assertFalse(m.isFertile)
    }

    @Test
    fun loggedDayInsideAPhase_hasFlowAndPhase() {
        val m = mark(d(11, 3))

        assertEquals(FlowLevel.HEAVY, m.loggedFlow)
        assertEquals(Phase.MENSTRUAL, m.phase)
    }

    @Test
    fun fertileWindow_markedExactlyFromStartToEnd() {
        val phases = calendar.forecast!!.phases
        assertEquals(d(11, 11), phases.fertileStart)
        assertEquals(d(11, 17), phases.fertileEnd)

        assertFalse(mark(d(11, 10)).isFertile)
        for (day in 11..17) assertTrue("Nov $day", mark(d(11, day)).isFertile)
        assertFalse(mark(d(11, 18)).isFertile)
    }

    @Test
    fun predictedPeriod_typicalLengthDaysFromPredictedStart() {
        val forecast = calendar.forecast!!
        assertEquals(d(11, 30), forecast.prediction.nextStart)
        assertEquals(4, forecast.periodLengthDays)

        assertFalse(mark(d(11, 29)).isPredictedPeriod)
        val predicted = (0L..10L).map { d(11, 30).plusDays(it) }.filter { mark(it).isPredictedPeriod }
        assertEquals(listOf(d(11, 30), d(12, 1), d(12, 2), d(12, 3)), predicted)
    }

    @Test
    fun predictedPeriodDays_haveNoPhase() {
        for (day in listOf(d(11, 30), d(12, 3))) {
            val m = mark(day)
            assertTrue(m.isPredictedPeriod)
            assertNull(m.phase)
            assertFalse(m.isFertile)
        }
    }

    @Test
    fun dayAfterLastPredictedPeriodDay_noMarks() {
        val m = mark(d(12, 4))

        assertNull(m.loggedFlow)
        assertNoForecastMarks(m)
        assertFalse(m.isToday)
    }

    @Test
    fun predictedRangeOutsideTypicalLength_notMarked() {
        val prediction = calendar.forecast!!.prediction
        // earliest (Nov 27) .. the day before nextStart is in the range but not a predicted day
        assertEquals(d(11, 27), prediction.earliest)
        var date = prediction.earliest
        while (date.isBefore(prediction.nextStart)) {
            assertFalse(date.toString(), mark(date).isPredictedPeriod)
            date = date.plusDays(1)
        }
    }

    @Test
    fun earlierCycle_loggedFlowOnly() {
        val logged = mark(d(10, 4))
        assertEquals(FlowLevel.LIGHT, logged.loggedFlow)
        assertNoForecastMarks(logged)

        val unlogged = mark(d(10, 20))
        assertNull(unlogged.loggedFlow)
        assertNoForecastMarks(unlogged)
    }

    @Test
    fun onePeriodOnly_placeholderFlaggedAndPhasesPresent() {
        val s = state(days(d(11, 1), d(11, 5)), d(11, 13))

        assertTrue(s.isPlaceholderForecast)
        assertEquals(Phase.MENSTRUAL, s.markOn(d(11, 2)).phase)
        assertEquals(Phase.FOLLICULAR, s.markOn(d(11, 13)).phase)
        assertEquals(Phase.LUTEAL, s.markOn(placeholderNextStart.minusDays(1)).phase)
        assertTrue(s.markOn(placeholderNextStart).isPredictedPeriod)
    }

    @Test
    fun twoPeriods_notPlaceholder() {
        assertFalse(calendar.isPlaceholderForecast)
    }

    @Test
    fun noData_onlyIsTodaySet() {
        val today = d(11, 13)
        val s = state(emptyMap(), today)

        assertNull(s.forecast)
        assertFalse(s.isPlaceholderForecast)
        for (date in listOf(d(11, 1), today, d(11, 29))) {
            val m = s.markOn(date)
            assertNull(m.loggedFlow)
            assertNoForecastMarks(m)
            assertEquals(date == today, m.isToday)
        }
    }

    @Test
    fun todayWhenLate_noPhase() {
        val today = d(12, 2)   // predicted start was Nov 30
        val s = state(days(d(11, 1), d(11, 5)), today)

        assertTrue(s.forecast!!.isLate)
        val m = s.markOn(today)
        assertTrue(m.isToday)
        assertNull(m.phase)
        assertFalse(m.isFertile)
    }

    @Test
    fun late_predictedDaysBeforeTodayNotMarked_todayAndLaterStillMarked() {
        // Nov 1-5 only: predicted Nov 30 - Dec 4 (5 days). Today is two days after the start, so late.
        val start = placeholderNextStart
        val lastDay = start.plusDays(CycleDefaults.PERIOD_LENGTH_DAYS - 1L)
        val today = start.plusDays(2)
        val s = state(days(d(11, 1), d(11, 5)), today)
        assertTrue(s.forecast!!.isLate)

        assertFalse("predicted start", s.markOn(start).isPredictedPeriod)
        assertFalse("yesterday", s.markOn(today.minusDays(1)).isPredictedPeriod)
        assertTrue("today", s.markOn(today).isPredictedPeriod)
        assertTrue("last predicted day", s.markOn(lastDay).isPredictedPeriod)
        assertFalse("day after", s.markOn(lastDay.plusDays(1)).isPredictedPeriod)
    }

    @Test
    fun onPredictedStartDay_notLate_startDayStillMarked() {
        val today = placeholderNextStart
        val s = state(days(d(11, 1), d(11, 5)), today)

        assertFalse(s.forecast!!.isLate)
        assertTrue(s.markOn(today).isPredictedPeriod)
    }

    @Test
    fun everyCurrentCycleDay_hasExactlyOnePhaseMatchingPhaseOn() {
        val forecast = calendar.forecast!!
        var date = forecast.status.lastPeriod.start
        while (date.isBefore(forecast.prediction.nextStart)) {
            val phase = mark(date).phase
            assertNotNull("$date has no phase", phase)
            assertEquals(date.toString(), forecast.phases.phaseOn(date), phase)
            val covering = forecast.phases.ranges.count { !date.isBefore(it.start) && !date.isAfter(it.end) }
            assertEquals(date.toString(), 1, covering)
            date = date.plusDays(1)
        }
        assertNull(mark(forecast.status.lastPeriod.start.minusDays(1)).phase)
    }

    @Test
    fun calendarTodayPhase_matchesHome() {
        val periods = groupIntoPeriods(twoPeriods.keys.toList())
        val home = buildHomeState(periods, d(11, 13), predictor) as HomeUiState.Ready

        assertEquals(home.phases, calendar.forecast!!.phases)
        assertEquals(home.currentPhase, mark(d(11, 13)).phase)
        assertTrue(mark(d(11, 13)).isToday)
    }
}
