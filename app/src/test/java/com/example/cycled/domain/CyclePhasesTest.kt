package com.example.cycled.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CyclePhasesTest {

    private fun d(month: Int, day: Int) = LocalDate.of(2026, month, day)

    private val nov1to5 = Period(d(11, 1), d(11, 5))

    /** Phases with rule-based ovulation (next start - 14) and period length (5), like AverageCyclePredictor. */
    private fun rulePhases(lastPeriod: Period, nextStart: LocalDate, today: LocalDate) = cyclePhases(
        lastPeriod = lastPeriod,
        nextStart = nextStart,
        ovulation = nextStart.minusDays(CycleDefaults.DAYS_FROM_OVULATION_TO_NEXT_START.toLong()),
        periodLengthDays = CycleDefaults.PERIOD_LENGTH_DAYS,
        today = today,
    )

    private fun CyclePhases.range(phase: Phase): PhaseRange? = ranges.firstOrNull { it.phase == phase }

    @Test
    fun typicalCycle_allFourPhasesAndFertileWindow() {
        val phases = rulePhases(nov1to5, nextStart = d(11, 29), today = d(11, 13))

        assertEquals(
            listOf(
                PhaseRange(Phase.MENSTRUAL, d(11, 1), d(11, 5)),
                PhaseRange(Phase.FOLLICULAR, d(11, 6), d(11, 13)),
                PhaseRange(Phase.OVULATORY, d(11, 14), d(11, 16)),
                PhaseRange(Phase.LUTEAL, d(11, 17), d(11, 28)),
            ),
            phases.ranges,
        )
        assertEquals(d(11, 15), phases.ovulation)
        assertEquals(d(11, 10), phases.fertileStart)
        assertEquals(d(11, 16), phases.fertileEnd)
    }

    @Test
    fun typicalCycle_phaseOn() {
        val phases = rulePhases(nov1to5, nextStart = d(11, 29), today = d(11, 13))

        assertNull(phases.phaseOn(d(10, 31)))
        assertEquals(Phase.MENSTRUAL, phases.phaseOn(d(11, 3)))
        assertEquals(Phase.FOLLICULAR, phases.phaseOn(d(11, 13)))
        assertEquals(Phase.OVULATORY, phases.phaseOn(d(11, 15)))
        assertEquals(Phase.LUTEAL, phases.phaseOn(d(11, 20)))
        assertEquals(Phase.LUTEAL, phases.phaseOn(d(11, 28)))
        assertNull(phases.phaseOn(d(11, 29)))
    }

    @Test
    fun cycleOneDayLonger_shiftsLaterPhases() {
        val phases = rulePhases(nov1to5, nextStart = d(11, 30), today = d(11, 13))

        assertEquals(PhaseRange(Phase.FOLLICULAR, d(11, 6), d(11, 14)), phases.range(Phase.FOLLICULAR))
        assertEquals(PhaseRange(Phase.OVULATORY, d(11, 15), d(11, 17)), phases.range(Phase.OVULATORY))
        assertEquals(PhaseRange(Phase.LUTEAL, d(11, 18), d(11, 29)), phases.range(Phase.LUTEAL))
        assertEquals(Phase.FOLLICULAR, phases.phaseOn(d(11, 13)))
    }

    @Test
    fun ongoingPeriod_menstrualUsesDefaultLength() {
        val phases = rulePhases(Period(d(11, 13), d(11, 13)), nextStart = d(12, 11), today = d(11, 13))

        assertEquals(PhaseRange(Phase.MENSTRUAL, d(11, 13), d(11, 17)), phases.range(Phase.MENSTRUAL))
    }

    @Test
    fun finishedShortPeriod_menstrualUsesLoggedEnd() {
        val phases = rulePhases(Period(d(11, 1), d(11, 3)), nextStart = d(11, 29), today = d(11, 13))

        assertEquals(PhaseRange(Phase.MENSTRUAL, d(11, 1), d(11, 3)), phases.range(Phase.MENSTRUAL))
        assertEquals(d(11, 4), phases.range(Phase.FOLLICULAR)!!.start)
    }

    @Test
    fun longLoggedPeriod_menstrualUsesLoggedEnd() {
        val phases = rulePhases(Period(d(11, 1), d(11, 7)), nextStart = d(11, 29), today = d(11, 13))

        assertEquals(PhaseRange(Phase.MENSTRUAL, d(11, 1), d(11, 7)), phases.range(Phase.MENSTRUAL))
    }

    @Test
    fun shortCycle_follicularIsOneDay() {
        val phases = rulePhases(nov1to5, nextStart = d(11, 22), today = d(11, 13))

        assertEquals(PhaseRange(Phase.FOLLICULAR, d(11, 6), d(11, 6)), phases.range(Phase.FOLLICULAR))
        assertEquals(PhaseRange(Phase.OVULATORY, d(11, 7), d(11, 9)), phases.range(Phase.OVULATORY))
        assertEquals(PhaseRange(Phase.LUTEAL, d(11, 10), d(11, 21)), phases.range(Phase.LUTEAL))
    }

    @Test
    fun veryShortCycle_noFollicularPhase() {
        val phases = rulePhases(nov1to5, nextStart = d(11, 16), today = d(11, 13))

        assertNull(phases.range(Phase.FOLLICULAR))
        assertEquals(PhaseRange(Phase.OVULATORY, d(11, 6), d(11, 6)), phases.range(Phase.OVULATORY))
        assertEquals(PhaseRange(Phase.LUTEAL, d(11, 7), d(11, 15)), phases.range(Phase.LUTEAL))
    }

    @Test
    fun everyCycleLength_rangesCoverCycleInOrderWithoutGapsOrOverlaps() {
        for (length in 15..60) {
            val nextStart = d(11, 1).plusDays(length.toLong())
            val ranges = rulePhases(nov1to5, nextStart = nextStart, today = d(11, 13)).ranges

            assertEquals("length $length: first start", d(11, 1), ranges.first().start)
            assertEquals("length $length: last end", nextStart.minusDays(1), ranges.last().end)
            assertEquals(
                "length $length: phases in order",
                ranges.map { it.phase }.sorted(),
                ranges.map { it.phase },
            )
            ranges.forEach {
                assertTrue("length $length: $it is empty", !it.end.isBefore(it.start))
            }
            ranges.zipWithNext { a, b ->
                assertEquals("length $length: $a then $b", a.end.plusDays(1), b.start)
            }
        }
    }
}
