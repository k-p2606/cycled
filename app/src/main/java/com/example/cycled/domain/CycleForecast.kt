package com.example.cycled.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Everything predicted about the current cycle, shared by Home and Calendar so the
 * two screens always agree.
 */
data class CycleForecast(
    val status: CycleStatus,          // from cycleStatus(); status.lastPeriod starts the current cycle
    val prediction: Prediction,
    val phases: CyclePhases,          // includes the fertile window
    val periodLengthDays: Int,        // from the prediction; used for phases and predicted period days
    val isLate: Boolean,              // true when today is after prediction.nextStart
) {
    /** Predicted length of the current cycle: from its first day to the predicted next start. */
    val cycleLengthDays: Int
        get() = ChronoUnit.DAYS.between(status.lastPeriod.start, prediction.nextStart).toInt()
}

/**
 * Builds the [CycleForecast] for [today] from the logged [periods], or returns null
 * when no period has started on or before [today].
 *
 * The next start, range, period length and ovulation all come from [predictor]; the
 * current period keeps its logged end and the "may still be going" rule in [cyclePhases].
 */
fun cycleForecast(periods: List<Period>, today: LocalDate, predictor: CyclePredictor): CycleForecast? {
    val status = cycleStatus(periods, today) ?: return null
    val prediction = predictor.predict(periods) ?: return null

    return CycleForecast(
        status = status,
        prediction = prediction,
        phases = cyclePhases(
            lastPeriod = status.lastPeriod,
            nextStart = prediction.nextStart,
            ovulation = prediction.ovulation,
            periodLengthDays = prediction.periodLengthDays,
            today = today,
        ),
        periodLengthDays = prediction.periodLengthDays,
        isLate = today.isAfter(prediction.nextStart),
    )
}
