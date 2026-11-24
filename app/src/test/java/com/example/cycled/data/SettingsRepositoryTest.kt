package com.example.cycled.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** SettingsRepository against a real Preferences DataStore in a temp file. */
class SettingsRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dataStore = PreferenceDataStoreFactory.create(scope = scope) {
        File(tmp.root, "settings.preferences_pb")
    }
    private val repository = SettingsRepository(dataStore)

    @After
    fun tearDown() = scope.cancel()

    private fun current() = runBlocking { repository.settings.first() }

    @Test
    fun defaults_matchD9() {
        assertEquals(Settings(name = "", avgCycle = 28, avgPeriod = 5, remindPeriod = true, remindDaily = false), current())
    }

    @Test
    fun valuesAreSaved() = runBlocking {
        repository.setName("  Maya ")
        repository.setAvgCycle(31)
        repository.setAvgPeriod(6)
        repository.setRemindPeriod(false)
        repository.setRemindDaily(true)

        assertEquals(Settings("Maya", 31, 6, remindPeriod = false, remindDaily = true), current())
    }

    @Test
    fun averages_areClampedToTheirRanges() = runBlocking {
        repository.setAvgCycle(19)
        repository.setAvgPeriod(11)
        assertEquals(20, current().avgCycle)
        assertEquals(10, current().avgPeriod)

        repository.setAvgCycle(99)
        repository.setAvgPeriod(0)
        assertEquals(45, current().avgCycle)
        assertEquals(2, current().avgPeriod)
    }

    @Test
    fun badStoredValues_areClampedOnRead() = runBlocking {
        dataStore.edit { it[intPreferencesKey("avgCycle")] = 3 }
        assertEquals(20, current().avgCycle)
    }

    @Test
    fun clear_restoresTheDefaults() = runBlocking {
        repository.setName("Maya")
        repository.setAvgCycle(40)
        repository.setRemindDaily(true)
        repository.clear()
        assertEquals(Settings(), current())
    }
}
