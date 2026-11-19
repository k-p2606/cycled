package com.example.cycled.ui

import com.example.cycled.domain.AverageCyclePredictor
import com.example.cycled.domain.CycleDefaults
import com.example.cycled.domain.Period
import com.example.cycled.domain.Phase
import com.example.cycled.domain.PhaseRange
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeStateBuilderTest {

    private fun d(month: Int, day: Int) = LocalDate.of(2026, month, day)

    private val predictor = AverageCyclePredictor()

    private val nov1to5 = Period(d(11, 1), d(11, 5))

    /** Next start for nov1to5 alone, from the placeholder cycle length (Nov 30). */
    private val placeholderNextStart = d(11, 1).plusDays(CycleDefaults.CYCLE_LENGTH_DAYS.toLong())

    private fun ready(periods: List<Period>, today: LocalDate): HomeUiState.Ready {
        val state = buildHomeState(periods, today, predictor)
        assertTrue("expected Ready but was $state", state is HomeUiState.Ready)
        return state as HomeUiState.Ready
    }

    @Test
    fun noPeriods_isNoData() {
        assertEquals(HomeUiState.NoData, buildHomeState(emptyList(), d(11, 13), predictor))
    }

    @Test
    fun onePeriod_midCycle() {
        val state = ready(listOf(nov1to5), d(11, 13))

        assertEquals(13, state.status.cycleDay)
        assertEquals(placeholderNextStart, state.prediction.nextStart)
        assertEquals(0, state.prediction.basedOnCycles)
        assertEquals(Phase.FOLLICULAR, state.currentPhase)
        assertFalse(state.isLate)
    }

    @Test
    fun twoPeriods_predictionUsesCycleLength() {
        val state = ready(listOf(Period(d(10, 3), d(10, 7)), nov1to5), d(11, 13))

        assertEquals(d(11, 30), state.prediction.nextStart)
        assertEquals(1, state.prediction.basedOnCycles)
        assertEquals(Phase.FOLLICULAR, state.currentPhase)
    }

    @Test
    fun ongoingPeriod_menstrualUsesPastPeriodLength() {
        val periods = listOf(Period(d(10, 3), d(10, 5)), Period(d(11, 13), d(11, 13)))

        val state = ready(periods, d(11, 13))

        assertEquals(
            PhaseRange(Phase.MENSTRUAL, d(11, 13), d(11, 15)),
            state.phases.ranges.first { it.phase == Phase.MENSTRUAL },
        )
        assertEquals(Phase.MENSTRUAL, state.currentPhase)
    }

    @Test
    fun onPredictedStartDay_noPhaseAndNotLate() {
        val state = ready(listOf(nov1to5), placeholderNextStart)

        assertNull(state.currentPhase)
        assertFalse(state.isLate)
    }

    @Test
    fun afterPredictedStartDay_noPhaseAndLate() {
        val state = ready(listOf(nov1to5), d(12, 2))

        assertNull(state.currentPhase)
        assertTrue(state.isLate)
    }
}
