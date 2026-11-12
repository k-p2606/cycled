package com.example.cycled.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class Period(val start: LocalDate, val end: LocalDate) {
    /** Inclusive length: a period from Nov 1 to Nov 5 is 5 days. */
    val lengthDays: Int get() = ChronoUnit.DAYS.between(start, end).toInt() + 1
}
