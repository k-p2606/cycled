package com.example.cycled.domain

import java.time.temporal.ChronoUnit
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Predicts with the trained linear models in [model]. A line-by-line port of
 * predictions/ml_inference.py; MlCyclePredictorCrossCheckTest checks it against that
 * code's outputs in ml_cases.json.
 *
 * Day convention (Marquette): cycle day 1 is the first day of the period, so
 * ovulation = last start + (ovulation day - 1) and next start = last start + cycle days.
 *
 * Features come only from completed cycles: each period followed by another period
 * start. The latest period starts the current cycle and is never a feature, so a
 * period that may still be going is never used for the period-length features.
 */
class MlCyclePredictor(private val model: CycleModel) : CyclePredictor {

    /** One completed cycle: its length, and the length of the period that started it (null if unknown). */
    data class PastCycle(val lengthDays: Int, val periodLengthDays: Int?)

    /** Raw model outputs and the whole-day values the app uses (names as in ml_inference.py). */
    data class Output(
        val cycleRaw: Double,
        val periodRaw: Double,
        val lutealRaw: Double?,
        val ovulationRaw: Double,
        val rangeLowRaw: Double,
        val rangeHighRaw: Double,
        val cycleDays: Int,
        val periodDays: Int,
        val ovulationDay: Int,        // cycle day, day 1 = first day of the period
        val rangeLowDays: Int,        // offsets from the last period start
        val rangeHighDays: Int,
    )

    override fun predict(periods: List<Period>): Prediction? {
        val ordered = periods.sortedBy { it.start }.distinctBy { it.start }
        if (ordered.isEmpty()) return null

        val history = ordered.zipWithNext { a, b ->
            PastCycle(ChronoUnit.DAYS.between(a.start, b.start).toInt(), a.lengthDays)
        }
        val features = features(history)
        val out = infer(features)

        val lastStart = ordered.last().start
        val nextStart = lastStart.plusDays(out.cycleDays.toLong())
        return Prediction(
            nextStart = nextStart,
            earliest = lastStart.plusDays(out.rangeLowDays.toLong()),
            latest = lastStart.plusDays(out.rangeHighDays.toLong()),
            basedOnCycles = features.getValue("n_cycles").toInt(),
            periodLengthDays = out.periodDays,
            // infer() already keeps this at least ovulationMinDaysBeforeNextStart before nextStart.
            ovulation = lastStart.plusDays(out.ovulationDay - 1L),
        )
    }

    /**
     * Features from the last [CycleModel.maxCyclesUsed] valid cycles of [history] (oldest
     * first), with population fallbacks. Same as compute_features() in ml_inference.py:
     * - n_cycles: valid earlier cycles used
     * - mean_cycle, last_cycle: of those cycles, else the fallback cycle length
     * - sd_cycle: population SD (divide by n) when n >= 2, else the fallback SD
     * - mean_period, last_period: of the known period lengths in those cycles, else the fallback
     */
    fun features(history: List<PastCycle>): Map<String, Double> {
        val used = history.filter { it.lengthDays in model.validCycleDays }.takeLast(model.maxCyclesUsed)
        val cycles = used.map { it.lengthDays }
        val periods = used.mapNotNull { it.periodLengthDays }
        val n = cycles.size

        val meanCycle: Double
        val lastCycle: Double
        if (n > 0) {
            meanCycle = cycles.sum().toDouble() / n
            lastCycle = cycles.last().toDouble()
        } else {
            meanCycle = model.fallbackCycleLength
            lastCycle = model.fallbackCycleLength
        }
        val sdCycle = if (n >= 2) {
            var squares = 0.0
            for (c in cycles) {
                val d = c - meanCycle
                squares += d * d
            }
            sqrt(squares / n)
        } else {
            model.fallbackSdCycle
        }
        val meanPeriod: Double
        val lastPeriod: Double
        if (periods.isNotEmpty()) {
            meanPeriod = periods.sum().toDouble() / periods.size
            lastPeriod = periods.last().toDouble()
        } else {
            meanPeriod = model.fallbackPeriodLength
            lastPeriod = model.fallbackPeriodLength
        }

        return linkedMapOf(
            "n_cycles" to n.toDouble(),
            "mean_cycle" to meanCycle,
            "last_cycle" to lastCycle,
            "sd_cycle" to sdCycle,
            "mean_period" to meanPeriod,
            "last_period" to lastPeriod,
        )
    }

    /** Raw outputs and whole-day values for [features]. Same as predict() in ml_inference.py. */
    fun infer(features: Map<String, Double>): Output {
        val x = DoubleArray(model.features.size) { features.getValue(model.features[it]) }

        val cycleRaw = model.cycleLength.apply(x)
        val cycleDays = clamp(roundHalfUp(cycleRaw), model.cycleDaysClamp)

        val periodRaw = model.periodLength.apply(x)
        val periodDays = clamp(roundHalfUp(periodRaw), model.periodDaysClamp)

        val lutealRaw: Double?
        val ovulationRaw: Double
        when (model.ovulationMethod) {
            OvulationMethod.LUTEAL -> {
                lutealRaw = model.lutealLength!!.apply(x)
                ovulationRaw = cycleRaw - lutealRaw
            }
            OvulationMethod.DIRECT -> {
                lutealRaw = null
                ovulationRaw = model.ovulationDay!!.apply(x)
            }
        }
        // Ovulation at least ovulationMinDaysBeforeNextStart before the next start (day cycleDays + 1).
        val latestOvulation = cycleDays + 1 - model.ovulationMinDaysBeforeNextStart
        val ovulationDay = clamp(roundHalfUp(ovulationRaw), 1..latestOvulation)

        val rangeLowRaw = model.rangeLow.apply(x)
        val rangeHighRaw = model.rangeHigh.apply(x)
        val half = model.rangeMinHalfWidthDays
        val rangeLowDays = minOf(roundHalfUp(rangeLowRaw), cycleDays - half)
        val rangeHighDays = maxOf(roundHalfUp(rangeHighRaw), cycleDays + half)

        return Output(
            cycleRaw = cycleRaw,
            periodRaw = periodRaw,
            lutealRaw = lutealRaw,
            ovulationRaw = ovulationRaw,
            rangeLowRaw = rangeLowRaw,
            rangeHighRaw = rangeHighRaw,
            cycleDays = cycleDays,
            periodDays = periodDays,
            ovulationDay = ovulationDay,
            rangeLowDays = rangeLowDays,
            rangeHighDays = rangeHighDays,
        )
    }

    private companion object {
        /** Python's floor(x + 0.5), which matches Kotlin's roundToInt(): ties round up. */
        fun roundHalfUp(x: Double): Int = floor(x + 0.5).toInt()

        /** Python's max(low, min(x, high)): never throws, even if the range is empty. */
        fun clamp(x: Int, range: IntRange): Int = maxOf(range.first, minOf(x, range.last))
    }
}
