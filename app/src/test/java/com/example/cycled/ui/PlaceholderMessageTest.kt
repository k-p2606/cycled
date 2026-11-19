package com.example.cycled.ui

import com.example.cycled.data.FlowLevel
import com.example.cycled.domain.AverageCyclePredictor
import com.example.cycled.domain.MlCyclePredictor
import com.example.cycled.domain.Period
import com.example.cycled.domain.TestAssets
import com.example.cycled.domain.groupIntoPeriods
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceholderMessageTest {

    private fun d(month: Int, day: Int) = LocalDate.of(2026, month, day)

    private val onePeriod = listOf(Period(d(11, 1), d(11, 5)))

    @Test
    fun mlModel_noHistory_showsItsPredictedCycleLength() {
        val state = buildHomeState(onePeriod, d(11, 13), MlCyclePredictor(TestAssets.model())) as HomeUiState.Ready

        assertEquals(0, state.prediction.basedOnCycles)
        assertEquals(30, state.cycleLengthDays)
        assertEquals(
            "Based on a typical 30-day cycle until you've logged two periods",
            CycleMessages.placeholderBasis(state.cycleLengthDays),
        )
    }

    @Test
    fun calendarNote_usesTheForecastsCycleLength() {
        val logged = (1..5).associate { d(11, it) to FlowLevel.MEDIUM }
        val periods = groupIntoPeriods(logged.keys.toList())
        val state = buildCalendarState(logged, periods, d(11, 13), AverageCyclePredictor())

        assertEquals(29, state.forecast!!.cycleLengthDays)
        assertEquals(
            "Predictions use a typical 29-day cycle until you've logged two periods.",
            CycleMessages.placeholderNote(state.forecast!!.cycleLengthDays),
        )
    }
}
