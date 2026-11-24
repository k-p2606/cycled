package com.example.cycled.ui.logs

import com.example.cycled.domain.CycleStats
import com.example.cycled.domain.groupIntoPeriods
import com.example.cycled.ui.LogsUiState
import com.example.cycled.ui.SampleData
import com.example.cycled.ui.buildLogsState
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LogsSummaryTest {

    private fun ready(state: LogsUiState) = state as LogsUiState.Ready

    @Test
    fun builder_fillsTheSummaryFromSampleData() {
        val state = ready(buildLogsState(SampleData.periods, SampleData.periodDays, SampleData.dailyLogs, SampleData.today))

        assertEquals(CycleStats(averageCycleDays = 28, averagePeriodDays = 5, variationDays = 1, cyclesUsed = 3), state.summary)
    }

    @Test
    fun builder_onePeriod_hasCyclesButNoSummary() {
        val days = SampleData.periodDays.filterKeys { it >= LocalDate.of(2026, 10, 31) }
        val state = ready(buildLogsState(groupIntoPeriods(days.keys.toList()), days, emptyMap(), SampleData.today))

        assertEquals(1, state.cycles.size)
        assertNull(state.summary)
    }

    @Test
    fun builder_oneCompletedCycle_hasNoVariation() {
        val days = SampleData.periodDays.filterKeys { it >= LocalDate.of(2026, 10, 1) }
        val summary = ready(buildLogsState(groupIntoPeriods(days.keys.toList()), days, emptyMap(), SampleData.today)).summary!!

        assertEquals(28, summary.averageCycleDays)
        assertNull(summary.variationDays)
        assertEquals(1, summary.cyclesUsed)
    }

    @Test
    fun items_matchTheDesign() {
        val stats = CycleStats(averageCycleDays = 28, averagePeriodDays = 5, variationDays = 1, cyclesUsed = 3)
        assertEquals(
            listOf("28 days" to "avg cycle", "5 days" to "avg period", "±1 day" to "varies by"),
            LogsFormat.summaryItems(stats),
        )
        assertEquals(
            "Over your last 3 cycles: average cycle 28 days, average period 5 days, varies by about 1 day.",
            LogsFormat.summarySpoken(stats),
        )
    }

    @Test
    fun nullItems_areLeftOut() {
        val stats = CycleStats(averageCycleDays = 30, averagePeriodDays = null, variationDays = null, cyclesUsed = 1)
        assertEquals(listOf("30 days" to "avg cycle"), LogsFormat.summaryItems(stats))
        assertEquals("Over your last cycle: average cycle 30 days.", LogsFormat.summarySpoken(stats))
    }

    @Test
    fun plurals_andNoVariation() {
        val stats = CycleStats(averageCycleDays = 28, averagePeriodDays = 1, variationDays = 0, cyclesUsed = 2)
        assertEquals(
            listOf("28 days" to "avg cycle", "1 day" to "avg period", "±0 days" to "varies by"),
            LogsFormat.summaryItems(stats),
        )
        assertEquals(
            "Over your last 2 cycles: average cycle 28 days, average period 1 day, varies by less than a day.",
            LogsFormat.summarySpoken(stats),
        )
    }
}
