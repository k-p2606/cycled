package com.example.cycled.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CycleDayTest {

    private fun d(month: Int, day: Int) = LocalDate.of(2026, month, day)

    private val nov1to5 = Period(d(11, 1), d(11, 5))

    @Test
    fun emptyList_returnsNull() {
        assertNull(cycleStatus(emptyList(), d(11, 13)))
    }

    @Test
    fun onlyPeriodAfterToday_returnsNull() {
        assertNull(cycleStatus(listOf(Period(d(11, 20), d(11, 22))), d(11, 13)))
    }

    @Test
    fun todayIsFirstDayOfLatestPeriod_isDayOneOnPeriod() {
        val status = cycleStatus(listOf(nov1to5), d(11, 1))

        assertNotNull(status)
        assertEquals(1, status!!.cycleDay)
        assertTrue(status.onPeriod)
        assertEquals(1, status.periodDay)
    }

    @Test
    fun todayInMiddleOfPeriod_isOnPeriod() {
        val status = cycleStatus(listOf(nov1to5), d(11, 3))!!

        assertEquals(3, status.cycleDay)
        assertTrue(status.onPeriod)
        assertEquals(3, status.periodDay)
    }

    @Test
    fun todayIsLastDayOfPeriod_isStillOnPeriod() {
        val status = cycleStatus(listOf(nov1to5), d(11, 5))!!

        assertTrue(status.onPeriod)
        assertEquals(5, status.periodDay)
    }

    @Test
    fun dayAfterPeriodEnds_isNotOnPeriod() {
        val status = cycleStatus(listOf(nov1to5), d(11, 6))!!

        assertEquals(6, status.cycleDay)
        assertFalse(status.onPeriod)
        assertNull(status.periodDay)
    }

    @Test
    fun wellAfterPeriod_cycleDayKeepsCounting() {
        val status = cycleStatus(listOf(nov1to5), d(11, 13))!!

        assertEquals(13, status.cycleDay)
        assertFalse(status.onPeriod)
    }

    @Test
    fun twoPeriods_usesMostRecent() {
        val periods = listOf(Period(d(10, 3), d(10, 7)), nov1to5)

        val status = cycleStatus(periods, d(11, 13))!!

        assertEquals(nov1to5, status.lastPeriod)
        assertEquals(13, status.cycleDay)
    }

    @Test
    fun twoPeriodsInReverseOrder_giveSameResult() {
        val sorted = listOf(Period(d(10, 3), d(10, 7)), nov1to5)

        assertEquals(
            cycleStatus(sorted, d(11, 13)),
            cycleStatus(sorted.reversed(), d(11, 13)),
        )
        assertEquals(nov1to5, cycleStatus(sorted.reversed(), d(11, 13))!!.lastPeriod)
    }

    @Test
    fun periodCrossingMonthBoundary_countsAcrossMonths() {
        val status = cycleStatus(listOf(Period(d(10, 30), d(11, 2))), d(11, 4))!!

        assertEquals(6, status.cycleDay)
    }

    @Test
    fun periodCrossingYearBoundary_countsAcrossYears() {
        val period = Period(LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 2))

        val status = cycleStatus(listOf(period), LocalDate.of(2027, 1, 5))!!

        assertEquals(7, status.cycleDay)
    }

    @Test
    fun periodAfterToday_isIgnored() {
        val periods = listOf(nov1to5, Period(d(11, 20), d(11, 22)))

        val status = cycleStatus(periods, d(11, 13))!!

        assertEquals(nov1to5, status.lastPeriod)
        assertEquals(13, status.cycleDay)
    }
}
