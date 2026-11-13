package com.example.cycled.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cycled.domain.CycleStatus
import com.example.cycled.domain.Period
import com.example.cycled.ui.CycleViewModel
import com.example.cycled.ui.HomeUiState
import com.example.cycled.ui.theme.CycledTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeRoute(viewModel: CycleViewModel, onLogClick: () -> Unit) {
    val state by viewModel.homeState.collectAsStateWithLifecycle()
    HomeScreen(state = state, onLogClick = onLogClick)
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onLogClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Cycled", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(32.dp))

            when (state) {
                HomeUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }

                HomeUiState.NoData -> {
                    Text("No periods logged yet", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Log your last period to see where you are in your cycle.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    LogButton(onLogClick)
                }

                is HomeUiState.Ready -> {
                    ReadyContent(state.status)
                    LogButton(onLogClick)
                }
            }
        }
    }
}

@Composable
private fun ReadyContent(status: CycleStatus) {
    Text("Day ${status.cycleDay}", style = MaterialTheme.typography.displayLarge)

    val period = status.lastPeriod
    if (status.onPeriod) {
        Text("Period · day ${status.periodDay}", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            "Period started ${period.start.format(DateFormatter)}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Text("of your cycle", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            "Last period: ${period.start.format(DateFormatter)} – " +
                "${period.end.format(DateFormatter)} · ${daysLabel(period.lengthDays)}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LogButton(onClick: () -> Unit) {
    Spacer(Modifier.height(24.dp))
    Button(onClick = onClick) { Text("Log period") }
}

private fun daysLabel(days: Int): String = if (days == 1) "1 day" else "$days days"

/** "Nov 1" in the device's default locale. */
private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")

@Preview(showBackground = true)
@Composable
private fun HomeScreenLoadingPreview() {
    CycledTheme {
        HomeScreen(state = HomeUiState.Loading, onLogClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenNoDataPreview() {
    CycledTheme {
        HomeScreen(state = HomeUiState.NoData, onLogClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenMidCyclePreview() {
    val status = CycleStatus(
        lastPeriod = Period(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5)),
        cycleDay = 13,
        onPeriod = false,
        periodDay = null,
    )
    CycledTheme {
        HomeScreen(state = HomeUiState.Ready(status), onLogClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenOnPeriodPreview() {
    val status = CycleStatus(
        lastPeriod = Period(LocalDate.of(2026, 11, 11), LocalDate.of(2026, 11, 13)),
        cycleDay = 3,
        onPeriod = true,
        periodDay = 3,
    )
    CycledTheme {
        HomeScreen(state = HomeUiState.Ready(status), onLogClick = {})
    }
}
