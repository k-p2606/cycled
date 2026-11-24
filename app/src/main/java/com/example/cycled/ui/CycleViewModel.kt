package com.example.cycled.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cycled.CycledApplication
import com.example.cycled.data.CycleRepository
import com.example.cycled.data.FlowChoice
import com.example.cycled.data.Mood
import com.example.cycled.data.PeriodDay
import com.example.cycled.data.Symptom
import com.example.cycled.domain.AverageCyclePredictor
import com.example.cycled.domain.CyclePredictor
import com.example.cycled.domain.Period
import com.example.cycled.domain.groupIntoPeriods
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CycleViewModel(
    private val repository: CycleRepository,
    /** Today's date. Screens call `viewModel.today()` so tests and previews can supply a fixed date. */
    val today: () -> LocalDate = { LocalDate.now() },
    /** The app passes CycledApplication.predictor (the ML models); the default is for tests and previews. */
    private val predictor: CyclePredictor = AverageCyclePredictor(),
    /** Keeps Calendar's selected day across process death; the ViewModel itself survives rotation. */
    private val savedState: SavedStateHandle = SavedStateHandle(),
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
            .map { periods -> buildHomeState(periods, today(), predictor) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    /** Calendar's selected day, as an epoch day in [savedState]. */
    private val selectedDate: StateFlow<LocalDate> =
        savedState.getStateFlow(KEY_SELECTED_DAY, today().toEpochDay())
            .map { LocalDate.ofEpochDay(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, today())

    /**
     * Calendar's state, recalculated whenever a day is logged or changed, or another
     * day is selected. Logged days and periods come from the same emission so they
     * never disagree.
     */
    val calendarState: StateFlow<CalendarUiState> =
        combine(repository.observePeriodDays(), repository.observeAllLogs(), selectedDate) { days, logs, selected ->
            buildCalendarState(
                loggedDays = days.associate { it.date to it.flow },
                periods = groupIntoPeriods(days.map { it.date }),
                today = today(),
                predictor = predictor,
                dailyLogs = logs.associateBy { it.date },
                selected = selected,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            buildCalendarState(emptyMap(), emptyList(), today(), predictor, selected = selectedDate.value),
        )

    /** Logs' cycles, recalculated whenever any day's flow, symptoms or mood changes. */
    val logsState: StateFlow<LogsUiState> =
        combine(repository.observePeriodDays(), repository.observeAllLogs()) { days, logs ->
            buildLogsState(
                periods = groupIntoPeriods(days.map { it.date }),
                periodDays = days.associate { it.date to it.flow },
                dailyLogs = logs.associateBy { it.date },
                today = today(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LogsUiState.Loading)

    /** Shows [date] in Calendar's log panel. */
    fun selectDate(date: LocalDate) {
        savedState[KEY_SELECTED_DAY] = date.toEpochDay()
    }

    /** Sets or clears [date]'s flow (D7). Future days can't be logged. */
    fun setFlow(date: LocalDate, choice: FlowChoice?) = edit(date) { repository.setFlow(date, choice) }

    fun toggleSymptom(date: LocalDate, symptom: Symptom) = edit(date) { repository.toggleSymptom(date, symptom) }

    fun setMood(date: LocalDate, mood: Mood?) = edit(date) { repository.setMood(date, mood) }

    private fun edit(date: LocalDate, block: suspend () -> Unit) {
        if (date.isAfter(today())) return
        viewModelScope.launch { block() }
    }

    companion object {
        private const val KEY_SELECTED_DAY = "calendar_selected_day"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CycledApplication
                CycleViewModel(app.repository, predictor = app.predictor, savedState = createSavedStateHandle())
            }
        }
    }
}
