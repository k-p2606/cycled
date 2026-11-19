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
        val periods = listOf(Period(d(11, 1), d(11, 5)))
        val nextStart = d(11, 1).plusDays(CycleDefaults.CYCLE_LENGTH_DAYS.toLong())   // placeholder: Nov 30

        assertFalse(cycleForecast(periods, nextStart, predictor)!!.isLate)
        assertTrue(cycleForecast(periods, nextStart.plusDays(1), predictor)!!.isLate)
    }

    @Test
    fun ovulationAndPeriodLength_comeFromThePrediction() {
        // A fake model: next start Nov 29, a 7-day period and ovulation on Nov 10, four days
        // earlier than the rule (Nov 29 - 14 = Nov 15) would put it.
        val fake = object : CyclePredictor {
            override fun predict(periods: List<Period>) = Prediction(
                nextStart = d(11, 29),
                earliest = d(11, 26),
                latest = d(12, 2),
                basedOnCycles = 0,
                periodLengthDays = 7,
                ovulation = d(11, 10),
            )
        }
        // Period logged Nov 1 only and today Nov 2: it may still be going.
        val f = cycleForecast(listOf(Period(d(11, 1), d(11, 1))), d(11, 2), fake)!!

        assertEquals(7, f.periodLengthDays)
        assertEquals(d(11, 10), f.phases.ovulation)
        assertEquals(
            listOf(
                PhaseRange(Phase.MENSTRUAL, d(11, 1), d(11, 7)),     // 7 days from the prediction
                PhaseRange(Phase.FOLLICULAR, d(11, 8), d(11, 8)),
                PhaseRange(Phase.OVULATORY, d(11, 9), d(11, 11)),    // ovulation ± 1
                PhaseRange(Phase.LUTEAL, d(11, 12), d(11, 28)),      // up to the day before nextStart
            ),
            f.phases.ranges,
        )
        assertEquals(d(11, 5), f.phases.fertileStart)                // ovulation - 5
        assertEquals(d(11, 11), f.phases.fertileEnd)                 // ovulation + 1
        assertNull(f.phases.phaseOn(d(11, 29)))
    }

    @Test
    fun finishedPeriod_keepsItsLoggedEnd() {
        val fake = object : CyclePredictor {
            override fun predict(periods: List<Period>) =
                Prediction(d(11, 29), d(11, 26), d(12, 2), 0, periodLengthDays = 7, ovulation = d(11, 15))
        }
        // Logged Nov 1-3 and today Nov 13: finished, so the 7-day prediction doesn't stretch it.
        val f = cycleForecast(listOf(Period(d(11, 1), d(11, 3))), d(11, 13), fake)!!

        assertEquals(PhaseRange(Phase.MENSTRUAL, d(11, 1), d(11, 3)), f.phases.ranges.first())
    }

    @Test
    fun mlPredictor_phasesCoverCycleInOrderWithoutGapsOrOverlaps() {
        val ml = MlCyclePredictor(TestAssets.model())
        val last = d(11, 1)
        for (length in 15..60) {
            for (periodDays in listOf(1, 5, 10)) {
                for (ongoing in listOf(true, false)) {
                    val previous = Period(last.minusDays(length.toLong()), last.minusDays(length - periodDays + 1L))
                    val current = Period(last, if (ongoing) last else last.plusDays(2))
                    val today = if (ongoing) last.plusDays(1) else last.plusDays(5)
                    val label = "length $length, period $periodDays, ongoing $ongoing"

                    val f = cycleForecast(listOf(previous, current), today, ml)!!
                    val ranges = f.phases.ranges
                    assertEquals(label, last, ranges.first().start)
                    assertEquals(label, f.prediction.nextStart.minusDays(1), ranges.last().end)
                    assertEquals(label, ranges.map { it.phase }.sorted(), ranges.map { it.phase })
                    ranges.zipWithNext { a, b -> assertEquals(label, a.end.plusDays(1), b.start) }
                    assertTrue(label, f.phases.fertileEnd.isBefore(f.prediction.nextStart))
                    assertTrue(label, !f.prediction.earliest.isAfter(f.prediction.nextStart))
                    assertTrue(label, !f.prediction.latest.isBefore(f.prediction.nextStart))
                }
            }
        }
    }
}
