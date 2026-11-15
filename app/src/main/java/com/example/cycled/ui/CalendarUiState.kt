package com.example.cycled.ui

import com.example.cycled.data.FlowLevel
import com.example.cycled.domain.CycleForecast
import com.example.cycled.domain.Phase
import java.time.LocalDate

/** What the calendar shows on one day. Drawing decides which marks win when several are set. */
data class DayMark(
    val date: LocalDate,
    val loggedFlow: FlowLevel?,          // set whenever the day is logged, even if it also has a phase
    val phase: Phase?,                   // only in the current cycle, before the predicted next start
    val isPredictedPeriod: Boolean,      // nextStart .. nextStart + periodLengthDays - 1
    val isFertile: Boolean,              // fertileStart .. fertileEnd, current cycle only
    val isToday: Boolean,
)

data class CalendarUiState(
    val forecast: CycleForecast?,                  // null when nothing is logged on or before today
    val isPlaceholderForecast: Boolean,            // true when the forecast uses the default cycle length
    val loggedDays: Map<LocalDate, FlowLevel>,
    val today: LocalDate,
) {
    /** The marks for [date]; any date works, inside or outside the visible month. */
    fun markOn(date: LocalDate): DayMark = dayMarkOn(date, loggedDays, forecast, today)
}
