package com.example.cycled.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * Lays out [month] as calendar weeks, each a list of exactly 7 cells, with the
 * first row starting on [firstDayOfWeek].
 *
 * A null cell is padding: it sits before the 1st or after the last day of the
 * month. Dates from neighbouring months are never used. Only as many rows as the
 * month needs are returned (4 to 6). This never reads the clock or the locale, so
 * the caller decides [firstDayOfWeek].
 */
fun monthGrid(month: YearMonth, firstDayOfWeek: DayOfWeek): List<List<LocalDate?>> {
    val first = month.atDay(1)
    val leadingNulls = (first.dayOfWeek.value - firstDayOfWeek.value + 7) % 7

    val cells: List<LocalDate?> =
        List(leadingNulls) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    val trailingNulls = (7 - cells.size % 7) % 7

    return (cells + List(trailingNulls) { null }).chunked(7)
}
