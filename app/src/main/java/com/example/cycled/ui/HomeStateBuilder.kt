package com.example.cycled.ui

import com.example.cycled.data.DailyLog
import com.example.cycled.domain.CyclePredictor
import com.example.cycled.domain.DayNotes
import com.example.cycled.domain.Period
import com.example.cycled.domain.Recommendation
import com.example.cycled.domain.RecommendationCatalog
import com.example.cycled.domain.TideModel
import com.example.cycled.domain.cycleForecast
import com.example.cycled.domain.phaseOn
import com.example.cycled.domain.recContext
import com.example.cycled.domain.recommendationsFor
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Builds everything Home shows from the logged [periods] and [today]'s date:
 * the current cycle day, the predicted next period, the cycle's phases, and what
 * the tide, moon and "coming up" rows need. Every date comes from cycleForecast() (D1).
 *
 * "For today" tips come from [catalog] for the forecast's phase and late flag plus the
 * symptoms and moods in [dailyLogs] from the last 7 days.
 */
fun buildHomeState(
    periods: List<Period>,
    today: LocalDate,
    predictor: CyclePredictor,
    dailyLogs: Map<LocalDate, DailyLog> = emptyMap(),
    catalog: List<Recommendation> = RecommendationCatalog,
): HomeUiState {
    val forecast = cycleForecast(periods, today, predictor) ?: return HomeUiState.NoData
    val cycleStart = forecast.status.lastPeriod.start
    fun cycleDay(date: LocalDate) = ChronoUnit.DAYS.between(cycleStart, date).toInt() + 1

    val prediction = forecast.prediction
    val phases = forecast.phases
    val currentPhase = phases.phaseOn(today)
    val cycleLength = forecast.cycleLengthDays
    val ovulationDay = cycleDay(phases.ovulation)
    val cycleDay = forecast.status.cycleDay
    val pastCycle = cycleDay > cycleLength
    val fertile = DateSpan(phases.fertileStart, phases.fertileEnd)
    val lastWeek = dailyLogs
        .filterKeys { !it.isAfter(today) && it.isAfter(today.minusDays(7)) }
        .mapValues { (_, log) -> DayNotes(log.noFlow, log.symptoms, log.mood) }
    val tips = recommendationsFor(recContext(currentPhase, forecast.isLate, lastWeek, today), catalog)

    return HomeUiState.Ready(
        status = forecast.status,
        prediction = prediction,
        phases = phases,
        currentPhase = currentPhase,
        isLate = forecast.isLate,
        today = today,
        tideName = currentPhase?.tideName ?: SLACK_WATER,
        phaseSubline = when {
            currentPhase != null -> "${currentPhase.displayName} phase"
            forecast.isLate -> CycleMessages.PERIOD_MAY_BE_LATE
            else -> CycleMessages.PERIOD_EXPECTED_TODAY
        },
        ovulationCycleDay = ovulationDay,
        phaseDays = phases.ranges.map { PhaseDays(it.phase, cycleDay(it.start), cycleDay(it.end)) },
        fertileWindow = fertile,
        comingUp = listOf(
            ComingUpRow(
                ComingUpKind.NEXT_PERIOD,
                DateSpan(prediction.nextStart, prediction.nextStart),
                untilText(today, prediction.nextStart),
            ),
            ComingUpRow(
                ComingUpKind.FERTILE_WINDOW,
                fertile,
                if (today in fertile) ComingUpRow.NOW else untilText(today, fertile.start),
            ),
            ComingUpRow(
                ComingUpKind.HIGH_TIDE,
                DateSpan(phases.ovulation, phases.ovulation),
                untilText(today, phases.ovulation),
            ),
        ),
        likelyRange = DateSpan(prediction.earliest, prediction.latest),
        moonPhase = if (pastCycle) 0.0 else TideModel(cycleLength, ovulationDay).phase(cycleDay.toDouble()),
        markerCycleDay = minOf(cycleDay, cycleLength),
        recommendations = tips.top3,
        allRecommendations = tips.byCategory.values.toList(),
        lateTip = tips.lateTip,
    )
}

/** "today", "tomorrow", "in N days", or "passed" once [date] is behind [today]. */
fun untilText(today: LocalDate, date: LocalDate): String =
    when (val days = ChronoUnit.DAYS.between(today, date)) {
        in Long.MIN_VALUE..-1L -> ComingUpRow.PASSED
        0L -> "today"
        1L -> "tomorrow"
        else -> "in $days days"
    }
