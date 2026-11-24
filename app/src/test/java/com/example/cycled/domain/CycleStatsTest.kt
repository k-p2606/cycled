package com.example.cycled.domain

import com.example.cycled.ui.SampleData
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CycleStatsTest {

    private val jan1: LocalDate = LocalDate.of(2026, 1, 1)

    /** Periods of [periodDays] days starting [gaps] apart, the first on 1 Jan 2026. */
    private fun periods(gaps: List<Int>, periodDays: Int = 5): List<Period> {
        val starts = gaps.runningFold(jan1) { start, gap -> start.plusDays(gap.toLong()) }
        return starts.map { Period(it, it.plusDays(periodDays - 1L)) }
    }

    @Test
    fun noData_isNull() {
        assertNull(cycleStats(emptyList(), jan1))
    }

    @Test
    fun onePeriod_hasNoCompletedCycle() {
        assertNull(cycleStats(periods(emptyList()), jan1.plusDays(10)))
    }

    @Test
    fun oneCycle_hasNoVariation() {
        val p = periods(listOf(30))
        val stats = cycleStats(p, p.last().start.plusDays(10))!!

        assertEquals(30, stats.averageCycleDays)
        assertEquals(5, stats.averagePeriodDays)
        assertNull(stats.variationDays)
        assertEquals(1, stats.cyclesUsed)
    }

    @Test
    fun eightCycles_onlyTheLastSixAreUsed() {
        // The first two (40 days) would pull the average up if they counted.
        val p = periods(listOf(40, 40, 28, 28, 30, 30, 26, 26))
        val stats = cycleStats(p, p.last().end.plusDays(5))!!

        assertEquals(6, stats.cyclesUsed)
        assertEquals(28, stats.averageCycleDays)        // (28+28+30+30+26+26) / 6
        assertEquals(2, stats.variationDays)            // population SD = sqrt(16/6) ≈ 1.63
    }

    @Test
    fun periodAverage_usesTheMostRecentSixFinishedPeriods() {
        val starts = periods(List(7) { 28 })                // 8 periods
        val lengths = listOf(9, 9, 4, 4, 4, 5, 5, 5)        // the first two must not count
        val p = starts.mapIndexed { i, it -> Period(it.start, it.start.plusDays(lengths[i] - 1L)) }

        assertEquals(5, cycleStats(p, p.last().end.plusDays(10))!!.averagePeriodDays)   // 27/6 = 4.5
    }

    @Test
    fun seventyDayGap_isExcluded() {
        val p = periods(listOf(28, 70, 30))
        val stats = cycleStats(p, p.last().end.plusDays(5))!!

        assertEquals(2, stats.cyclesUsed)
        assertEquals(29, stats.averageCycleDays)
        assertEquals(1, stats.variationDays)
    }

    @Test
    fun onlyInvalidGaps_isNull() {
        val p = periods(listOf(70))
        assertNull(cycleStats(p, p.last().end.plusDays(5)))
    }

    @Test
    fun currentPeriodStillGoing_isLeftOutOfThePeriodAverage() {
        // Two finished 5-day periods, then one logged for 2 days so far.
        val p = periods(listOf(28, 28)).dropLast(1) + periods(listOf(28, 28), periodDays = 2).last()
        val latest = p.last()

        assertEquals(5, cycleStats(p, latest.end)!!.averagePeriodDays)              // last day is today
        assertEquals(5, cycleStats(p, latest.end.plusDays(1))!!.averagePeriodDays)  // ... or yesterday
        assertEquals(4, cycleStats(p, latest.end.plusDays(2))!!.averagePeriodDays)  // finished: (5+5+2)/3
    }

    @Test
    fun oneCycle_currentPeriodGoing_usesTheEarlierPeriod() {
        // A completed cycle needs two periods, so only the latest can be left out.
        val p = listOf(Period(jan1, jan1), Period(jan1.plusDays(28), jan1.plusDays(29)))
        assertEquals(1, cycleStats(p, jan1.plusDays(29))!!.averagePeriodDays)
    }

    @Test
    fun unsortedInput_givesTheSameResult() {
        val p = periods(listOf(28, 26, 29))
        val today = p.last().end.plusDays(3)
        assertEquals(cycleStats(p, today), cycleStats(p.reversed(), today))
    }

    @Test
    fun handoffSampleData() {
        // Cycles 29 (9 Aug), 26 (7 Sep), 28 (3 Oct); every period is 5 days and the
        // current one ended 4 Nov, so it counts.
        val stats = cycleStats(SampleData.periods, SampleData.today)!!

        assertEquals(28, stats.averageCycleDays)
        assertEquals(5, stats.averagePeriodDays)
        assertEquals(1, stats.variationDays)
        assertEquals(3, stats.cyclesUsed)
    }
}
