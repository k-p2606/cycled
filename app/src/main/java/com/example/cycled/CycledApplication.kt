package com.example.cycled

import android.app.Application
import android.util.Log
import com.example.cycled.data.CycleRepository
import com.example.cycled.data.CycledDatabase
import com.example.cycled.domain.AverageCyclePredictor
import com.example.cycled.domain.CyclePredictor
import com.example.cycled.domain.MlCyclePredictor
import com.example.cycled.domain.parseCycleModel

class CycledApplication : Application() {
    private val database: CycledDatabase by lazy { CycledDatabase.getInstance(this) }

    val repository: CycleRepository by lazy {
        CycleRepository(database.periodDayDao(), database.dailyLogDao())
    }

    /**
     * The trained models from assets/cycled_model.json, loaded once. If the file can't be
     * read or parsed, the rule-based [AverageCyclePredictor] is used instead.
     */
    val predictor: CyclePredictor by lazy { loadPredictor() }

    private fun loadPredictor(): CyclePredictor = try {
        val json = assets.open(MODEL_ASSET).bufferedReader().use { it.readText() }
        MlCyclePredictor(parseCycleModel(json))
    } catch (e: Exception) {
        Log.e(TAG, "Couldn't load $MODEL_ASSET; using AverageCyclePredictor", e)
        AverageCyclePredictor()
    }

    private companion object {
        const val TAG = "CycledApplication"
        const val MODEL_ASSET = "cycled_model.json"
    }
}
