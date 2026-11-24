package com.example.cycled.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Profile settings (D9). The averages are saved only for now; nothing reads them yet. */
data class Settings(
    val name: String = "",
    val avgCycle: Int = DEFAULT_AVG_CYCLE,
    val avgPeriod: Int = DEFAULT_AVG_PERIOD,
    /** Saved only; no notifications yet. */
    val remindPeriod: Boolean = true,
    /** Saved only; no notifications yet. */
    val remindDaily: Boolean = false,
) {
    companion object {
        const val DEFAULT_AVG_CYCLE = 28
        const val DEFAULT_AVG_PERIOD = 5
        val AVG_CYCLE_RANGE = 20..45
        val AVG_PERIOD_RANGE = 2..10
    }
}

/**
 * Settings in Preferences DataStore. Values are clamped to their ranges both when
 * saved and when read, so a bad stored value can't reach the UI.
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<Settings> = dataStore.data.map { p ->
        Settings(
            name = p[NAME] ?: "",
            avgCycle = (p[AVG_CYCLE] ?: Settings.DEFAULT_AVG_CYCLE).coerceIn(Settings.AVG_CYCLE_RANGE),
            avgPeriod = (p[AVG_PERIOD] ?: Settings.DEFAULT_AVG_PERIOD).coerceIn(Settings.AVG_PERIOD_RANGE),
            remindPeriod = p[REMIND_PERIOD] ?: true,
            remindDaily = p[REMIND_DAILY] ?: false,
        )
    }

    suspend fun setName(name: String) = dataStore.edit { it[NAME] = name.trim() }

    suspend fun setAvgCycle(days: Int) = dataStore.edit { it[AVG_CYCLE] = days.coerceIn(Settings.AVG_CYCLE_RANGE) }

    suspend fun setAvgPeriod(days: Int) = dataStore.edit { it[AVG_PERIOD] = days.coerceIn(Settings.AVG_PERIOD_RANGE) }

    suspend fun setRemindPeriod(on: Boolean) = dataStore.edit { it[REMIND_PERIOD] = on }

    suspend fun setRemindDaily(on: Boolean) = dataStore.edit { it[REMIND_DAILY] = on }

    /** Back to the defaults (part of "Delete all my data"). */
    suspend fun clear() = dataStore.edit { it.clear() }

    private companion object {
        val NAME = stringPreferencesKey("name")
        val AVG_CYCLE = intPreferencesKey("avgCycle")
        val AVG_PERIOD = intPreferencesKey("avgPeriod")
        val REMIND_PERIOD = booleanPreferencesKey("remindPeriod")
        val REMIND_DAILY = booleanPreferencesKey("remindDaily")
    }
}
