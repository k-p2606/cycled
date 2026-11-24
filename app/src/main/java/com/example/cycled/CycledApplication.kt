package com.example.cycled

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.withTransaction
import com.example.cycled.data.CycleRepository
import com.example.cycled.data.CycledDatabase
import com.example.cycled.data.SettingsRepository
import com.example.cycled.domain.AverageCyclePredictor
import com.example.cycled.domain.CyclePredictor
import com.example.cycled.domain.MlCyclePredictor
import com.example.cycled.domain.parseCycleModel

/** One DataStore per process, as DataStore requires; the file is files/datastore/settings.preferences_pb. */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class CycledApplication : Application() {
    private val database: CycledDatabase by lazy { CycledDatabase.getInstance(this) }

    val repository: CycleRepository by lazy {
        CycleRepository(
            database.periodDayDao(),
            database.dailyLogDao(),
            inTransaction = { block -> database.withTransaction { block() } },
        )
    }

    /** Profile settings (D9). */
    val settings: SettingsRepository by lazy { SettingsRepository(settingsDataStore) }

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
