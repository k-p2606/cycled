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

    /** Prediction from a last start of Nov 1 with the placeholder length and the ±3 range. */
    private val placeholder = d(11, 1).plusDays(CycleDefaults.CYCLE_LENGTH_DAYS.toLong()).let {
        Prediction(it, it.minusDays(3), it.plusDays(3), basedOnCycles = 0)
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
        val prediction = predictor.predict(listOf(d(11, 1)))

        assertEquals(d(11, 30), placeholder.nextStart)
        assertEquals(placeholder, prediction)
    }

    @Test
    fun twoStarts_usesTheOneLength() {
        val prediction = predictor.predict(listOf(d(10, 3), d(11, 1)))

        assertEquals(Prediction(d(11, 30), d(11, 27), d(12, 3), basedOnCycles = 1), prediction)
    }

    @Test
    fun unsortedWithDuplicate_givesSameResultAsSorted() {
        val sorted = listOf(d(10, 3), d(11, 1))
        val messy = listOf(d(11, 1), d(10, 3), d(11, 1))

        assertEquals(predictor.predict(sorted), predictor.predict(messy))
    }

    @Test
    fun identicalLengths_rangeClampedToMinimum() {
        val prediction = predictor.predict(startsFromLengths(28, 28, 28))

        val min = CycleDefaults.MIN_RANGE_HALF_WIDTH_DAYS.toLong()
        assertEquals(
            Prediction(d(11, 29), d(11, 29).minusDays(min), d(11, 29).plusDays(min), basedOnCycles = 3),
            prediction,
        )
    }

    @Test
    fun varyingLengths_rangeIsRoundedStdDev() {
        val prediction = predictor.predict(startsFromLengths(24, 32, 28, 36))

        assertEquals(Prediction(d(12, 1), d(11, 27), d(12, 5), basedOnCycles = 4), prediction)
    }

    @Test
    fun outlierLength_isIgnored() {
        val prediction = predictor.predict(startsFromLengths(28, 90, 28))

        assertEquals(Prediction(d(11, 29), d(11, 26), d(12, 2), basedOnCycles = 2), prediction)
    }

    @Test
    fun onlyOutlierLength_fallsBackToPlaceholder() {
        val prediction = predictor.predict(startsFromLengths(90))

        assertEquals(placeholder, prediction)
    }

    @Test
    fun manyCycles_onlyMostRecentSixUsed() {
        val prediction = predictor.predict(startsFromLengths(40, 40, 28, 28, 28, 28, 28, 28))

        val min = CycleDefaults.MIN_RANGE_HALF_WIDTH_DAYS.toLong()
        assertEquals(
            Prediction(d(11, 29), d(11, 29).minusDays(min), d(11, 29).plusDays(min), basedOnCycles = 6),
            prediction,
        )
    }

    @Test
    fun veryIrregularLengths_rangeClampedToAtMostSeven() {
        val prediction = predictor.predict(startsFromLengths(20, 45, 20, 45))!!

        val halfWidth = ChronoUnit.DAYS.between(prediction.nextStart, prediction.latest)
        assertTrue("halfWidth was $halfWidth", halfWidth <= 7)
    }
}
