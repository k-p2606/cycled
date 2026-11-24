package com.example.cycled.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** A summary of her recent cycles, for display only; predictions come from [CyclePredictor]. */
data class CycleStats(
    /** Mean of the cycle lengths used, rounded to a whole day. */
    val averageCycleDays: Int,
    /** Mean length of her recent finished periods, rounded; null if none has finished. */
    val averagePeriodDays: Int?,
    /** Population standard deviation of the cycle lengths, rounded; null with fewer than 2. */
    val variationDays: Int?,
    /** How many cycle lengths the averages use (1..[CycleDefaults.MAX_CYCLES_USED]). */
    val cyclesUsed: Int,
)

/**
 * Summarises [periods] as of [today], using the same cycle window as [AverageCyclePredictor]:
 *
 * - Cycle lengths are the days between consecutive (de-duplicated) period starts. Lengths
 *   outside [CycleDefaults.MIN_VALID_CYCLE_DAYS]..[CycleDefaults.MAX_VALID_CYCLE_DAYS] are
 *   dropped, then the most recent [CycleDefaults.MAX_CYCLES_USED] are kept.
 * - The period average uses up to [CycleDefaults.MAX_CYCLES_USED] of the most recent finished
 *   periods. The latest period counts as still going (and is left out) if its last day is
 *   today or yesterday.
 * - Variation divides by n, as the predictor does.
 *
 * Returns null when there is no completed cycle. [periods] need not be sorted.
 */
fun cycleStats(periods: List<Period>, today: LocalDate): CycleStats? {
    val lengths = periods.map { it.start }.distinct().sorted()
        .zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b).toInt() }
        .filter { it in CycleDefaults.MIN_VALID_CYCLE_DAYS..CycleDefaults.MAX_VALID_CYCLE_DAYS }
        .takeLast(CycleDefaults.MAX_CYCLES_USED)
    if (lengths.isEmpty()) return null

    val mean = lengths.average()
    val variation = if (lengths.size < 2) {
        null
    } else {
        sqrt(lengths.sumOf { (it - mean) * (it - mean) } / lengths.size).roundToInt()
    }

    val sorted = periods.sortedBy { it.start }
    val latest = sorted.last()
    val finished = if (latest.end >= today.minusDays(1)) sorted.dropLast(1) else sorted
    val periodAverage = finished
        .takeLast(CycleDefaults.MAX_CYCLES_USED)
        .map { it.lengthDays }
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?.roundToInt()

    return CycleStats(
        averageCycleDays = mean.roundToInt(),
        averagePeriodDays = periodAverage,
        variationDays = variation,
        cyclesUsed = lengths.size,
    )
}
