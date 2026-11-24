package com.example.cycled.ui

import com.example.cycled.domain.AverageCyclePredictor
import com.example.cycled.domain.Period
import com.example.cycled.domain.Phase
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The fields buildHomeState() adds for the redesigned Home (handoff D2-D4), using the
 * handoff's sample history (D12) and the rule-based predictor:
 * starts 9 Aug, 7 Sep, 3 Oct, 31 Oct → average 28 days, next start 28 Nov (± 3),
 * ovulation 14 Nov (cycle day 15), phases: period 1-5, follicular 6-13,
 * ovulatory 14-16, luteal 17-28, fertile 9-15 Nov.
 */
class HomeStateTideTest {

    private fun d(month: Int, day: Int) = LocalDate.of(2026, month, day)

    private val predictor = AverageCyclePredictor()

    private val sample = listOf(
        Period(d(8, 9), d(8, 13)),
        Period(d(9, 7), d(9, 11)),
        Period(d(10, 3), d(10, 7)),
        Period(d(10, 31), d(11, 4)),
    )

    private fun ready(today: LocalDate, periods: List<Period> = sample): HomeUiState.Ready {
        val state = buildHomeState(periods, today, predictor)
        assertTrue("expected Ready but was $state", state is HomeUiState.Ready)
        return state as HomeUiState.Ready
    }

    private fun HomeUiState.Ready.until(kind: ComingUpKind) = comingUp.single { it.kind == kind }.until

    @Test
    fun sampleDay9_matchesTheDesign() {
        val state = ready(d(11, 8))

        assertEquals(d(11, 8), state.today)
        assertEquals("Flood tide", state.tideName)
        assertEquals("Follicular phase", state.phaseSubline)
        assertEquals(28, state.cycleLengthDays)
        assertEquals(15, state.ovulationCycleDay)
        assertEquals(
            listOf(
                PhaseDays(Phase.MENSTRUAL, 1, 5),
                PhaseDays(Phase.FOLLICULAR, 6, 13),
                PhaseDays(Phase.OVULATORY, 14, 16),
                PhaseDays(Phase.LUTEAL, 17, 28),
            ),
            state.phaseDays,
        )
        assertEquals(DateSpan(d(11, 9), d(11, 15)), state.fertileWindow)
        assertEquals(DateSpan(d(11, 25), d(12, 1)), state.likelyRange)
        assertEquals(9, state.markerCycleDay)
        assertEquals(0.5 * 8 / 14, state.moonPhase, 1e-9)
    }

    @Test
    fun sampleDay9_comingUpRows() {
        val state = ready(d(11, 8))

        assertEquals(
            listOf(
                ComingUpRow(ComingUpKind.NEXT_PERIOD, DateSpan(d(11, 28), d(11, 28)), "in 20 days"),
                ComingUpRow(ComingUpKind.FERTILE_WINDOW, DateSpan(d(11, 9), d(11, 15)), "tomorrow"),
                ComingUpRow(ComingUpKind.HIGH_TIDE, DateSpan(d(11, 14), d(11, 14)), "in 6 days"),
            ),
            state.comingUp,
        )
        assertFalse(state.comingUp.any { it.isPassed })
    }

    @Test
    fun insideFertileWindow_isNow() {
        assertEquals("now", ready(d(11, 10)).until(ComingUpKind.FERTILE_WINDOW))
        assertEquals("now", ready(d(11, 15)).until(ComingUpKind.FERTILE_WINDOW))
    }

    @Test
    fun ovulationDay_isHighWaterAndFullMoon() {
        val state = ready(d(11, 14))

        assertEquals(Phase.OVULATORY, state.currentPhase)
        assertEquals("High water", state.tideName)
        assertEquals("Ovulatory phase", state.phaseSubline)
        assertEquals("today", state.until(ComingUpKind.HIGH_TIDE))
        assertEquals(0.5, state.moonPhase, 1e-9)
    }

    @Test
    fun luteal_isEbbTideWithPassedRows() {
        val state = ready(d(11, 20))

        assertEquals("Ebb tide", state.tideName)
        assertEquals("Luteal phase", state.phaseSubline)
        assertEquals("in 8 days", state.until(ComingUpKind.NEXT_PERIOD))
        assertEquals("passed", state.until(ComingUpKind.FERTILE_WINDOW))
        assertEquals("passed", state.until(ComingUpKind.HIGH_TIDE))
        assertTrue(state.moonPhase > 0.5 && state.moonPhase < 1.0)
    }

    @Test
    fun menstrual_isLowWater() {
        val state = ready(d(11, 2))
        assertEquals("Low water", state.tideName)
        assertEquals("Menstrual phase", state.phaseSubline)
    }

    @Test
    fun late_isSlackWaterWithNewMoonAndClampedMarker() {
        val state = ready(d(12, 2))

        assertTrue(state.isLate)
        assertEquals(33, state.status.cycleDay)
        assertEquals("Slack water", state.tideName)
        assertEquals(CycleMessages.PERIOD_MAY_BE_LATE, state.phaseSubline)
        assertEquals(0.0, state.moonPhase, 0.0)
        assertEquals(28, state.markerCycleDay)
        assertTrue(state.comingUp.all { it.isPassed })
    }

    @Test
    fun predictedStartDay_isSlackWaterButNotLate() {
        val state = ready(d(11, 28))

        assertFalse(state.isLate)
        assertEquals("Slack water", state.tideName)
        assertEquals(CycleMessages.PERIOD_EXPECTED_TODAY, state.phaseSubline)
        assertEquals("today", state.until(ComingUpKind.NEXT_PERIOD))
        assertEquals(0.0, state.moonPhase, 0.0)
        assertEquals(28, state.markerCycleDay)
    }

    @Test
    fun firstDay_isNewMoon() {
        assertEquals(0.0, ready(d(10, 31)).moonPhase, 1e-9)
    }

    @Test
    fun onlyFuturePeriods_isNoData() {
        assertEquals(HomeUiState.NoData, buildHomeState(listOf(Period(d(12, 1), d(12, 3))), d(11, 8), predictor))
    }

    @Test
    fun noData_tideUsesTheDefaults() {
        // Population defaults: 29-day cycle, ovulation 14 days before the next start.
        assertEquals(29, HomeUiState.NO_DATA_CYCLE_LENGTH)
        assertEquals(16, HomeUiState.NO_DATA_OVULATION_DAY)
    }

    @Test
    fun untilText_wording() {
        val today = d(11, 8)
        assertEquals("passed", untilText(today, d(11, 7)))
        assertEquals("today", untilText(today, today))
        assertEquals("tomorrow", untilText(today, d(11, 9)))
        assertEquals("in 2 days", untilText(today, d(11, 10)))
    }
}
