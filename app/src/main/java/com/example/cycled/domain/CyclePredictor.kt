package com.example.cycled.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class Prediction(
    val nextStart: LocalDate,
    val earliest: LocalDate,
    val latest: LocalDate,
    val basedOnCycles: Int,   // how many of the user's cycle lengths were used; 0 = placeholder length
)

/**
 * Predicts when the next period will start from the start dates of past periods.
 *
 * This is the swap point for prediction: [AverageCyclePredictor] is used for now,
 * and the trained LinearCyclePredictor will replace it in session 12 without
 * callers needing to change.
 */
interface CyclePredictor {
    /** Returns null if [periodStarts] is empty. The list need not be sorted. */
    fun predict(periodStarts: List<LocalDate>): Prediction?
}

/**
 * Predicts the next start as the latest start plus the average recent cycle length.
 *
 * - Starts are sorted and de-duplicated; cycle lengths are the days between them.
 * - Lengths outside [CycleDefaults.MIN_VALID_CYCLE_DAYS]..[CycleDefaults.MAX_VALID_CYCLE_DAYS]
 *   are dropped (they usually mean a missed log), then the most recent
 *   [CycleDefaults.MAX_CYCLES_USED] are kept.
 * - With no usable lengths, [CycleDefaults.CYCLE_LENGTH_DAYS] is used and basedOnCycles is 0.
 * - The range is ±3 days with fewer than 3 lengths, otherwise ± the population
 *   standard deviation of the lengths, rounded and clamped to
 *   [CycleDefaults.MIN_RANGE_HALF_WIDTH_DAYS]..7 days.
 */
class AverageCyclePredictor : CyclePredictor {

    override fun predict(periodStarts: List<LocalDate>): Prediction? {
        val starts = periodStarts.distinct().sorted()
        if (starts.isEmpty()) return null

        val lengths = starts.zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b).toInt() }
            .filter { it in CycleDefaults.MIN_VALID_CYCLE_DAYS..CycleDefaults.MAX_VALID_CYCLE_DAYS }
            .takeLast(CycleDefaults.MAX_CYCLES_USED)

        val average = if (lengths.isEmpty()) {
            CycleDefaults.CYCLE_LENGTH_DAYS
        } else {
            lengths.average().roundToInt()
        }

        val halfWidth = if (lengths.size < 3) {
            3
        } else {
            val mean = lengths.average()
            val variance = lengths.sumOf { (it - mean) * (it - mean) } / lengths.size
            sqrt(variance).roundToInt().coerceIn(CycleDefaults.MIN_RANGE_HALF_WIDTH_DAYS, 7)
        }

        val nextStart = starts.last().plusDays(average.toLong())
        return Prediction(
            nextStart = nextStart,
            earliest = nextStart.minusDays(halfWidth.toLong()),
            latest = nextStart.plusDays(halfWidth.toLong()),
            basedOnCycles = lengths.size,
        )
    }
}
