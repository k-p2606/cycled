package com.example.cycled.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PeriodGroupingTest {

    private fun d(month: Int, day: Int) = LocalDate.of(2026, month, day)

    @Test
    fun emptyInput_returnsEmptyList() {
        assertTrue(groupIntoPeriods(emptyList()).isEmpty())
    }

    @Test
    fun singleDay_returnsOnePeriodOfOneDay() {
        val result = groupIntoPeriods(listOf(d(11, 1)))

        assertEquals(1, result.size)
        val period = result.single()
        assertEquals(period.start, period.end)
        assertEquals(d(11, 1), period.start)
        assertEquals(1, period.lengthDays)
    }

    @Test
    fun consecutiveDays_returnOnePeriod() {
        val days = (1..5).map { d(11, it) }

        val result = groupIntoPeriods(days)

        assertEquals(listOf(Period(d(11, 1), d(11, 5))), result)
        assertEquals(5, result.single().lengthDays)
    }

    @Test
    fun twoSeparateRuns_returnTwoPeriodsInOrder() {
        val days = (3..7).map { d(10, it) } + (1..5).map { d(11, it) }

        val result = groupIntoPeriods(days)

        assertEquals(
            listOf(Period(d(10, 3), d(10, 7)), Period(d(11, 1), d(11, 5))),
            result,
        )
    }

    @Test
    fun shuffledInput_givesSameResultAsSortedInput() {
        val sorted = (3..7).map { d(10, it) } + (1..5).map { d(11, it) }
        val shuffled = listOf(
            d(11, 3), d(10, 5), d(11, 1), d(10, 7), d(11, 5),
            d(10, 3), d(11, 2), d(10, 6), d(11, 4), d(10, 4),
        )

        assertEquals(groupIntoPeriods(sorted), groupIntoPeriods(shuffled))
    }

    @Test
    fun duplicateDates_areCountedOnce() {
        val result = groupIntoPeriods(listOf(d(11, 1), d(11, 1), d(11, 2)))

        assertEquals(listOf(Period(d(11, 1), d(11, 2))), result)
        assertEquals(2, result.single().lengthDays)
    }

    @Test
    fun oneDayGap_mergesIntoOnePeriod() {
        val result = groupIntoPeriods(listOf(d(11, 1), d(11, 2), d(11, 4)))

        assertEquals(listOf(Period(d(11, 1), d(11, 4))), result)
    }

    @Test
    fun twoDayGap_splitsIntoTwoPeriods() {
        val result = groupIntoPeriods(listOf(d(11, 1), d(11, 2), d(11, 5)))

        assertEquals(
            listOf(Period(d(11, 1), d(11, 2)), Period(d(11, 5), d(11, 5))),
            result,
        )
    }

    @Test
    fun maxGapZero_splitsOnAnyGap() {
        val result = groupIntoPeriods(listOf(d(11, 1), d(11, 2), d(11, 4)), maxGapDays = 0)

        assertEquals(
            listOf(Period(d(11, 1), d(11, 2)), Period(d(11, 4), d(11, 4))),
            result,
        )
    }

    @Test
    fun periodCrossingMonthBoundary_isOnePeriod() {
        val result = groupIntoPeriods(listOf(d(10, 30), d(10, 31), d(11, 1), d(11, 2)))

        assertEquals(listOf(Period(d(10, 30), d(11, 2))), result)
        assertEquals(4, result.single().lengthDays)
    }

    @Test
    fun periodCrossingYearBoundary_isOnePeriod() {
        val days = listOf(
            LocalDate.of(2026, 12, 30),
            LocalDate.of(2026, 12, 31),
            LocalDate.of(2027, 1, 1),
            LocalDate.of(2027, 1, 2),
        )

        val result = groupIntoPeriods(days)

        assertEquals(
            listOf(Period(LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 2))),
            result,
        )
        assertEquals(4, result.single().lengthDays)
    }
}
