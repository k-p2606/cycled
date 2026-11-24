package com.example.cycled.ui.profile

import com.example.cycled.data.Settings
import com.example.cycled.domain.Period
import com.example.cycled.ui.SampleData
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileStateTest {

    private fun d(month: Int, day: Int) = LocalDate.of(2026, month, day)

    @Test
    fun noName_isYouWithY() {
        val s = buildProfileState(Settings(), emptyList(), d(11, 8))
        assertEquals("You", s.displayName)
        assertEquals("Y", s.initial)
        assertEquals(0, s.cyclesLogged)
        assertEquals("0 cycles logged", s.cyclesLine)
    }

    @Test
    fun name_isUsedWithItsInitial() {
        val s = buildProfileState(Settings(name = "maya"), emptyList(), d(11, 8))
        assertEquals("maya", s.displayName)
        assertEquals("M", s.initial)
    }

    @Test
    fun cyclesLogged_countsCompletedCycles() {
        // Four starts in the sample history; the newest is the current cycle.
        assertEquals(3, buildProfileState(Settings(), SampleData.periods, SampleData.today).cyclesLogged)

        val one = listOf(Period(d(11, 1), d(11, 5)))
        assertEquals(0, buildProfileState(Settings(), one, d(11, 8)).cyclesLogged)

        val two = listOf(Period(d(10, 3), d(10, 7))) + one
        val s = buildProfileState(Settings(), two, d(11, 8))
        assertEquals(1, s.cyclesLogged)
        assertEquals("1 cycle logged", s.cyclesLine)
    }

    @Test
    fun futureStarts_doNotCount() {
        val periods = listOf(Period(d(10, 3), d(10, 7)), Period(d(11, 20), d(11, 22)))
        assertEquals(0, buildProfileState(Settings(), periods, d(11, 8)).cyclesLogged)
    }

    @Test
    fun steppers_disableAtTheRangeEnds() {
        val low = buildProfileState(Settings(avgCycle = 20, avgPeriod = 2), emptyList(), d(11, 8))
        assertFalse(low.canDecreaseCycle)
        assertTrue(low.canIncreaseCycle)
        assertFalse(low.canDecreasePeriod)
        assertTrue(low.canIncreasePeriod)

        val high = buildProfileState(Settings(avgCycle = 45, avgPeriod = 10), emptyList(), d(11, 8))
        assertTrue(high.canDecreaseCycle)
        assertFalse(high.canIncreaseCycle)
        assertTrue(high.canDecreasePeriod)
        assertFalse(high.canIncreasePeriod)
    }
}
