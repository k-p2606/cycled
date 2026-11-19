package com.example.cycled.domain

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** One linear model: intercept + sum(weights[i] * features[i]), features in [CycleModel.features] order. */
data class LinearModel(val intercept: Double, val weights: List<Double>) {
    fun apply(features: DoubleArray): Double {
        var sum = 0.0
        for (i in weights.indices) sum += weights[i] * features[i]
        return intercept + sum
    }
}

enum class OvulationMethod { DIRECT, LUTEAL }

/**
 * The trained models and their settings, as exported by predictions/ml_models.py to
 * assets/cycled_model.json. Plain data only; [MlCyclePredictor] does the maths.
 */
data class CycleModel(
    val version: Int,
    val features: List<String>,               // feature order for every model's weights
    val validCycleDays: IntRange,             // cycle lengths outside this are ignored
    val maxCyclesUsed: Int,                   // only the most recent valid cycles are used
    val fallbackCycleLength: Double,          // population values used when history is missing
    val fallbackPeriodLength: Double,
    val fallbackSdCycle: Double,
    val ovulationMethod: OvulationMethod,
    val cycleLength: LinearModel,
    val periodLength: LinearModel,
    val ovulationDay: LinearModel?,           // used when ovulationMethod is DIRECT
    val lutealLength: LinearModel?,           // used when ovulationMethod is LUTEAL
    val rangeLow: LinearModel,
    val rangeHigh: LinearModel,
    val cycleDaysClamp: IntRange,
    val periodDaysClamp: IntRange,
    val ovulationMinDaysBeforeNextStart: Int,
    val rangePercentiles: List<Int>,          // informational, e.g. [15, 85]
    val rangeMinHalfWidthDays: Int,           // the range covers at least the point prediction ± this
) {
    companion object {
        /** The features the inference code knows how to compute. */
        val KNOWN_FEATURES = listOf("n_cycles", "mean_cycle", "last_cycle", "sd_cycle", "mean_period", "last_period")
    }
}

/**
 * Builds a [CycleModel] from the JSON exported by predictions/ml_models.py.
 *
 * Throws [IllegalArgumentException] with a description when the JSON is malformed,
 * a key is missing, or the contents are inconsistent (unknown features, wrong weight counts).
 */
fun parseCycleModel(json: String): CycleModel {
    try {
        val root = JSONObject(json)
        val features = root.getJSONArray("features").toStringList()
        require(features.sorted() == CycleModel.KNOWN_FEATURES.sorted()) {
            "features must be ${CycleModel.KNOWN_FEATURES} in some order, was $features"
        }

        val models = root.getJSONObject("models")
        fun linear(name: String): LinearModel {
            val m = models.getJSONObject(name)
            val weights = m.getJSONArray("weights").toDoubleList()
            require(weights.size == features.size) {
                "model '$name' has ${weights.size} weights for ${features.size} features"
            }
            return LinearModel(m.getDouble("intercept"), weights)
        }

        val method = when (val m = root.getString("ovulation_method")) {
            "direct" -> OvulationMethod.DIRECT
            "luteal" -> OvulationMethod.LUTEAL
            else -> throw IllegalArgumentException("unknown ovulation_method '$m'")
        }
        val fallbacks = root.getJSONObject("fallbacks")
        val clamps = root.getJSONObject("clamps")

        return CycleModel(
            version = root.getInt("version"),
            features = features,
            validCycleDays = root.getJSONArray("valid_cycle_days").toIntRange(),
            maxCyclesUsed = root.getInt("max_cycles_used"),
            fallbackCycleLength = fallbacks.getDouble("cycle_length"),
            fallbackPeriodLength = fallbacks.getDouble("period_length"),
            fallbackSdCycle = fallbacks.getDouble("sd_cycle"),
            ovulationMethod = method,
            cycleLength = linear("cycle_length"),
            periodLength = linear("period_length"),
            ovulationDay = if (method == OvulationMethod.DIRECT) linear("ovulation_day") else null,
            lutealLength = if (method == OvulationMethod.LUTEAL) linear("luteal_length") else null,
            rangeLow = linear("range_low"),
            rangeHigh = linear("range_high"),
            cycleDaysClamp = clamps.getJSONArray("cycle_days").toIntRange(),
            periodDaysClamp = clamps.getJSONArray("period_days").toIntRange(),
            ovulationMinDaysBeforeNextStart = clamps.getInt("ovulation_min_days_before_next_start"),
            rangePercentiles = root.optJSONArray("range_percentiles")?.toIntList().orEmpty(),
            rangeMinHalfWidthDays = root.getInt("range_min_half_width_days"),
        )
    } catch (e: JSONException) {
        throw IllegalArgumentException("Invalid cycle model JSON: ${e.message}", e)
    }
}

private fun JSONArray.toStringList(): List<String> = List(length()) { getString(it) }

private fun JSONArray.toDoubleList(): List<Double> = List(length()) { getDouble(it) }

private fun JSONArray.toIntList(): List<Int> = List(length()) { getInt(it) }

private fun JSONArray.toIntRange(): IntRange {
    require(length() == 2) { "expected [low, high], got $this" }
    return getInt(0)..getInt(1)
}
