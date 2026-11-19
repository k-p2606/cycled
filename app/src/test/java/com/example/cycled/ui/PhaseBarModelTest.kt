package com.example.cycled.ui

import com.example.cycled.domain.AverageCyclePredictor
import com.example.cycled.domain.CycleDefaults
import com.example.cycled.domain.Period
import com.example.cycled.domain.Phase
import com.example.cycled.domain.cyclePhases
import com.example.cycled.domain.phaseOn
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhaseBarModelTest {

    private fun d(month: Int, day: Int) = LocalDate.of(2026, month, day)

    private val nov1to5 = Period(d(11, 1), d(11, 5))

    // Typical: Nov 1-5, next Nov 29. Menstrual 1-5, follicular 6-14, ovulatory 15-17, luteal 18-28.
    private fun typical(today: LocalDate) = cyclePhases(nov1to5, nextStart = d(11, 29), today = today)

    private fun model(today: LocalDate) = phaseBarModel(typical(today), today)!!

    private fun PhaseBarModel.segmentContaining(fraction: Float): PhaseSegment {
        var before = 0f
        for (segment in segments) {
            if (fraction < before + segment.fraction) return segment
            before += segment.fraction
        }
        return segments.last()
    }

    @Test
    fun typical_fourSegmentsInOrder_fractionsSumToOne() {
        val m = model(d(11, 15))

        assertEquals(
            listOf(Phase.MENSTRUAL, Phase.FOLLICULAR, Phase.OVULATORY, Phase.LUTEAL),
            m.segments.map { it.phase },
        )
        assertEquals(28, m.cycleLength)
        assertEquals(1f, m.segments.sumOf { it.fraction.toDouble() }.toFloat(), 1e-4f)
        assertEquals(3f / 28, m.segments[2].fraction, 1e-6f)
    }

    @Test
    fun todayFraction_insideCurrentPhaseSegment() {
        for (day in 1..28) {
            val today = d(11, day)
            val m = model(today)
            assertEquals("Nov $day", m.currentPhase, m.segmentContaining(m.todayFraction).phase)
            assertEquals("Nov $day", day, m.cycleDay)
        }
    }

    @Test
    fun shortCycle_zeroLengthFollicular_hasNoFollicularSegment() {
        // Ovulation Nov 7 (a 19-day cycle with a 13-day luteal phase): ovulatory starts the day after the period ends.
        val today = d(11, 7)
        val nextStart = d(11, 7).plusDays(CycleDefaults.LUTEAL_LENGTH_DAYS.toLong())
        val m = phaseBarModel(cyclePhases(nov1to5, nextStart = nextStart, today = today), today)!!

        assertFalse(m.segments.any { it.phase == Phase.FOLLICULAR })
        assertEquals(listOf(Phase.MENSTRUAL, Phase.OVULATORY, Phase.LUTEAL), m.segments.map { it.phase })
        assertEquals(1f, m.segments.sumOf { it.fraction.toDouble() }.toFloat(), 1e-4f)
        assertEquals(Phase.OVULATORY, m.currentPhase)
    }

    @Test
    fun todayOnFirstDayOfCycle() {
        // Today Nov 1 with the period logged through Nov 1 only (may still be going).
        val today = d(11, 1)
        val m = phaseBarModel(cyclePhases(Period(today, today), d(11, 29), today), today)!!

        assertEquals(1, m.cycleDay)
        assertEquals(Phase.MENSTRUAL, m.currentPhase)
        assertEquals(0.5f / 28, m.todayFraction, 1e-6f)
    }

    @Test
    fun todayOnLastDayBeforePredictedPeriod() {
        val m = model(d(11, 28))

        assertEquals(28, m.cycleDay)
        assertEquals(Phase.LUTEAL, m.currentPhase)
        assertEquals(27.5f / 28, m.todayFraction, 1e-6f)
        assertEquals(d(11, 29), m.nextStart)
    }

    @Test
    fun late_isNull() {
        assertNull(phaseBarModel(typical(d(11, 29)), d(11, 29)))   // predicted start day
        assertNull(phaseBarModel(typical(d(12, 2)), d(12, 2)))     // after it
    }

    @Test
    fun beforeCycle_isNull() {
        assertNull(phaseBarModel(typical(d(10, 31)), d(10, 31)))
    }

    @Test
    fun segmentsMatchPhaseOn_forEveryCycleDay() {
        // Two periods, as Home builds them, so this is the same data the calendar uses.
        val today = d(11, 13)
        val home = buildHomeState(
            listOf(Period(d(10, 3), d(10, 7)), nov1to5), today, AverageCyclePredictor(),
        ) as HomeUiState.Ready
        val m = phaseBarModel(home.phases, today)!!

        var date = m.segments.first().start
        while (date.isBefore(m.nextStart)) {
            val containing = m.segments.filter { !date.isBefore(it.start) && !date.isAfter(it.end) }
            assertEquals("$date", 1, containing.size)
            assertEquals("$date", home.phases.phaseOn(date), containing.single().phase)
            date = date.plusDays(1)
        }
        assertEquals(home.prediction.nextStart, m.nextStart)
    }

    @Test
    fun todayX_insideCurrentSegmentSpan_withGaps() {
        val width = 300f
        val gap = 6f
        for (day in 1..28) {
            val m = model(d(11, day))
            val spans = m.segmentSpans(width, gap)
            val (start, end) = spans[m.segments.indexOfFirst { it.phase == m.currentPhase }]
            val x = m.todayX(width, gap)
            assertTrue("Nov $day: $x not in $start..$end", x in start..end)
        }
        assertEquals(width, model(d(11, 13)).segmentSpans(width, gap).last().second, 1e-3f)
    }
}
