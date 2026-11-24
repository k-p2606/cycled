package com.example.cycled.ui

import com.example.cycled.data.DailyLog
import com.example.cycled.data.FlowChoice
import com.example.cycled.data.FlowLevel
import com.example.cycled.domain.CycleForecast
import com.example.cycled.domain.CyclePredictor
import com.example.cycled.domain.Period
import com.example.cycled.domain.cycleForecast
import com.example.cycled.domain.phaseOn
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Builds Calendar's state from the logged days, the periods they form, [today], the
 * daily logs (symptoms, mood, "no flow") and the [selected] day for the log panel.
 */
fun buildCalendarState(
    loggedDays: Map<LocalDate, FlowLevel>,
    periods: List<Period>,
    today: LocalDate,
    predictor: CyclePredictor,
    dailyLogs: Map<LocalDate, DailyLog> = emptyMap(),
    selected: LocalDate = today,
): CalendarUiState {
    val forecast = cycleForecast(periods, today, predictor)
    return CalendarUiState(
        forecast = forecast,
        isPlaceholderForecast = forecast?.prediction?.basedOnCycles == 0,
        loggedDays = loggedDays,
        today = today,
        dailyLogs = dailyLogs,
        selected = dayPanel(selected, loggedDays, dailyLogs, forecast, today),
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
 *   A day logged with flow "None" is never predicted (D7).
 * - Earlier cycles and days after the predicted period get nothing beyond logged flow.
 * - Symptoms and mood come from [dailyLogs] (the logged dot, D5).
 */
fun dayMarkOn(
    date: LocalDate,
    loggedDays: Map<LocalDate, FlowLevel>,
    forecast: CycleForecast?,
    today: LocalDate,
    dailyLogs: Map<LocalDate, DailyLog> = emptyMap(),
): DayMark {
    val loggedFlow = loggedDays[date]
    val isToday = date == today
    val log = dailyLogs[date]
    val noFlow = log?.noFlow == true && loggedFlow == null
    val hasSymptoms = log?.symptoms?.isNotEmpty() == true
    val hasMood = log?.mood != null
    if (forecast == null) {
        return DayMark(
            date, loggedFlow, phase = null, isPredictedPeriod = false, isFertile = false, isToday = isToday,
            noFlow = noFlow, hasSymptoms = hasSymptoms, hasMood = hasMood,
        )
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
    val isPredictedPeriod = !date.isBefore(nextStart) && !date.isAfter(predictedEnd) && !isStale && !noFlow

    return DayMark(
        date = date,
        loggedFlow = loggedFlow,
        phase = if (inCurrentCycle) phases.phaseOn(date) else null,
        isPredictedPeriod = isPredictedPeriod,
        isFertile = isFertile,
        isToday = isToday,
        noFlow = noFlow,
        hasSymptoms = hasSymptoms,
        hasMood = hasMood,
    )
}

/**
 * The log panel for [date]. Cycle day and tide name are shown for the current cycle:
 * from its start to the day before the predicted next start, and while late, on up to
 * today ("Slack water", D2). Other days get neither.
 */
fun dayPanel(
    date: LocalDate,
    loggedDays: Map<LocalDate, FlowLevel>,
    dailyLogs: Map<LocalDate, DailyLog>,
    forecast: CycleForecast?,
    today: LocalDate,
): DayPanel {
    val loggedFlow = loggedDays[date]
    val log = dailyLogs[date]

    var cycleDay: Int? = null
    var tideName: String? = null
    if (forecast != null) {
        val start = forecast.status.lastPeriod.start
        val nextStart = forecast.prediction.nextStart
        val day = ChronoUnit.DAYS.between(start, date).toInt() + 1
        when {
            date.isBefore(start) -> Unit
            date.isBefore(nextStart) -> {
                cycleDay = day
                tideName = forecast.phases.phaseOn(date)?.tideName ?: SLACK_WATER
            }
            !date.isAfter(today) -> {
                // On or after the predicted start, up to today: no new period yet.
                cycleDay = day
                tideName = SLACK_WATER
            }
        }
    }

    return DayPanel(
        date = date,
        isToday = date == today,
        isFuture = date.isAfter(today),
        loggedFlow = loggedFlow,
        flowChoice = FlowChoice.of(loggedFlow, log?.noFlow == true),
        symptoms = log?.symptoms.orEmpty(),
        mood = log?.mood,
        cycleDay = cycleDay,
        tideName = tideName,
    )
}
