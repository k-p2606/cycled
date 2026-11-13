package com.example.cycled.ui

import com.example.cycled.domain.CycleStatus

sealed interface HomeUiState {
    data object Loading : HomeUiState                         // before the database has answered
    data object NoData : HomeUiState                          // nothing logged yet
    data class Ready(val status: CycleStatus) : HomeUiState   // from domain.cycleStatus()
}
