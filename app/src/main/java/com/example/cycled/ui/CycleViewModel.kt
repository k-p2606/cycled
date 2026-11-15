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
import com.example.cycled.domain.AverageCyclePredictor
import com.example.cycled.domain.CyclePredictor
import com.example.cycled.domain.Period
import com.example.cycled.domain.groupIntoPeriods
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CycleViewModel(
    private val repository: CycleRepository,
    /** Today's date. Screens call `viewModel.today()` so tests and previews can supply a fixed date. */
    val today: () -> LocalDate = { LocalDate.now() },
    private val predictor: CyclePredictor = AverageCyclePredictor(),
) : ViewModel() {

    /** The individual logged bleeding days, ordered by date, each with its flow level. */
    val periodDays: StateFlow<List<PeriodDay>> =
        repository.observePeriodDays()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The days most recently saved (newest save first), for Log's "Recently logged" list. */
    val recentlyLoggedDays: StateFlow<List<PeriodDay>> =
        repository.observeRecentlyLoggedDays(RECENTLY_LOGGED_LIMIT)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Those days grouped into whole periods by groupIntoPeriods(). */
    val periods: StateFlow<List<Period>> =
        repository.observePeriods()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Home's state, recalculated whenever a period day is logged or removed. */
    val homeState: StateFlow<HomeUiState> =
        repository.observePeriods()
            .map { periods -> buildHomeState(periods, today(), predictor) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    /**
     * Calendar's state, recalculated whenever a period day is logged or removed.
     * Logged days and periods come from the same emission so they never disagree.
     */
    val calendarState: StateFlow<CalendarUiState> =
        repository.observePeriodDays()
            .map { days ->
                buildCalendarState(
                    loggedDays = days.associate { it.date to it.flow },
                    periods = groupIntoPeriods(days.map { it.date }),
                    today = today(),
                    predictor = predictor,
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                buildCalendarState(emptyMap(), emptyList(), today(), predictor),
            )

    fun logPeriodDay(date: LocalDate, flow: FlowLevel) {
        viewModelScope.launch { repository.logPeriodDay(date, flow) }
    }

    fun removePeriodDay(date: LocalDate) {
        viewModelScope.launch { repository.removePeriodDay(date) }
    }

    companion object {
        /** How many entries Log's "Recently logged" list shows. */
        const val RECENTLY_LOGGED_LIMIT = 7

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CycledApplication
                CycleViewModel(app.repository)
            }
        }
    }
}
