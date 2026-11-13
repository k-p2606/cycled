package com.example.cycled.domain

import java.time.LocalDate

enum class Phase { MENSTRUAL, FOLLICULAR, OVULATORY, LUTEAL }

data class PhaseRange(val phase: Phase, val start: LocalDate, val end: LocalDate)   // end inclusive

data class CyclePhases(
    val ranges: List<PhaseRange>,      // in order; a phase with zero days is left out
    val ovulation: LocalDate,          // estimated ovulation day
    val fertileStart: LocalDate,       // ovulation - 5 days (not before lastPeriod.start)
    val fertileEnd: LocalDate,         // ovulation + 1 day
)

/**
 * Splits the current cycle, from [lastPeriod]'s start to the day before [nextStart],
 * into menstrual, follicular, ovulatory and luteal phases.
 *
 * These phases are estimates calculated from dates, not measured. Ovulation is
 * assumed to be [lutealLengthDays] before the next period. While the period may
 * still be going (it was last logged today or yesterday), the menstrual phase lasts
 * at least [periodLengthDays]; otherwise it uses the logged end date.
 */
fun cyclePhases(
    lastPeriod: Period,
    nextStart: LocalDate,
    today: LocalDate,
    periodLengthDays: Int = CycleDefaults.PERIOD_LENGTH_DAYS,
    lutealLengthDays: Int = CycleDefaults.LUTEAL_LENGTH_DAYS,
): CyclePhases {
    val cycleEnd = nextStart.minusDays(1)

    // a) Menstrual
    val mayStillBeGoing = lastPeriod.end == today || lastPeriod.end == today.minusDays(1)
    val menstrualEnd = if (mayStillBeGoing) {
        later(lastPeriod.end, lastPeriod.start.plusDays(periodLengthDays - 1L))
    } else {
        lastPeriod.end
    }.let { earlier(it, cycleEnd) }

    // b) Ovulation
    val ovulation = nextStart.minusDays(lutealLengthDays.toLong())

    // c) Ovulatory
    val ovStart = later(ovulation.minusDays(1), menstrualEnd.plusDays(1))
    val ovEnd = earlier(later(ovulation.plusDays(1), ovStart), cycleEnd)

    // d), e) Follicular and luteal fill the gaps either side.
    val ranges = listOf(
        PhaseRange(Phase.MENSTRUAL, lastPeriod.start, menstrualEnd),
        PhaseRange(Phase.FOLLICULAR, menstrualEnd.plusDays(1), ovStart.minusDays(1)),
        PhaseRange(Phase.OVULATORY, ovStart, ovEnd),
        PhaseRange(Phase.LUTEAL, ovEnd.plusDays(1), cycleEnd),
    ).filter { !it.end.isBefore(it.start) }

    return CyclePhases(
        ranges = ranges,
        ovulation = ovulation,
        fertileStart = later(ovulation.minusDays(5), lastPeriod.start),
        fertileEnd = ovulation.plusDays(1),
    )
}

/** The phase [date] falls in, or null before the cycle started or from the predicted next start on. */
fun CyclePhases.phaseOn(date: LocalDate): Phase? =
    ranges.firstOrNull { !date.isBefore(it.start) && !date.isAfter(it.end) }?.phase

private fun later(a: LocalDate, b: LocalDate): LocalDate = if (a.isAfter(b)) a else b

private fun earlier(a: LocalDate, b: LocalDate): LocalDate = if (a.isBefore(b)) a else b
