package com.example.cycled.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CyclePhasesTest {

    private fun d(month: Int, day: Int) = LocalDate.of(2026, month, day)

    private val nov1to5 = Period(d(11, 1), d(11, 5))

    private fun CyclePhases.range(phase: Phase): PhaseRange? = ranges.firstOrNull { it.phase == phase }

    private val luteal = CycleDefaults.LUTEAL_LENGTH_DAYS.toLong()

    @Test
    fun typicalCycle_allFourPhasesAndFertileWindow() {
        val phases = cyclePhases(nov1to5, nextStart = d(11, 29), today = d(11, 13))

        assertEquals(
            listOf(
                PhaseRange(Phase.MENSTRUAL, d(11, 1), d(11, 5)),
                PhaseRange(Phase.FOLLICULAR, d(11, 6), d(11, 14)),
                PhaseRange(Phase.OVULATORY, d(11, 15), d(11, 17)),
                PhaseRange(Phase.LUTEAL, d(11, 18), d(11, 28)),
            ),
            phases.ranges,
        )
        assertEquals(d(11, 29).minusDays(luteal), phases.ovulation)   // Nov 16 with a 13-day luteal phase
        assertEquals(phases.ovulation.minusDays(5), phases.fertileStart)
        assertEquals(phases.ovulation.plusDays(1), phases.fertileEnd)
    }

    @Test
    fun typicalCycle_phaseOn() {
        val phases = cyclePhases(nov1to5, nextStart = d(11, 29), today = d(11, 13))

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
        val phases = cyclePhases(nov1to5, nextStart = d(11, 30), today = d(11, 13))

        assertEquals(PhaseRange(Phase.FOLLICULAR, d(11, 6), d(11, 15)), phases.range(Phase.FOLLICULAR))
        assertEquals(PhaseRange(Phase.OVULATORY, d(11, 16), d(11, 18)), phases.range(Phase.OVULATORY))
        assertEquals(PhaseRange(Phase.LUTEAL, d(11, 19), d(11, 29)), phases.range(Phase.LUTEAL))
        assertEquals(Phase.FOLLICULAR, phases.phaseOn(d(11, 13)))
    }

    @Test
    fun ongoingPeriod_menstrualUsesDefaultLength() {
        val phases = cyclePhases(Period(d(11, 13), d(11, 13)), nextStart = d(12, 11), today = d(11, 13))

        assertEquals(PhaseRange(Phase.MENSTRUAL, d(11, 13), d(11, 17)), phases.range(Phase.MENSTRUAL))
    }

    @Test
    fun finishedShortPeriod_menstrualUsesLoggedEnd() {
        val phases = cyclePhases(Period(d(11, 1), d(11, 3)), nextStart = d(11, 29), today = d(11, 13))

        assertEquals(PhaseRange(Phase.MENSTRUAL, d(11, 1), d(11, 3)), phases.range(Phase.MENSTRUAL))
        assertEquals(d(11, 4), phases.range(Phase.FOLLICULAR)!!.start)
    }

    @Test
    fun longLoggedPeriod_menstrualUsesLoggedEnd() {
        val phases = cyclePhases(Period(d(11, 1), d(11, 7)), nextStart = d(11, 29), today = d(11, 13))

        assertEquals(PhaseRange(Phase.MENSTRUAL, d(11, 1), d(11, 7)), phases.range(Phase.MENSTRUAL))
    }

    @Test
    fun shortCycle_follicularIsOneDay() {
        // Ovulation Nov 8, so the ovulatory phase starts Nov 7 and only Nov 6 is follicular.
        val nextStart = d(11, 8).plusDays(luteal)
        val phases = cyclePhases(nov1to5, nextStart = nextStart, today = d(11, 13))

        assertEquals(PhaseRange(Phase.FOLLICULAR, d(11, 6), d(11, 6)), phases.range(Phase.FOLLICULAR))
        assertEquals(PhaseRange(Phase.OVULATORY, d(11, 7), d(11, 9)), phases.range(Phase.OVULATORY))
        assertEquals(PhaseRange(Phase.LUTEAL, d(11, 10), nextStart.minusDays(1)), phases.range(Phase.LUTEAL))
    }

    @Test
    fun veryShortCycle_noFollicularPhase() {
        val phases = cyclePhases(nov1to5, nextStart = d(11, 16), today = d(11, 13))

        assertNull(phases.range(Phase.FOLLICULAR))
        assertEquals(PhaseRange(Phase.OVULATORY, d(11, 6), d(11, 6)), phases.range(Phase.OVULATORY))
        assertEquals(PhaseRange(Phase.LUTEAL, d(11, 7), d(11, 15)), phases.range(Phase.LUTEAL))
    }

    @Test
    fun everyCycleLength_rangesCoverCycleInOrderWithoutGapsOrOverlaps() {
        for (length in 15..60) {
            val nextStart = d(11, 1).plusDays(length.toLong())
            val ranges = cyclePhases(nov1to5, nextStart = nextStart, today = d(11, 13)).ranges

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
