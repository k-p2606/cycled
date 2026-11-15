package com.example.cycled.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import com.example.cycled.domain.AverageCyclePredictor
import com.example.cycled.domain.CycleDefaults
import com.example.cycled.domain.CycleStatus
import com.example.cycled.domain.Period
import com.example.cycled.domain.Phase
import com.example.cycled.ui.CycleViewModel
import com.example.cycled.ui.HomeUiState
import com.example.cycled.ui.buildHomeState
import com.example.cycled.ui.theme.CycledTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun HomeRoute(viewModel: CycleViewModel, onLogClick: () -> Unit, onCalendarClick: () -> Unit) {
    val state by viewModel.homeState.collectAsStateWithLifecycle()
    HomeScreen(state = state, onLogClick = onLogClick, onCalendarClick = onCalendarClick)
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onLogClick: () -> Unit,
    onCalendarClick: () -> Unit,
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
                    HomeActions(onLogClick = onLogClick, onCalendarClick = onCalendarClick)
                }

                is HomeUiState.Ready -> {
                    ReadyContent(state.status)
                    Spacer(Modifier.height(16.dp))
                    NextPeriodCard(state)
                    CurrentPhaseCard(state)
                    Text(
                        "Estimates based on your logged dates. Not medical advice or contraception.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    HomeActions(onLogClick = onLogClick, onCalendarClick = onCalendarClick)
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
private fun NextPeriodCard(state: HomeUiState.Ready) {
    val prediction = state.prediction
    // Today, worked out from the status so this screen never reads the clock.
    val today = state.status.lastPeriod.start.plusDays(state.status.cycleDay - 1L)
    val range = "Likely between ${prediction.earliest.format(DateFormatter)} and " +
        prediction.latest.format(DateFormatter)

    val (headline, detail) = when {
        state.isLate ->
            "Your period may be late" to "Expected around ${prediction.nextStart.format(DateFormatter)}"
        today == prediction.nextStart ->
            "Expected today" to range
        else -> {
            val daysUntil = ChronoUnit.DAYS.between(today, prediction.nextStart).toInt()
            "Around ${prediction.nextStart.format(DateFormatter)}" to "$range · in ${daysLabel(daysUntil)}"
        }
    }

    val basis = when (prediction.basedOnCycles) {
        0 -> "Based on a typical ${CycleDefaults.CYCLE_LENGTH_DAYS}-day cycle until you've logged two periods"
        1 -> "Based on your last cycle"
        else -> "Based on your last ${prediction.basedOnCycles} cycles"
    }

    InfoCard(label = "Next period", headline = headline, detail = detail, footnote = basis)
}

@Composable
private fun CurrentPhaseCard(state: HomeUiState.Ready) {
    val phase = state.currentPhase ?: return
    val range = state.phases.ranges.firstOrNull { it.phase == phase }

    InfoCard(
        label = "Current phase",
        headline = phase.title,
        detail = range?.let { "${it.start.format(DateFormatter)} – ${it.end.format(DateFormatter)}" },
        footnote = phase.description,
    )
}

@Composable
private fun InfoCard(label: String, headline: String, detail: String?, footnote: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(headline, style = MaterialTheme.typography.titleLarge)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodyLarge)
            }
            Text(
                footnote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val Phase.title: String
    get() = when (this) {
        Phase.MENSTRUAL -> "Menstrual"
        Phase.FOLLICULAR -> "Follicular"
        Phase.OVULATORY -> "Ovulatory"
        Phase.LUTEAL -> "Luteal"
    }

private val Phase.description: String
    get() = when (this) {
        Phase.MENSTRUAL -> "Your period"
        Phase.FOLLICULAR -> "After your period, before ovulation"
        Phase.OVULATORY -> "Around your estimated ovulation"
        Phase.LUTEAL -> "After ovulation, before your next period"
    }

@Composable
private fun HomeActions(onLogClick: () -> Unit, onCalendarClick: () -> Unit) {
    Spacer(Modifier.height(24.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onLogClick) { Text("Log period") }
        Button(onClick = onCalendarClick) { Text("Calendar") }
    }
}

private fun daysLabel(days: Int): String = if (days == 1) "1 day" else "$days days"

/** "Nov 1" in the device's default locale. */
private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")

@Preview(showBackground = true)
@Composable
private fun HomeScreenLoadingPreview() {
    CycledTheme {
        HomeScreen(state = HomeUiState.Loading, onLogClick = {}, onCalendarClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenNoDataPreview() {
    CycledTheme {
        HomeScreen(state = HomeUiState.NoData, onLogClick = {}, onCalendarClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenMidCyclePreview() {
    val state = buildHomeState(
        periods = listOf(Period(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5))),
        today = LocalDate.of(2026, 11, 13),
        predictor = AverageCyclePredictor(),
    )
    CycledTheme {
        HomeScreen(state = state, onLogClick = {}, onCalendarClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenOnPeriodPreview() {
    val state = buildHomeState(
        periods = listOf(Period(LocalDate.of(2026, 11, 11), LocalDate.of(2026, 11, 13))),
        today = LocalDate.of(2026, 11, 13),
        predictor = AverageCyclePredictor(),
    )
    CycledTheme {
        HomeScreen(state = state, onLogClick = {}, onCalendarClick = {})
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun HomeScreenPlaceholderPredictionPreview() {
    val state = buildHomeState(
        periods = listOf(Period(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5))),
        today = LocalDate.of(2026, 11, 13),
        predictor = AverageCyclePredictor(),
    )
    CycledTheme {
        HomeScreen(state = state, onLogClick = {}, onCalendarClick = {})
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun HomeScreenOwnDataPreview() {
    val state = buildHomeState(
        periods = listOf(
            Period(LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 7)),
            Period(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5)),
        ),
        today = LocalDate.of(2026, 11, 13),
        predictor = AverageCyclePredictor(),
    )
    CycledTheme {
        HomeScreen(state = state, onLogClick = {}, onCalendarClick = {})
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun HomeScreenLatePreview() {
    val state = buildHomeState(
        periods = listOf(Period(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5))),
        today = LocalDate.of(2026, 12, 2),
        predictor = AverageCyclePredictor(),
    )
    CycledTheme {
        HomeScreen(state = state, onLogClick = {}, onCalendarClick = {})
    }
}
