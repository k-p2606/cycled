package com.example.cycled.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class CycleStatus(
    val lastPeriod: Period,   // the most recent period that started on or before today
    val cycleDay: Int,        // day 1 = lastPeriod.start
    val onPeriod: Boolean,    // true if today is between lastPeriod.start and lastPeriod.end, inclusive
    val periodDay: Int?,      // day within the period when onPeriod (1 = start day), otherwise null
)

/**
 * Works out where [today] falls in the user's current cycle.
 *
 * Day 1 of the cycle is the first day of the most recent period that started on or
 * before [today]. Periods starting after [today] are ignored, and [periods] need not
 * be sorted. Returns null if no period has started on or before [today].
 */
fun cycleStatus(periods: List<Period>, today: LocalDate): CycleStatus? {
    val lastPeriod = periods
        .filter { !it.start.isAfter(today) }
        .maxByOrNull { it.start }
        ?: return null

    val cycleDay = ChronoUnit.DAYS.between(lastPeriod.start, today).toInt() + 1
    val onPeriod = !today.isAfter(lastPeriod.end)

    return CycleStatus(
        lastPeriod = lastPeriod,
        cycleDay = cycleDay,
        onPeriod = onPeriod,
        periodDay = if (onPeriod) cycleDay else null,
    )
}
