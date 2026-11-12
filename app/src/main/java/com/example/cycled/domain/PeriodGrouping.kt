package com.example.cycled.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Groups logged period days into [Period]s, ordered by start date.
 *
 * Input may be in any order and may contain duplicates; it is sorted and
 * de-duplicated first. Two consecutive logged days belong to the same period
 * if at most [maxGapDays] calendar days are missing between them. With the
 * default of 1, Nov 1, 2 and 4 form one period (Nov 1–4), while Nov 1, 2 and 5
 * form two (Nov 1–2 and Nov 5–5).
 */
fun groupIntoPeriods(days: List<LocalDate>, maxGapDays: Int = 1): List<Period> {
    val sorted = days.distinct().sorted()
    if (sorted.isEmpty()) return emptyList()

    val periods = mutableListOf<Period>()
    var start = sorted.first()
    var end = start

    for (day in sorted.drop(1)) {
        val missingDays = ChronoUnit.DAYS.between(end, day) - 1
        if (missingDays <= maxGapDays) {
            end = day
        } else {
            periods += Period(start, end)
            start = day
            end = day
        }
    }
    periods += Period(start, end)
    return periods
}
