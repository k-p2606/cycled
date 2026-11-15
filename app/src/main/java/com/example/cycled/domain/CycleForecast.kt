package com.example.cycled.domain

import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Everything predicted about the current cycle, shared by Home and Calendar so the
 * two screens always agree.
 */
data class CycleForecast(
    val status: CycleStatus,          // from cycleStatus(); status.lastPeriod starts the current cycle
    val prediction: Prediction,
    val phases: CyclePhases,          // includes the fertile window
    val periodLengthDays: Int,        // typical period length used for phases and predicted period days
    val isLate: Boolean,              // true when today is after prediction.nextStart
)

/**
 * Builds the [CycleForecast] for [today] from the logged [periods], or returns null
 * when no period has started on or before [today].
 *
 * The typical period length is the average length of up to
 * [CycleDefaults.MAX_CYCLES_USED] periods before the current one (which may still be
 * going), or [CycleDefaults.PERIOD_LENGTH_DAYS] when there are none.
 */
fun cycleForecast(periods: List<Period>, today: LocalDate, predictor: CyclePredictor): CycleForecast? {
    val status = cycleStatus(periods, today) ?: return null
    val prediction = predictor.predict(periods.map { it.start }) ?: return null

    val pastLengths = (periods - status.lastPeriod)
        .sortedBy { it.start }
        .takeLast(CycleDefaults.MAX_CYCLES_USED)
        .map { it.lengthDays }
    val periodLengthDays =
        if (pastLengths.isEmpty()) CycleDefaults.PERIOD_LENGTH_DAYS else pastLengths.average().roundToInt()

    return CycleForecast(
        status = status,
        prediction = prediction,
        phases = cyclePhases(status.lastPeriod, prediction.nextStart, today, periodLengthDays),
        periodLengthDays = periodLengthDays,
        isLate = today.isAfter(prediction.nextStart),
    )
}
