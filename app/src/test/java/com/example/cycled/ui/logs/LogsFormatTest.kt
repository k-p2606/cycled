package com.example.cycled.ui.logs

import com.example.cycled.data.FlowLevel
import com.example.cycled.data.Mood
import com.example.cycled.data.Symptom
import com.example.cycled.domain.CycleLog
import com.example.cycled.domain.LoggedDay
import com.example.cycled.ui.LogsUiState
import com.example.cycled.ui.SampleData
import com.example.cycled.ui.buildLogsState
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LogsFormatTest {

    private val uk = Locale.UK
    private fun d(month: Int, day: Int) = LocalDate.of(2026, month, day)

    private val cycles =
        (buildLogsState(SampleData.periods, SampleData.periodDays, SampleData.dailyLogs, SampleData.today) as LogsUiState.Ready).cycles
    private val current = cycles[0]
    private val october = cycles[1]

    private fun day(flow: FlowLevel? = null, noFlow: Boolean = false, symptoms: Set<Symptom> = emptySet(), mood: Mood? = null) =
        LoggedDay(d(11, 4), cycleDay = 5, flow = flow, noFlow = noFlow, symptoms = symptoms, mood = mood)

    @Test
    fun tabs() {
        assertEquals("Current" to "Day 9", LogsFormat.tab(current, uk))
        assertEquals("3 Oct" to "28 days", LogsFormat.tab(october, uk))
        assertEquals("Cycle from 3 October, 28 days", LogsFormat.tabSpoken(october, uk))
    }

    @Test
    fun headingAndSubline_matchTheDesign() {
        assertEquals("31 Oct – today", LogsFormat.heading(current, uk))
        assertEquals("Day 9 so far · period 5 days · 5 days logged", LogsFormat.subline(current))
        assertEquals("3 Oct – 30 Oct", LogsFormat.heading(october, uk))
        assertEquals("28-day cycle · period 5 days · 8 days logged", LogsFormat.subline(october))
    }

    @Test
    fun subline_singular() {
        val one = CycleLog(d(11, 8), null, lengthDays = 1, periodDays = 1, days = listOf(day(FlowLevel.LIGHT)))
        assertEquals("Day 1 so far · period 1 day · 1 day logged", LogsFormat.subline(one))
    }

    @Test
    fun rowLines() {
        assertEquals("Heavy flow", LogsFormat.flowLine(day(FlowLevel.HEAVY)))
        assertEquals("Spotting", LogsFormat.flowLine(day(FlowLevel.SPOTTING)))
        assertEquals("No flow", LogsFormat.flowLine(day(noFlow = true)))
        assertNull(LogsFormat.flowLine(day(mood = Mood.LOW)))

        assertEquals(
            "Cramps · Tired · Mood: low",
            LogsFormat.detailLine(day(symptoms = setOf(Symptom.FATIGUE, Symptom.CRAMPS), mood = Mood.LOW), uk),
        )
        assertEquals("Mood: bright", LogsFormat.detailLine(day(mood = Mood.HAPPY), uk))
        assertNull(LogsFormat.detailLine(day(FlowLevel.LIGHT), uk))
    }

    @Test
    fun rowSpoken() {
        assertEquals(
            "Wednesday 4 November, light flow, mood: calm, day 5 of the cycle",
            LogsFormat.rowSpoken(day(FlowLevel.LIGHT, mood = Mood.CALM), uk),
        )
    }

    @Test
    fun noPeriods_isNoData() {
        assertEquals(LogsUiState.NoData, buildLogsState(emptyList(), emptyMap(), SampleData.dailyLogs, SampleData.today))
    }
}
