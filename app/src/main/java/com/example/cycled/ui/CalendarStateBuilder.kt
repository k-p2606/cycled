package com.example.cycled.ui

import com.example.cycled.data.FlowLevel
import com.example.cycled.domain.CycleForecast
import com.example.cycled.domain.CyclePredictor
import com.example.cycled.domain.Period
import com.example.cycled.domain.cycleForecast
import com.example.cycled.domain.phaseOn
import java.time.LocalDate

/** Builds Calendar's state from the logged days, the periods they form, and [today]. */
fun buildCalendarState(
    loggedDays: Map<LocalDate, FlowLevel>,
    periods: List<Period>,
    today: LocalDate,
    predictor: CyclePredictor,
): CalendarUiState {
    val forecast = cycleForecast(periods, today, predictor)
    return CalendarUiState(
        forecast = forecast,
        isPlaceholderForecast = forecast?.prediction?.basedOnCycles == 0,
        loggedDays = loggedDays,
        today = today,
    )
}

/**
 * Works out the marks for [date].
 *
 * - Logged flow is always reported.
 * - Only the current cycle (latest period start up to the day before the predicted
 *   next start) gets a phase and fertile marks, matching Home's phaseOn().
 * - Predicted period days run from the predicted next start for the typical period
 *   length; the rest of the earliest..latest range is not marked. When the period is
 *   late, predicted days before [today] are not marked; today and later still are.
 * - Earlier cycles and days after the predicted period get nothing beyond logged flow.
 */
fun dayMarkOn(
    date: LocalDate,
    loggedDays: Map<LocalDate, FlowLevel>,
    forecast: CycleForecast?,
    today: LocalDate,
): DayMark {
    val loggedFlow = loggedDays[date]
    val isToday = date == today
    if (forecast == null) {
        return DayMark(date, loggedFlow, phase = null, isPredictedPeriod = false, isFertile = false, isToday = isToday)
    }

    val cycleStart = forecast.status.lastPeriod.start
    val nextStart = forecast.prediction.nextStart
    val inCurrentCycle = !date.isBefore(cycleStart) && date.isBefore(nextStart)
    val phases = forecast.phases

    val isFertile = inCurrentCycle &&
        !date.isBefore(phases.fertileStart) && !date.isAfter(phases.fertileEnd)
    val predictedEnd = nextStart.plusDays(forecast.periodLengthDays - 1L)
    // When late, predicted days already behind today are stale and are dropped.
    val isStale = forecast.isLate && date.isBefore(today)
    val isPredictedPeriod = !date.isBefore(nextStart) && !date.isAfter(predictedEnd) && !isStale

    return DayMark(
        date = date,
        loggedFlow = loggedFlow,
        phase = if (inCurrentCycle) phases.phaseOn(date) else null,
        isPredictedPeriod = isPredictedPeriod,
        isFertile = isFertile,
        isToday = isToday,
    )
}
