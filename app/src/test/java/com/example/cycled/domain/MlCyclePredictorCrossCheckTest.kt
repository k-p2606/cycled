package com.example.cycled.domain

import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks MlCyclePredictor against predictions/ml_inference.py: ml_cases.json holds that
 * code's features and outputs for 16 histories, computed with the same cycled_model.json.
 */
class MlCyclePredictorCrossCheckTest {

    private val model = TestAssets.model()
    private val predictor = MlCyclePredictor(model)
    private val root = JSONObject(TestAssets.mlCasesJson())
    private val cases: List<JSONObject> =
        root.getJSONArray("cases").let { a -> List(a.length()) { a.getJSONObject(it) } }

    private fun history(case: JSONObject): List<MlCyclePredictor.PastCycle> {
        val rows = case.getJSONArray("history")
        return List(rows.length()) { i ->
            val row: JSONArray = rows.getJSONArray(i)
            MlCyclePredictor.PastCycle(row.getInt(0), if (row.isNull(1)) null else row.getInt(1))
        }
    }

    private fun assertClose(message: String, expected: Double, actual: Double) {
        assertEquals(message, expected, actual, TOLERANCE)
    }

    @Test
    fun casesWereMadeWithThisModelVersion() {
        assertEquals(model.version, root.getInt("model_version"))
        assertEquals(16, cases.size)
    }

    @Test
    fun featuresMatchPython() {
        for (case in cases) {
            val name = case.getString("name")
            val expected = case.getJSONObject("features")
            val actual = predictor.features(history(case))

            assertEquals(name, expected.keys().asSequence().toSet(), actual.keys)
            for (key in actual.keys) {
                assertClose("$name: $key", expected.getDouble(key), actual.getValue(key))
            }
        }
    }

    @Test
    fun outputsMatchPython() {
        for (case in cases) {
            val name = case.getString("name")
            val e = case.getJSONObject("expected")
            val out = predictor.infer(predictor.features(history(case)))

            assertClose("$name: cycle_raw", e.getDouble("cycle_raw"), out.cycleRaw)
            assertClose("$name: period_raw", e.getDouble("period_raw"), out.periodRaw)
            if (e.isNull("luteal_raw")) {
                assertNull("$name: luteal_raw", out.lutealRaw)
            } else {
                assertClose("$name: luteal_raw", e.getDouble("luteal_raw"), out.lutealRaw!!)
            }
            assertClose("$name: ovulation_raw", e.getDouble("ovulation_raw"), out.ovulationRaw)
            assertClose("$name: range_low_raw", e.getDouble("range_low_raw"), out.rangeLowRaw)
            assertClose("$name: range_high_raw", e.getDouble("range_high_raw"), out.rangeHighRaw)

            assertEquals("$name: cycle_days", e.getInt("cycle_days"), out.cycleDays)
            assertEquals("$name: period_days", e.getInt("period_days"), out.periodDays)
            assertEquals("$name: ovulation_day", e.getInt("ovulation_day"), out.ovulationDay)
            assertEquals("$name: range_low_days", e.getInt("range_low_days"), out.rangeLowDays)
            assertEquals("$name: range_high_days", e.getInt("range_high_days"), out.rangeHighDays)
        }
    }

    /**
     * End to end from logged periods: each case whose period lengths are all known is
     * turned into periods ending with a current period starting Nov 1, 2026.
     */
    @Test
    fun predictFromPeriods_matchesPython() {
        val lastStart = LocalDate.of(2026, 11, 1)
        var checked = 0
        for (case in cases) {
            val past = history(case)
            if (past.any { it.periodLengthDays == null }) continue
            val name = case.getString("name")
            val e = case.getJSONObject("expected")

            val periods = mutableListOf(Period(lastStart, lastStart.plusDays(2)))
            for (cycle in past.reversed()) {
                val start = periods.first().start.minusDays(cycle.lengthDays.toLong())
                periods.add(0, Period(start, start.plusDays(cycle.periodLengthDays!! - 1L)))
            }
            val prediction = predictor.predict(periods)!!

            assertEquals(name, lastStart.plusDays(e.getLong("cycle_days")), prediction.nextStart)
            assertEquals(name, lastStart.plusDays(e.getLong("range_low_days")), prediction.earliest)
            assertEquals(name, lastStart.plusDays(e.getLong("range_high_days")), prediction.latest)
            assertEquals(name, lastStart.plusDays(e.getLong("ovulation_day") - 1), prediction.ovulation)
            assertEquals(name, e.getInt("period_days"), prediction.periodLengthDays)
            assertEquals(name, case.getJSONObject("features").getDouble("n_cycles").toInt(), prediction.basedOnCycles)
            checked++
        }
        assertTrue("checked $checked cases", checked >= 10)
    }

    @Test
    fun ovulationIsAtLeastTheMinimumBeforeNextStart() {
        for (case in cases) {
            val out = predictor.infer(predictor.features(history(case)))
            val daysBeforeNextStart = out.cycleDays + 1 - out.ovulationDay
            assertTrue(case.getString("name"), daysBeforeNextStart >= model.ovulationMinDaysBeforeNextStart)
        }
    }

    @Test
    fun noPeriods_returnsNull() {
        assertNull(predictor.predict(emptyList()))
    }

    private companion object {
        const val TOLERANCE = 1e-6
    }
}
