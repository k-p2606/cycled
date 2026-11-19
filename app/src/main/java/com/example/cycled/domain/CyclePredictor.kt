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
    val periodLengthDays: Int,   // length of the current and the predicted period
    val ovulation: LocalDate,    // estimated ovulation day in the current cycle
)

/**
 * Predicts the next period, the period length and ovulation from the logged periods.
 *
 * This is the swap point for prediction: the app uses [MlCyclePredictor], with
 * [AverageCyclePredictor] as the rule-based baseline and fallback.
 */
interface CyclePredictor {
    /** Returns null if [periods] is empty. The list need not be sorted. */
    fun predict(periods: List<Period>): Prediction?
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
 * - The period length is the average length of up to [CycleDefaults.MAX_CYCLES_USED]
 *   periods before the latest one (which may still be going), or
 *   [CycleDefaults.PERIOD_LENGTH_DAYS] when there are none.
 * - Ovulation is [CycleDefaults.DAYS_FROM_OVULATION_TO_NEXT_START] days before the next start.
 */
class AverageCyclePredictor : CyclePredictor {

    override fun predict(periods: List<Period>): Prediction? {
        val starts = periods.map { it.start }.distinct().sorted()
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

        val latestPeriod = periods.maxBy { it.start }
        val pastPeriodLengths = (periods - latestPeriod)
            .sortedBy { it.start }
            .takeLast(CycleDefaults.MAX_CYCLES_USED)
            .map { it.lengthDays }
        val periodLengthDays = if (pastPeriodLengths.isEmpty()) {
            CycleDefaults.PERIOD_LENGTH_DAYS
        } else {
            pastPeriodLengths.average().roundToInt()
        }

        val nextStart = starts.last().plusDays(average.toLong())
        return Prediction(
            nextStart = nextStart,
            earliest = nextStart.minusDays(halfWidth.toLong()),
            latest = nextStart.plusDays(halfWidth.toLong()),
            basedOnCycles = lengths.size,
            periodLengthDays = periodLengthDays,
            ovulation = nextStart.minusDays(CycleDefaults.DAYS_FROM_OVULATION_TO_NEXT_START.toLong()),
        )
    }
}
