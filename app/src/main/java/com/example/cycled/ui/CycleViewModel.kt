package com.example.cycled.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cycled.CycledApplication
import com.example.cycled.data.CycleRepository
import com.example.cycled.data.FlowLevel
import com.example.cycled.data.PeriodDay
import com.example.cycled.domain.Period
import com.example.cycled.domain.cycleStatus
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CycleViewModel(
    private val repository: CycleRepository,
    private val today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    /** The individual logged bleeding days, ordered by date, each with its flow level. */
    val periodDays: StateFlow<List<PeriodDay>> =
        repository.observePeriodDays()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Those days grouped into whole periods by groupIntoPeriods(). */
    val periods: StateFlow<List<Period>> =
        repository.observePeriods()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Home's state, recalculated whenever a period day is logged or removed. */
    val homeState: StateFlow<HomeUiState> =
        repository.observePeriods()
            .map { periods ->
                val status = cycleStatus(periods, today())
                if (status == null) HomeUiState.NoData else HomeUiState.Ready(status)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    fun logPeriodDay(date: LocalDate, flow: FlowLevel) {
        viewModelScope.launch { repository.logPeriodDay(date, flow) }
    }

    fun removePeriodDay(date: LocalDate) {
        viewModelScope.launch { repository.removePeriodDay(date) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CycledApplication
                CycleViewModel(app.repository)
            }
        }
    }
}
