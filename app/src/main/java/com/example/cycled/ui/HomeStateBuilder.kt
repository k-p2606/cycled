package com.example.cycled.ui

import com.example.cycled.domain.CyclePredictor
import com.example.cycled.domain.Period
import com.example.cycled.domain.cycleForecast
import com.example.cycled.domain.phaseOn
import java.time.LocalDate

/**
 * Builds everything Home shows from the logged [periods] and [today]'s date:
 * the current cycle day, the predicted next period, and the cycle's phases.
 */
fun buildHomeState(periods: List<Period>, today: LocalDate, predictor: CyclePredictor): HomeUiState {
    val forecast = cycleForecast(periods, today, predictor) ?: return HomeUiState.NoData

    return HomeUiState.Ready(
        status = forecast.status,
        prediction = forecast.prediction,
        phases = forecast.phases,
        currentPhase = forecast.phases.phaseOn(today),
        isLate = forecast.isLate,
    )
}
