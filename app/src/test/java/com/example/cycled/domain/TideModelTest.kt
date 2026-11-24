package com.example.cycled.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TideModelTest {

    private val eps = 1e-9

    // ---- D3 checks, for a spread of cycle shapes ----

    private val shapes = listOf(28 to 14, 29 to 16, 21 to 8, 35 to 21, 45 to 31, 24 to 13)

    @Test
    fun phase_isZeroOnDayOneAndHalfAtOvulation() {
        for ((l, o) in shapes) {
            val tide = TideModel(l, o)
            assertEquals("L=$l O=$o p(1)", 0.0, tide.phase(1.0), eps)
            assertEquals("L=$l O=$o p(O)", 0.5, tide.phase(o.toDouble()), eps)
        }
    }

    @Test
    fun phase_keepsRisingToBelowOneAtDayL() {
        for ((l, o) in shapes) {
            val tide = TideModel(l, o)
            val values = (1..l).map { tide.phase(it.toDouble()) }
            values.zipWithNext().forEach { (a, b) -> assertTrue("L=$l O=$o not rising: $a -> $b", b > a) }
            assertTrue("L=$l O=$o p(L) = ${values.last()}", values.last() < 1.0)
        }
    }

    @Test
    fun x_spansTwentyToThreeSeventy() {
        for ((l, o) in shapes) {
            val tide = TideModel(l, o)
            assertEquals(20.0, tide.x(1.0), eps)
            assertEquals(370.0, tide.x(l.toDouble()), eps)
            assertEquals(9.5, tide.dayAt(tide.x(9.5)), eps)
        }
    }

    // ---- L = 28, O = 14 matches the design's fixed formulas within 0.02 ----

    private val design = TideModel(28, 14)

    private fun designP(d: Double) = if (d <= 14) (d - 1) / 26 else 0.5 + (d - 14) / 30
    private fun designX(d: Double) = 20 + (d - 1) / 27 * 350
    private fun g(d: Double, m: Double, s: Double) = exp(-(d - m) * (d - m) / (2 * s * s))

    private val days = (0..280).map { 1 + it * 0.1 } // days 1..29 in tenths

    @Test
    fun designCycle_phaseAndXMatchTheDesign() {
        for (d in days.filter { it <= 28 }) {
            assertEquals("p($d)", designP(d), design.phase(d), 0.02)
            assertEquals("x($d)", designX(d), design.x(d), 0.02)
        }
    }

    @Test
    fun designCycle_tideHeightMatchesTheDesign() {
        for (d in days.filter { it <= 28 }) {
            val x = designX(d)
            val expected = 72 + 40 * cos(2 * PI * designP(d))
            assertEquals("tide at day $d", expected, design.tideY(x), 0.02)
        }
    }

    @Test
    fun designCycle_hormonesMatchTheDesign() {
        for (d in days) {
            assertEquals("estrogen($d)", 0.12 + 0.78 * g(d, 12.5, 2.3) + 0.32 * g(d, 21.0, 3.5), design.estrogen(d), 0.02)
            assertEquals("progesterone($d)", 0.05 + 0.8 * g(d, 21.0, 3.3), design.progesterone(d), 0.02)
            assertEquals("lh($d)", 0.06 + 0.88 * g(d, 13.6, 0.75), design.lh(d), 0.02)
        }
    }

    // ---- Stretching and edges ----

    @Test
    fun hormonePeaks_followOvulation() {
        val tide = TideModel(35, 21)
        assertEquals(0.94, tide.lh(20.6), 1e-6)                     // LH peak at O - 0.4
        assertEquals(0.85, tide.progesterone(28.0), 1e-6)           // progesterone peak at O + 7
        assertTrue(tide.estrogen(19.5) > tide.estrogen(16.0))      // first estrogen peak at O - 1.5
        assertTrue(tide.estrogen(19.5) > tide.estrogen(23.0))
    }

    @Test
    fun tide_isLowOnDayOneAndHighAtOvulation() {
        val tide = TideModel(30, 16)
        assertEquals(112.0, tide.tideY(tide.x(1.0)), eps)
        assertEquals(32.0, tide.tideY(tide.x(16.0)), eps)
    }

    @Test
    fun waterEdge_coversTheWholeWidth() {
        val edge = TideModel(28, 14).waterEdge()
        assertTrue(edge.first().x < 0)
        assertTrue(edge.last().x > TideModel.WIDTH)
        edge.zipWithNext().forEach { (a, b) -> assertTrue(b.x > a.x) }
        // Ripples stay within 4 of the tide line.
        edge.forEach { assertTrue(it.y in 28.0..116.0) }
    }

    @Test
    fun markerPoint_isTheEdgePointNearestTheDay() {
        val tide = TideModel(28, 14)
        val edge = tide.waterEdge()
        val marker = tide.markerPoint(9, edge)
        val target = tide.x(9.0)
        assertTrue(edge.all { abs(it.x - target) >= abs(marker.x - target) })
        assertTrue(abs(marker.x - target) < 1.0)
    }

    @Test
    fun ovulationDay_isClampedIntoTheCycle() {
        assertEquals(2, TideModel(28, -3).ovulationDay)
        assertEquals(28, TideModel(28, 40).ovulationDay)
        // Still defined at the clamped edges.
        assertEquals(0.5, TideModel(28, 40).phase(28.0), eps)
        assertFalse(TideModel(28, 1).phase(1.0).isNaN())
    }

    @Test
    fun labelDay_isTheMiddleAndSkipsOneDayPhases() {
        val tide = TideModel(28, 14)
        assertEquals(3.0, tide.labelDay(1, 5)!!, eps)
        assertEquals(9.5, tide.labelDay(6, 13)!!, eps)
        assertEquals(null, tide.labelDay(14, 14))
    }

    // ---- Moon ----

    @Test
    fun moonPath_followsTheHandoffFormula() {
        // Waxing (p < 0.25): right limb, terminator arc with sweep 0.
        assertEquals(
            "M82.000 14.000A68.000 68.000 0 0 1 82.000 150.000A48.083 68.000 0 0 0 82.000 14.000Z",
            moonPathData(0.125, 82.0, 82.0, 68.0),
        )
        // Waning (0.5 <= p < 0.75): left limb, terminator arc with sweep 0.
        assertEquals(
            "M82.000 14.000A68.000 68.000 0 0 0 82.000 150.000A48.083 68.000 0 0 0 82.000 14.000Z",
            moonPathData(0.625, 82.0, 82.0, 68.0),
        )
    }

    @Test
    fun moonPath_quarterHasAStraightTerminator() {
        val path = moonPathData(0.25, 82.0, 82.0, 68.0)
        assertEquals("M82.000 14.000A68.000 68.000 0 0 1 82.000 150.000L82.000 14.000Z", path)
    }

    @Test
    fun moonShadow_isHalfACycleOn() {
        assertEquals(moonPathData(0.5, 82.0, 82.0, 68.0), moonShadowPathData(0.0, 82.0, 82.0, 68.0))
        assertEquals(moonPathData(0.0, 82.0, 82.0, 68.0), moonShadowPathData(0.5, 82.0, 82.0, 68.0))
        assertEquals(moonPathData(0.1, 82.0, 82.0, 68.0), moonShadowPathData(0.6, 82.0, 82.0, 68.0))
    }
}
