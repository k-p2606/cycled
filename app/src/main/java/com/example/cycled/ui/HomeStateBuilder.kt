package com.example.cycled.ui

import com.example.cycled.domain.CycleDefaults
import com.example.cycled.domain.CyclePredictor
import com.example.cycled.domain.Period
import com.example.cycled.domain.cyclePhases
import com.example.cycled.domain.cycleStatus
import com.example.cycled.domain.phaseOn
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Builds everything Home shows from the logged [periods] and [today]'s date:
 * the current cycle day, the predicted next period, and the cycle's phases.
 */
fun buildHomeState(periods: List<Period>, today: LocalDate, predictor: CyclePredictor): HomeUiState {
    val status = cycleStatus(periods, today) ?: return HomeUiState.NoData
    val prediction = predictor.predict(periods.map { it.start }) ?: return HomeUiState.NoData

    val pastLengths = (periods - status.lastPeriod)
        .sortedBy { it.start }
        .takeLast(CycleDefaults.MAX_CYCLES_USED)
        .map { it.lengthDays }
    val periodLengthDays =
        if (pastLengths.isEmpty()) CycleDefaults.PERIOD_LENGTH_DAYS else pastLengths.average().roundToInt()

    val phases = cyclePhases(status.lastPeriod, prediction.nextStart, today, periodLengthDays)

    return HomeUiState.Ready(
        status = status,
        prediction = prediction,
        phases = phases,
        currentPhase = phases.phaseOn(today),
        isLate = today.isAfter(prediction.nextStart),
    )
}
