package com.example.cycled.domain

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CycleModelParserTest {

    private val json = TestAssets.modelJson()

    @Test
    fun appModel_readsVersionFeaturesAndWeights() {
        val model = parseCycleModel(json)

        assertEquals(2, model.version)
        assertEquals(
            listOf("n_cycles", "mean_cycle", "last_cycle", "sd_cycle", "mean_period", "last_period"),
            model.features,
        )
        val linear = listOfNotNull(
            model.cycleLength, model.periodLength, model.ovulationDay,
            model.lutealLength, model.rangeLow, model.rangeHigh,
        )
        assertEquals(5, linear.size)   // direct ovulation: no luteal model
        linear.forEach { assertEquals(model.features.size, it.weights.size) }
        assertEquals(OvulationMethod.DIRECT, model.ovulationMethod)
        assertNull(model.lutealLength)
        assertEquals(listOf(15, 85), model.rangePercentiles)
        assertEquals(3, model.rangeMinHalfWidthDays)
        assertEquals(15..60, model.validCycleDays)
    }

    @Test
    fun appModel_readsExactNumbers() {
        val model = parseCycleModel(json)
        val cycle = JSONObject(json).getJSONObject("models").getJSONObject("cycle_length")

        assertEquals(cycle.getDouble("intercept"), model.cycleLength.intercept, 0.0)
        assertEquals(cycle.getJSONArray("weights").getDouble(1), model.cycleLength.weights[1], 0.0)
    }

    @Test
    fun malformedJson_failsClearly() {
        val e = assertThrows(IllegalArgumentException::class.java) { parseCycleModel("{ not json") }
        assertTrue(e.message, e.message!!.startsWith("Invalid cycle model JSON"))
    }

    @Test
    fun missingKey_failsClearly() {
        val broken = JSONObject(json).apply { remove("models") }.toString()

        val e = assertThrows(IllegalArgumentException::class.java) { parseCycleModel(broken) }
        assertTrue(e.message, e.message!!.contains("models"))
    }

    @Test
    fun wrongWeightCount_failsClearly() {
        val root = JSONObject(json)
        root.getJSONObject("models").getJSONObject("period_length").getJSONArray("weights").remove(0)

        val e = assertThrows(IllegalArgumentException::class.java) { parseCycleModel(root.toString()) }
        assertTrue(e.message, e.message!!.contains("period_length"))
    }

    @Test
    fun unknownFeature_failsClearly() {
        val root = JSONObject(json)
        root.getJSONArray("features").put(0, "age")

        val e = assertThrows(IllegalArgumentException::class.java) { parseCycleModel(root.toString()) }
        assertTrue(e.message, e.message!!.contains("features"))
    }
}
