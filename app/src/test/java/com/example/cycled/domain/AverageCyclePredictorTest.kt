package com.example.cycled.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AverageCyclePredictorTest {

    private fun d(month: Int, day: Int) = LocalDate.of(2026, month, day)

    private val predictor = AverageCyclePredictor()

    /** Each start as a 5-day period, so the period length is 5 with or without past periods. */
    private fun periodsFrom(starts: List<LocalDate>) = starts.map { Period(it, it.plusDays(4)) }

    /** Expected prediction with the rule-based period length (5) and ovulation (next start - 14). */
    private fun expected(nextStart: LocalDate, earliest: LocalDate, latest: LocalDate, basedOnCycles: Int) =
        Prediction(
            nextStart, earliest, latest, basedOnCycles,
            periodLengthDays = CycleDefaults.PERIOD_LENGTH_DAYS,
            ovulation = nextStart.minusDays(CycleDefaults.DAYS_FROM_OVULATION_TO_NEXT_START.toLong()),
        )

    /** Prediction from a last start of Nov 1 with the placeholder length and the ±3 range. */
    private val placeholder = d(11, 1).plusDays(CycleDefaults.CYCLE_LENGTH_DAYS.toLong()).let {
        expected(it, it.minusDays(3), it.plusDays(3), basedOnCycles = 0)
    }

    /**
     * Start dates ending on Nov 1, 2026, built by counting backwards through
     * [lengths] (given oldest first).
     */
    private fun startsFromLengths(vararg lengths: Int): List<LocalDate> {
        val starts = mutableListOf(d(11, 1))
        for (length in lengths.reversed()) {
            starts.add(0, starts.first().minusDays(length.toLong()))
        }
        return starts
    }

    @Test
    fun emptyList_returnsNull() {
        assertNull(predictor.predict(emptyList()))
    }

    @Test
    fun oneStart_usesPlaceholderLength() {
        val prediction = predictor.predict(periodsFrom(listOf(d(11, 1))))

        assertEquals(d(11, 30), placeholder.nextStart)
        assertEquals(placeholder, prediction)
    }

    @Test
    fun twoStarts_usesTheOneLength() {
        val prediction = predictor.predict(periodsFrom(listOf(d(10, 3), d(11, 1))))

        assertEquals(expected(d(11, 30), d(11, 27), d(12, 3), basedOnCycles = 1), prediction)
    }

    @Test
    fun unsortedWithDuplicate_givesSameResultAsSorted() {
        val sorted = listOf(d(10, 3), d(11, 1))
        val messy = listOf(d(11, 1), d(10, 3), d(11, 1))

        assertEquals(predictor.predict(periodsFrom(sorted)), predictor.predict(periodsFrom(messy)))
    }

    @Test
    fun identicalLengths_rangeClampedToMinimum() {
        val prediction = predictor.predict(periodsFrom(startsFromLengths(28, 28, 28)))

        val min = CycleDefaults.MIN_RANGE_HALF_WIDTH_DAYS.toLong()
        assertEquals(
            expected(d(11, 29), d(11, 29).minusDays(min), d(11, 29).plusDays(min), basedOnCycles = 3),
            prediction,
        )
    }

    @Test
    fun varyingLengths_rangeIsRoundedStdDev() {
        val prediction = predictor.predict(periodsFrom(startsFromLengths(24, 32, 28, 36)))

        assertEquals(expected(d(12, 1), d(11, 27), d(12, 5), basedOnCycles = 4), prediction)
    }

    @Test
    fun outlierLength_isIgnored() {
        val prediction = predictor.predict(periodsFrom(startsFromLengths(28, 90, 28)))

        assertEquals(expected(d(11, 29), d(11, 26), d(12, 2), basedOnCycles = 2), prediction)
    }

    @Test
    fun onlyOutlierLength_fallsBackToPlaceholder() {
        val prediction = predictor.predict(periodsFrom(startsFromLengths(90)))

        assertEquals(placeholder, prediction)
    }

    @Test
    fun manyCycles_onlyMostRecentSixUsed() {
        val prediction = predictor.predict(periodsFrom(startsFromLengths(40, 40, 28, 28, 28, 28, 28, 28)))

        val min = CycleDefaults.MIN_RANGE_HALF_WIDTH_DAYS.toLong()
        assertEquals(
            expected(d(11, 29), d(11, 29).minusDays(min), d(11, 29).plusDays(min), basedOnCycles = 6),
            prediction,
        )
    }

    @Test
    fun veryIrregularLengths_rangeClampedToAtMostSeven() {
        val prediction = predictor.predict(periodsFrom(startsFromLengths(20, 45, 20, 45)))!!

        val halfWidth = ChronoUnit.DAYS.between(prediction.nextStart, prediction.latest)
        assertTrue("halfWidth was $halfWidth", halfWidth <= 7)
    }

    @Test
    fun periodLength_averagesPeriodsBeforeTheLatest() {
        val periods = listOf(
            Period(d(9, 5), d(9, 7)),    // 3 days
            Period(d(10, 3), d(10, 7)),  // 5 days
            Period(d(11, 1), d(11, 10)), // latest (may still be going), ignored
        )

        assertEquals(4, predictor.predict(periods)!!.periodLengthDays)
    }

    @Test
    fun periodLength_onlyMostRecentSixPastPeriodsUsed() {
        val tenDay = Period(d(3, 1), d(3, 10))
        val threeDay = (4..9).map { month -> Period(d(month, 1), d(month, 3)) }
        val latest = Period(d(11, 1), d(11, 1))

        assertEquals(3, predictor.predict(listOf(tenDay) + threeDay + latest)!!.periodLengthDays)
    }

    @Test
    fun periodLength_onePeriod_usesDefault() {
        val prediction = predictor.predict(listOf(Period(d(11, 1), d(11, 10))))!!

        assertEquals(CycleDefaults.PERIOD_LENGTH_DAYS, prediction.periodLengthDays)
    }

    @Test
    fun ovulation_isFourteenDaysBeforeNextStart() {
        val prediction = predictor.predict(listOf(Period(d(10, 3), d(10, 7)), Period(d(11, 1), d(11, 5))))!!

        assertEquals(d(11, 30), prediction.nextStart)
        assertEquals(d(11, 16), prediction.ovulation)
        assertEquals(
            CycleDefaults.DAYS_FROM_OVULATION_TO_NEXT_START.toLong(),
            ChronoUnit.DAYS.between(prediction.ovulation, prediction.nextStart),
        )
    }
}
