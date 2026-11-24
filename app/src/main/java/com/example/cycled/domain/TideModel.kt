package com.example.cycled.domain

import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/** A point in the tide drawing's 390 × 172 design box (y grows downwards). */
data class TidePoint(val x: Double, val y: Double)

/**
 * The maths behind Home's tide drawing and moon (handoff "Home" and D3), stretched to
 * the user's predicted cycle. Pure: no Android or Compose, all in design units
 * (a 390 × 172 box) that the UI scales to the screen.
 *
 * [cycleLengthDays] is L (predicted start minus cycle start) and [ovulationCycleDay] is
 * O (predicted ovulation's cycle day, 1 = cycle start). O is clamped to 2..L so both
 * halves of [phase] are defined however odd the prediction.
 */
class TideModel(cycleLengthDays: Int, ovulationCycleDay: Int) {

    /** L, at least 2 so x() is defined. */
    val cycleLength: Int = cycleLengthDays.coerceAtLeast(2)

    /** O, clamped to 2..L. */
    val ovulationDay: Int = ovulationCycleDay.coerceIn(2, cycleLength)

    /**
     * Moon and tide phase for cycle [day]: 0 = new moon / low water on day 1,
     * 0.5 = full moon / high water at ovulation, rising towards (but below) 1 by day L.
     * Outside 1..L the two lines simply continue, so the wave runs off both edges smoothly.
     */
    fun phase(day: Double): Double {
        val o = ovulationDay.toDouble()
        return if (day <= o) {
            0.5 * (day - 1) / (o - 1)
        } else {
            0.5 + 0.5 * (day - o) / (cycleLength + 1 - o)
        }
    }

    /** x of cycle [day]: day 1 at 20, day L at 370. */
    fun x(day: Double): Double = LEFT + (day - 1) / (cycleLength - 1) * SPAN

    /** The (fractional) cycle day at [x]; the inverse of [x]. */
    fun dayAt(x: Double): Double = 1 + (x - LEFT) / SPAN * (cycleLength - 1)

    /** Water height at [x]: lowest (y 112) on day 1, highest (y 32) at ovulation. */
    fun tideY(x: Double): Double = TIDE_MID + TIDE_AMPLITUDE * cos(2 * PI * phase(dayAt(x)))

    /**
     * The front water edge: sharp-crested ripples (a trochoid) riding the tide, from
     * just off the left edge until one point past the right edge. Fill from here down
     * to [HEIGHT]. The back layer is the same edge moved by [BACK_DX], [BACK_DY].
     */
    fun waterEdge(): List<TidePoint> {
        val points = mutableListOf<TidePoint>()
        var t = TROCHOID_T_START
        while (true) {
            val x = TROCHOID_STEP_X * t - TROCHOID_LOOP * sin(t)
            points += TidePoint(x, tideY(x) - RIPPLE * cos(t))
            if (x > WIDTH) break
            t += TROCHOID_T_STEP
        }
        return points
    }

    /** The today marker's spot: the point of [edge] whose x is nearest x([day]). */
    fun markerPoint(day: Int, edge: List<TidePoint> = waterEdge()): TidePoint {
        val target = x(day.toDouble())
        return edge.minBy { abs(it.x - target) }
    }

    /** Typical estrogen level (0..1) on [day]: peaks at O - 1.5 and O + 7. */
    fun estrogen(day: Double): Double =
        0.12 + 0.78 * g(day, ovulationDay - 1.5, 2.3) + 0.32 * g(day, ovulationDay + 7.0, 3.5)

    /** Typical progesterone level (0..1) on [day]: peaks at O + 7. */
    fun progesterone(day: Double): Double = 0.05 + 0.8 * g(day, ovulationDay + 7.0, 3.3)

    /** Typical LH level (0..1) on [day]: a sharp surge at O - 0.4. */
    fun lh(day: Double): Double = 0.06 + 0.88 * g(day, ovulationDay - 0.4, 0.75)

    /**
     * Where a phase label goes: the middle of cycle days [startDay]..[endDay] (inclusive),
     * or null for a phase shorter than 2 days.
     */
    fun labelDay(startDay: Int, endDay: Int): Double? =
        if (endDay - startDay + 1 < 2) null else (startDay + endDay) / 2.0

    companion object {
        const val WIDTH = 390.0
        const val HEIGHT = 172.0
        const val LEFT = 20.0
        const val SPAN = 350.0

        const val TIDE_MID = 72.0
        const val TIDE_AMPLITUDE = 40.0
        const val RIPPLE = 4.0

        /** Back wave layer offset from the front one. */
        const val BACK_DX = 11.0
        const val BACK_DY = -9.0

        /** y of the phase labels' centre line. */
        const val PHASE_LABEL_Y = 164.0

        /** Where the today marker's dotted line ends. */
        const val MARKER_LINE_END_Y = 146.0

        private const val TROCHOID_T_START = -7.0
        private const val TROCHOID_T_STEP = 0.15
        private const val TROCHOID_STEP_X = 4.4
        private const val TROCHOID_LOOP = 3.4

        /** y for a hormone level [v] (0..1). */
        fun hormoneY(v: Double): Double = 136 - 104 * v

        private fun g(d: Double, mean: Double, sd: Double): Double =
            exp(-(d - mean) * (d - mean) / (2 * sd * sd))
    }
}

/**
 * SVG path data for the lit shape of a moon at phase [p] (0 = new, 0.5 = full),
 * centred on ([cx], [cy]) with radius [r] (handoff "Moon shape for phase p").
 */
fun moonPathData(p: Double, cx: Double, cy: Double, r: Double): String {
    val rx = r * abs(cos(2 * PI * p))
    val waxing = p < 0.5
    val limb = if (waxing) 1 else 0
    val term = if (waxing) (if (p < 0.25) 0 else 1) else (if (p < 0.75) 0 else 1)
    val top = cy - r
    val bottom = cy + r
    // An arc with rx = 0 is a straight line in SVG; write it as one so no parser divides by 0.
    val terminator = if (rx < 1e-6) {
        "L${n(cx)} ${n(top)}"
    } else {
        "A${n(rx)} ${n(r)} 0 0 $term ${n(cx)} ${n(top)}"
    }
    return "M${n(cx)} ${n(top)}A${n(r)} ${n(r)} 0 0 $limb ${n(cx)} ${n(bottom)}${terminator}Z"
}

/**
 * The shadow over a moon at phase [p]: the moon shape half a cycle on, wrapped into
 * 0..1 (so a waning moon darkens from the right). Empty at full moon, the whole disc
 * at new moon.
 */
fun moonShadowPathData(p: Double, cx: Double, cy: Double, r: Double): String =
    moonPathData((p + 0.5).mod(1.0), cx, cy, r)

/** Locale-independent number for path data (never a decimal comma). */
private fun n(v: Double): String = String.format(Locale.ROOT, "%.3f", v)
