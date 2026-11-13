package com.example.cycled.ui

import com.example.cycled.domain.CyclePhases
import com.example.cycled.domain.CycleStatus
import com.example.cycled.domain.Phase
import com.example.cycled.domain.Prediction

sealed interface HomeUiState {
    data object Loading : HomeUiState                         // before the database has answered
    data object NoData : HomeUiState                          // nothing logged yet
    data class Ready(
        val status: CycleStatus,                              // from domain.cycleStatus()
        val prediction: Prediction,
        val phases: CyclePhases,
        val currentPhase: Phase?,   // null when today is on or after the predicted start
        val isLate: Boolean,        // true when today is after prediction.nextStart
    ) : HomeUiState
}
