package com.example.cycled.ui

import com.example.cycled.data.DailyLog
import com.example.cycled.data.FlowChoice
import com.example.cycled.data.FlowLevel
import com.example.cycled.data.Mood
import com.example.cycled.data.Symptom
import com.example.cycled.domain.CycleForecast
import com.example.cycled.domain.Phase
import java.time.LocalDate

/** What the calendar shows on one day. [style] decides which mark wins when several are set. */
data class DayMark(
    val date: LocalDate,
    val loggedFlow: FlowLevel?,          // set whenever the day is logged, even if it also has a phase
    val phase: Phase?,                   // only in the current cycle, before the predicted next start
    val isPredictedPeriod: Boolean,      // nextStart .. nextStart + periodLengthDays - 1, not logged "no flow"
    val isFertile: Boolean,              // fertileStart .. fertileEnd, current cycle only (screen-reader text)
    val isToday: Boolean,
    val noFlow: Boolean = false,         // logged flow "None" (D7)
    val hasSymptoms: Boolean = false,
    val hasMood: Boolean = false,
) {
    /** The small dot under the day: symptoms or mood logged (D5). */
    val hasLoggedDot: Boolean get() = hasSymptoms || hasMood

    /** The day's look (handoff "Day styling", first match wins). Today's ring is drawn on top. */
    val style: DayStyle
        get() = when {
            loggedFlow != null -> DayStyle.LOGGED_FLOW
            isPredictedPeriod -> DayStyle.PREDICTED_PERIOD
            phase == Phase.OVULATORY -> DayStyle.OVULATORY
            phase == Phase.FOLLICULAR -> DayStyle.FOLLICULAR
            phase == Phase.LUTEAL -> DayStyle.LUTEAL
            else -> DayStyle.PLAIN
        }
}

enum class DayStyle {
    /** Coral fill, sky number. Any flow but None, including Spotting. */
    LOGGED_FLOW,
    /** Dashed coral ring, light coral number. */
    PREDICTED_PERIOD,
    /** Cream fill, sky number. */
    OVULATORY,
    /** Teal fill, white number. */
    FOLLICULAR,
    /** Lavender ring. */
    LUTEAL,
    /** No style (menstrual without flow logged, other cycles, no data). */
    PLAIN,
}

/** Everything the log panel shows for the selected day. */
data class DayPanel(
    val date: LocalDate,
    val isToday: Boolean,
    /** Future days can be viewed but not logged (D6). */
    val isFuture: Boolean,
    /** Flow logged on the day, including old Spotting; drives the period button. */
    val loggedFlow: FlowLevel?,
    /** The Flow chip to show selected; null for nothing or Spotting (D7). */
    val flowChoice: FlowChoice?,
    val symptoms: Set<Symptom>,
    val mood: Mood?,
    /** Cycle day and tide name ("Day 9 · Flood tide"); both null outside the current cycle. */
    val cycleDay: Int?,
    val tideName: String?,
)

data class CalendarUiState(
    val forecast: CycleForecast?,                  // null when nothing is logged on or before today
    val isPlaceholderForecast: Boolean,            // true when the forecast uses the default cycle length
    val loggedDays: Map<LocalDate, FlowLevel>,
    val today: LocalDate,
    val dailyLogs: Map<LocalDate, DailyLog> = emptyMap(),
    /** The day whose log the panel shows; today unless another day was picked. */
    val selected: DayPanel = dayPanel(today, loggedDays, dailyLogs, forecast, today),
) {
    /** The marks for [date]; any date works, inside or outside the visible month. */
    fun markOn(date: LocalDate): DayMark = dayMarkOn(date, loggedDays, forecast, today, dailyLogs)
}
