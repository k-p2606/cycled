package com.example.cycled.ui

import com.example.cycled.domain.CycleDefaults
import com.example.cycled.domain.CyclePhases
import com.example.cycled.domain.CycleStatus
import com.example.cycled.domain.Phase
import com.example.cycled.domain.Prediction
import com.example.cycled.domain.Recommendation
import java.time.LocalDate
import java.time.temporal.ChronoUnit

sealed interface HomeUiState {
    data object Loading : HomeUiState                         // before the database has answered
    data object NoData : HomeUiState                          // nothing logged yet
    data class Ready(
        val status: CycleStatus,                              // from domain.cycleStatus()
        val prediction: Prediction,
        val phases: CyclePhases,
        val currentPhase: Phase?,   // null when today is on or after the predicted start
        val isLate: Boolean,        // true when today is after prediction.nextStart
        val today: LocalDate,
        /** "Flood tide", or "Slack water" with no phase (D2). */
        val tideName: String,
        /** "Follicular phase"; "Your period may be late" when late (D2). */
        val phaseSubline: String,
        /** O: cycle day of the predicted ovulation (1 = cycle start). */
        val ovulationCycleDay: Int,
        /** The forecast's phases as cycle days, in order. */
        val phaseDays: List<PhaseDays>,
        /** The forecast's fertile window (ovulation - 5 to + 1). */
        val fertileWindow: DateSpan,
        /** Next period, fertile window and high tide, in that order (D4). */
        val comingUp: List<ComingUpRow>,
        /** The prediction's earliest to latest next start. */
        val likelyRange: DateSpan,
        /** Moon and tide phase today (0 = new, 0.5 = full); 0 once past day L (D3). */
        val moonPhase: Double,
        /** Cycle day the today marker sits on: today's, clamped to L (D3). */
        val markerCycleDay: Int,
        /** "For today": up to three tips, most relevant first. */
        val recommendations: List<Recommendation> = emptyList(),
        /** "See all": one tip per category, in category order (Body, Fitness, Health, Mind, Food). */
        val allRecommendations: List<Recommendation> = emptyList(),
        /** The late-period tip, shown above the categories in "See all"; null unless late. */
        val lateTip: Recommendation? = null,
    ) : HomeUiState {
        /** Predicted length of the current cycle: from its first day to the predicted next start. */
        val cycleLengthDays: Int
            get() = ChronoUnit.DAYS.between(status.lastPeriod.start, prediction.nextStart).toInt()
    }

    companion object {
        /** L and O for the tide drawn with no data: the population defaults. */
        const val NO_DATA_CYCLE_LENGTH = CycleDefaults.CYCLE_LENGTH_DAYS
        const val NO_DATA_OVULATION_DAY =
            CycleDefaults.CYCLE_LENGTH_DAYS - CycleDefaults.DAYS_FROM_OVULATION_TO_NEXT_START + 1
    }
}

/** Inclusive date range. */
data class DateSpan(val start: LocalDate, val end: LocalDate) {
    operator fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)
}

/** A phase as cycle days (1 = cycle start), inclusive. */
data class PhaseDays(val phase: Phase, val startDay: Int, val endDay: Int)

enum class ComingUpKind { NEXT_PERIOD, FERTILE_WINDOW, HIGH_TIDE }

/**
 * One of Home's three "coming up" rows. [dates] is a single day for the next period
 * and high tide, a range for the fertile window. [until] is "today", "tomorrow",
 * "in N days", "now" (fertile window only) or "passed".
 */
data class ComingUpRow(
    val kind: ComingUpKind,
    val dates: DateSpan,
    val until: String,
) {
    val isPassed: Boolean get() = until == PASSED

    companion object {
        const val PASSED = "passed"
        const val NOW = "now"
    }
}
