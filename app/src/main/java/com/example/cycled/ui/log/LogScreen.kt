package com.example.cycled.ui.log

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cycled.data.FlowLevel
import com.example.cycled.data.PeriodDay
import com.example.cycled.ui.CycleViewModel
import com.example.cycled.ui.theme.CycledTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun LogRoute(viewModel: CycleViewModel, modifier: Modifier = Modifier) {
    val periodDays by viewModel.periodDays.collectAsStateWithLifecycle()
    LogScreen(
        periodDays = periodDays,
        onSave = viewModel::logPeriodDay,
        onRemove = viewModel::removePeriodDay,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    periodDays: List<PeriodDay>,
    onSave: (LocalDate, FlowLevel) -> Unit,
    onRemove: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()

    // Temporary UI state; saved data only ever comes from periodDays.
    var selectedEpochDay by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    var selectedFlow by rememberSaveable { mutableStateOf<FlowLevel?>(null) }
    // The date whose saved flow has already been copied into selectedFlow, so the
    // pre-fill runs once per date change and doesn't overwrite the user's choice
    // after rotation.
    var prefilledEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)
    val existing = periodDays.firstOrNull { it.date == selectedDate }

    fun selectDate(date: LocalDate) {
        selectedEpochDay = date.toEpochDay()
        selectedFlow = periodDays.firstOrNull { it.date == date }?.flow
        prefilledEpochDay = date.toEpochDay()
    }

    // Pre-select the saved flow if the data for the selected date arrives after
    // the date was chosen (e.g. today's entry on first load).
    LaunchedEffect(selectedEpochDay, existing) {
        if (existing != null && prefilledEpochDay != selectedEpochDay) {
            selectedFlow = existing.flow
            prefilledEpochDay = selectedEpochDay
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Log period", style = MaterialTheme.typography.headlineMedium)

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(selectedDate.format(DateFormatter))
            }

            Text("Flow", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowLevel.entries.forEach { level ->
                    FilterChip(
                        selected = selectedFlow == level,
                        onClick = { selectedFlow = level },
                        label = { Text(level.label) },
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        val flow = selectedFlow ?: return@Button
                        onSave(selectedDate, flow)
                        scope.launch { snackbarHostState.showSnackbar("Saved") }
                    },
                    enabled = selectedFlow != null,
                ) {
                    Text(if (existing != null) "Update" else "Save")
                }
                if (existing != null) {
                    TextButton(
                        onClick = {
                            onRemove(selectedDate)
                            scope.launch { snackbarHostState.showSnackbar("Removed") }
                        },
                    ) {
                        Text("Remove")
                    }
                }
            }

            HorizontalDivider()

            Text("Recently logged", style = MaterialTheme.typography.titleMedium)
            val recent = periodDays.sortedByDescending { it.date }.take(7)
            if (recent.isEmpty()) {
                Text(
                    "Nothing logged yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column {
                    recent.forEach { day ->
                        ListItem(
                            headlineContent = { Text(day.date.format(DateFormatter)) },
                            supportingContent = { Text(day.flow.label) },
                            modifier = Modifier.clickable { selectDate(day.date) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showDatePicker) {
        val todayMillis = today.toUtcMillis()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toUtcMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= todayMillis

                override fun isSelectableYear(year: Int): Boolean = year <= today.year
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { selectDate(it.utcMillisToLocalDate()) }
                        showDatePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private val DateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH)

private val FlowLevel.label: String
    get() = when (this) {
        FlowLevel.SPOTTING -> "Spotting"
        FlowLevel.LIGHT -> "Light"
        FlowLevel.MEDIUM -> "Medium"
        FlowLevel.HEAVY -> "Heavy"
    }

// The Material 3 date picker works in UTC milliseconds. Always convert via UTC,
// never ZoneId.systemDefault(), or dates shift by a day in negative time zones.
private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.utcMillisToLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@Preview(showBackground = true)
@Composable
private fun LogScreenEmptyPreview() {
    CycledTheme {
        LogScreen(periodDays = emptyList(), onSave = { _, _ -> }, onRemove = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun LogScreenWithEntriesPreview() {
    val today = LocalDate.now()
    CycledTheme {
        LogScreen(
            periodDays = listOf(
                PeriodDay(today.minusDays(2), FlowLevel.LIGHT),
                PeriodDay(today.minusDays(1), FlowLevel.HEAVY),
                PeriodDay(today, FlowLevel.MEDIUM),
            ),
            onSave = { _, _ -> },
            onRemove = {},
        )
    }
}
