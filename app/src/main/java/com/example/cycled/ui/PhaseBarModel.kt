package com.example.cycled.ui

import com.example.cycled.domain.CyclePhases
import com.example.cycled.domain.Phase
import com.example.cycled.domain.phaseOn
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** One phase on Home's phase bar. [fraction] is its share of the whole cycle. */
data class PhaseSegment(
    val phase: Phase,
    val start: LocalDate,
    val end: LocalDate,      // inclusive
    val fraction: Float,
)

data class PhaseBarModel(
    val segments: List<PhaseSegment>,   // cycle order; zero-length phases left out
    val todayFraction: Float,           // middle of today's day: (dayIndex + 0.5) / cycleLength
    val currentPhase: Phase,
    val cycleDay: Int,                  // 1 = first day of the cycle
    val cycleLength: Int,               // days from cycle start to the day before the predicted period
) {
    /** The predicted next period start: the day after the last segment. */
    val nextStart: LocalDate get() = segments.last().end.plusDays(1)

    /**
     * Horizontal spans (start, end) of each segment for a bar [totalWidth] wide with
     * [gap] between segments. Gaps come out of the total, so spans keep their fractions
     * relative to each other.
     */
    fun segmentSpans(totalWidth: Float, gap: Float): List<Pair<Float, Float>> {
        val available = (totalWidth - gap * (segments.size - 1)).coerceAtLeast(0f)
        var x = 0f
        return segments.map { segment ->
            val width = available * segment.fraction
            (x to x + width).also { x += width + gap }
        }
    }

    /** Where today's marker goes on that bar: inside the current segment, at today's share of it. */
    fun todayX(totalWidth: Float, gap: Float): Float {
        val spans = segmentSpans(totalWidth, gap)
        var before = 0f
        segments.forEachIndexed { i, segment ->
            if (todayFraction <= before + segment.fraction || i == segments.lastIndex) {
                val (start, end) = spans[i]
                val within = ((todayFraction - before) / segment.fraction).coerceIn(0f, 1f)
                return start + (end - start) * within
            }
            before += segment.fraction
        }
        return 0f
    }
}

/**
 * Builds Home's phase bar from [phases]. Returns null when [today] is not inside the
 * phase ranges, i.e. before the cycle started or on/after the predicted period (late).
 */
fun phaseBarModel(phases: CyclePhases, today: LocalDate): PhaseBarModel? {
    val currentPhase = phases.phaseOn(today) ?: return null
    val ranges = phases.ranges
    val cycleStart = ranges.first().start
    val cycleLength = ChronoUnit.DAYS.between(cycleStart, ranges.last().end).toInt() + 1
    val dayIndex = ChronoUnit.DAYS.between(cycleStart, today).toInt()

    return PhaseBarModel(
        segments = ranges.map {
            val days = ChronoUnit.DAYS.between(it.start, it.end).toInt() + 1
            PhaseSegment(it.phase, it.start, it.end, days.toFloat() / cycleLength)
        },
        todayFraction = (dayIndex + 0.5f) / cycleLength,
        currentPhase = currentPhase,
        cycleDay = dayIndex + 1,
        cycleLength = cycleLength,
    )
}
