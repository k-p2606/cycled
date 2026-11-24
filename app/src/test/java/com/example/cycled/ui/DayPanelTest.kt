package com.example.cycled.ui

import com.example.cycled.data.DailyLog
import com.example.cycled.data.FlowChoice
import com.example.cycled.data.FlowLevel
import com.example.cycled.data.Mood
import com.example.cycled.data.Symptom
import com.example.cycled.domain.AverageCyclePredictor
import com.example.cycled.domain.groupIntoPeriods
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Calendar's log panel for the selected day. Sample history (D12): current cycle from
 * 31 Oct, next start 28 Nov, ovulation 14 Nov (rule-based predictor).
 */
class DayPanelTest {

    private fun d(month: Int, day: Int) = LocalDate.of(2026, month, day)

    private fun state(
        selected: LocalDate,
        today: LocalDate = SampleData.today,
        days: Map<LocalDate, FlowLevel> = SampleData.periodDays,
        logs: Map<LocalDate, DailyLog> = SampleData.dailyLogs,
    ) = buildCalendarState(days, groupIntoPeriods(days.keys.toList()), today, AverageCyclePredictor(), logs, selected)

    @Test
    fun defaultSelection_isToday() {
        val s = buildCalendarState(SampleData.periodDays, SampleData.periods, SampleData.today, AverageCyclePredictor())
        assertEquals(SampleData.today, s.selected.date)
        assertTrue(s.selected.isToday)
    }

    @Test
    fun today_showsCycleDayAndTide() {
        val panel = state(SampleData.today).selected
        assertEquals(9, panel.cycleDay)
        assertEquals("Flood tide", panel.tideName)
        assertFalse(panel.isFuture)
        assertNull(panel.loggedFlow)
        assertNull(panel.flowChoice)
    }

    @Test
    fun loggedDay_hasFlowSymptomsAndMood() {
        val panel = state(d(10, 31)).selected
        assertEquals(FlowLevel.MEDIUM, panel.loggedFlow)
        assertEquals(FlowChoice.MEDIUM, panel.flowChoice)
        assertEquals(setOf(Symptom.CRAMPS), panel.symptoms)
        assertEquals(Mood.LOW, panel.mood)
        assertEquals(1, panel.cycleDay)
        assertEquals("Low water", panel.tideName)
    }

    @Test
    fun earlierCycle_hasNoCycleDay() {
        val panel = state(d(10, 15)).selected
        assertNull(panel.cycleDay)
        assertNull(panel.tideName)
        assertEquals(setOf(Symptom.HEADACHE), panel.symptoms)
    }

    @Test
    fun futureDay_inCycle_isViewableButFuture() {
        val panel = state(d(11, 14)).selected
        assertTrue(panel.isFuture)
        assertEquals(15, panel.cycleDay)
        assertEquals("High water", panel.tideName)
    }

    @Test
    fun futureDay_pastThePrediction_hasNoCycleDay() {
        val panel = state(d(12, 5)).selected
        assertTrue(panel.isFuture)
        assertNull(panel.cycleDay)
    }

    @Test
    fun late_daysFromPredictedStartToToday_areSlackWater() {
        val today = d(12, 2)
        assertEquals("Slack water", state(today, today).selected.tideName)
        assertEquals(33, state(today, today).selected.cycleDay)
        assertEquals("Slack water", state(d(11, 29), today).selected.tideName)
        assertNull(state(d(12, 3), today).selected.cycleDay)
    }

    @Test
    fun noFlow_selectsNone_andSpottingSelectsNothing() {
        val logs = mapOf(d(11, 6) to DailyLog(d(11, 6), noFlow = true))
        assertEquals(FlowChoice.NONE, state(d(11, 6), logs = logs).selected.flowChoice)

        val days = SampleData.periodDays + (d(11, 6) to FlowLevel.SPOTTING)
        val spotting = state(d(11, 6), days = days).selected
        assertEquals(FlowLevel.SPOTTING, spotting.loggedFlow)
        assertNull(spotting.flowChoice)
    }

    @Test
    fun noData_panelHasNoCycleDay() {
        val panel = state(SampleData.today, days = emptyMap(), logs = emptyMap()).selected
        assertNull(panel.cycleDay)
        assertNull(panel.tideName)
        assertTrue(panel.symptoms.isEmpty())
    }
}
