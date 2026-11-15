package com.example.cycled.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CycleForecastTest {

    private fun d(month: Int, day: Int) = LocalDate.of(2026, month, day)

    private val predictor = AverageCyclePredictor()

    @Test
    fun noPeriods_isNull() {
        assertNull(cycleForecast(emptyList(), d(11, 13), predictor))
    }

    @Test
    fun onlyFuturePeriods_isNull() {
        assertNull(cycleForecast(listOf(Period(d(11, 20), d(11, 22))), d(11, 13), predictor))
    }

    @Test
    fun onePeriod_usesDefaultPeriodLength() {
        val f = cycleForecast(listOf(Period(d(11, 1), d(11, 3))), d(11, 13), predictor)!!

        assertEquals(CycleDefaults.PERIOD_LENGTH_DAYS, f.periodLengthDays)
        assertEquals(0, f.prediction.basedOnCycles)
        assertEquals(d(11, 1), f.status.lastPeriod.start)
    }

    @Test
    fun periodLength_averagesPastPeriodsExcludingCurrent() {
        val periods = listOf(
            Period(d(9, 5), d(9, 7)),    // 3 days
            Period(d(10, 3), d(10, 9)),  // 7 days
            Period(d(11, 1), d(11, 1)),  // current, ignored
        )
        val f = cycleForecast(periods, d(11, 13), predictor)!!

        assertEquals(5, f.periodLengthDays)
    }

    @Test
    fun isLate_onlyAfterPredictedStart() {
        val periods = listOf(Period(d(11, 1), d(11, 5)))   // next start Nov 29

        assertFalse(cycleForecast(periods, d(11, 29), predictor)!!.isLate)
        assertTrue(cycleForecast(periods, d(11, 30), predictor)!!.isLate)
    }
}
