package com.example.cycled.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cycled.CycledApplication
import com.example.cycled.data.CycleRepository
import com.example.cycled.data.SettingsRepository
import com.example.cycled.ui.SampleData
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Profile's state and actions: settings (D9), deleting everything, and loading sample data (D12). */
class ProfileViewModel(
    private val settings: SettingsRepository,
    private val cycles: CycleRepository,
    private val today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    /** Null until DataStore and the database have answered. */
    val state: StateFlow<ProfileUiState?> =
        combine(settings.settings, cycles.observePeriods()) { s, periods -> buildProfileState(s, periods, today()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setAvgCycle(days: Int) = launch { settings.setAvgCycle(days) }
    fun setAvgPeriod(days: Int) = launch { settings.setAvgPeriod(days) }
    fun setRemindPeriod(on: Boolean) = launch { settings.setRemindPeriod(on) }
    fun setRemindDaily(on: Boolean) = launch { settings.setRemindDaily(on) }

    /** Clears both tables and every setting. */
    fun deleteAllData() = launch {
        cycles.deleteAll()
        settings.clear()
    }

    /** Debug builds only: replaces all logged data with the handoff's sample CSV. Settings are kept. */
    fun loadSampleData() = launch { cycles.replaceAll(SampleData.periodDays, SampleData.dailyLogs.values) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CycledApplication
                ProfileViewModel(app.settings, app.repository)
            }
        }
    }
}
