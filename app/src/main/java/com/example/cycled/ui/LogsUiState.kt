package com.example.cycled.ui

import com.example.cycled.data.DailyLog
import com.example.cycled.data.FlowLevel
import com.example.cycled.domain.CycleLog
import com.example.cycled.domain.CycleStats
import com.example.cycled.domain.DayNotes
import com.example.cycled.domain.Period
import com.example.cycled.domain.cycleLogs
import com.example.cycled.domain.cycleStats
import java.time.LocalDate

sealed interface LogsUiState {
    data object Loading : LogsUiState                  // before the database has answered
    data object NoData : LogsUiState                   // no period logged yet: tabs hidden (D10)
    data class Ready(
        /** Newest first; the first is the current cycle. */
        val cycles: List<CycleLog>,
        val today: LocalDate,
        /** Averages over her recent cycles; null until a cycle has been completed. */
        val summary: CycleStats? = null,
    ) : LogsUiState
}

/** Builds Logs from the logged periods, period days and daily logs (D10), with the stats summary. */
fun buildLogsState(
    periods: List<Period>,
    periodDays: Map<LocalDate, FlowLevel>,
    dailyLogs: Map<LocalDate, DailyLog>,
    today: LocalDate,
): LogsUiState {
    val notes = dailyLogs.mapValues { (_, log) -> DayNotes(log.noFlow, log.symptoms, log.mood) }
    val cycles = cycleLogs(periods, periodDays, notes, today)
    return if (cycles.isEmpty()) LogsUiState.NoData else LogsUiState.Ready(cycles, today, cycleStats(periods, today))
}
