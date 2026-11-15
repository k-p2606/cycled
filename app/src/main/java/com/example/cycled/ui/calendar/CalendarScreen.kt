package com.example.cycled.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cycled.data.FlowLevel
import com.example.cycled.domain.monthGrid
import com.example.cycled.ui.CycleViewModel
import com.example.cycled.ui.theme.CycledTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun CalendarRoute(viewModel: CycleViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val periodDays by viewModel.periodDays.collectAsStateWithLifecycle()
    val loggedDays = remember(periodDays) { periodDays.associate { it.date to it.flow } }
    val today = remember { viewModel.today() }
    val firstDayOfWeek = remember { WeekFields.of(Locale.getDefault()).firstDayOfWeek }
    // YearMonth is Serializable, so the default saver can put it in the Bundle.
    var month by rememberSaveable { mutableStateOf(YearMonth.from(today)) }

    CalendarScreen(
        month = month,
        loggedDays = loggedDays,
        today = today,
        firstDayOfWeek = firstDayOfWeek,
        onPreviousMonth = { month = month.minusMonths(1) },
        onNextMonth = { month = month.plusMonths(1) },
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    month: YearMonth,
    loggedDays: Map<LocalDate, FlowLevel>,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = Locale.getDefault()
    val weeks = remember(month, firstDayOfWeek) { monthGrid(month, firstDayOfWeek) }

    Scaffold(
        modifier = modifier,
        topBar = {
            // No icon library on the classpath, so the back button is a text button.
            TopAppBar(
                title = { Text("Calendar") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier.semantics { contentDescription = "Previous month" },
                ) { Text("‹", style = MaterialTheme.typography.titleLarge) }
                Text(
                    text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                TextButton(
                    onClick = onNextMonth,
                    modifier = Modifier.semantics { contentDescription = "Next month" },
                ) { Text("›", style = MaterialTheme.typography.titleLarge) }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                (0L until 7L).map { firstDayOfWeek.plus(it) }.forEach { day ->
                    Text(
                        text = day.getDisplayName(TextStyle.NARROW, locale),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clearAndSetSemantics {
                                contentDescription = day.getDisplayName(TextStyle.FULL, locale)
                            },
                    )
                }
            }

            Column {
                weeks.forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                            ) {
                                if (date != null) {
                                    DayCell(
                                        date = date,
                                        flow = loggedDays[date],
                                        isToday = date == today,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * One day in the grid. [flow] is non-null when the day is logged as a period day.
 * Later states (phases, predictions) should be added here.
 */
@Composable
private fun DayCell(
    date: LocalDate,
    flow: FlowLevel?,
    isToday: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val logged = flow != null
    val description = buildString {
        append(date.format(DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault())))
        if (isToday) append(", today")
        if (flow != null) append(", logged, ${flow.spokenLabel} flow")
    }

    // Outer box: today's ring. Inner box: the logged fill, inset so the ring
    // stays visible around it when today is also logged.
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(2.dp)
            .clearAndSetSemantics { contentDescription = description }
            .then(if (isToday) Modifier.border(2.dp, colors.primary, CircleShape) else Modifier)
            .padding(if (isToday && logged) 4.dp else 0.dp)
            .then(if (logged) Modifier.background(colors.primary, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (logged) colors.onPrimary else colors.onSurface,
        )
    }
}

private val FlowLevel.spokenLabel: String
    get() = when (this) {
        FlowLevel.SPOTTING -> "spotting"
        FlowLevel.LIGHT -> "light"
        FlowLevel.MEDIUM -> "medium"
        FlowLevel.HEAVY -> "heavy"
    }

private fun nov(day: Int) = LocalDate.of(2026, 11, day)

private val Nov1to5: Map<LocalDate, FlowLevel> = mapOf(
    nov(1) to FlowLevel.LIGHT,
    nov(2) to FlowLevel.HEAVY,
    nov(3) to FlowLevel.MEDIUM,
    nov(4) to FlowLevel.LIGHT,
    nov(5) to FlowLevel.SPOTTING,
)

@Composable
private fun PreviewCalendar(month: YearMonth, loggedDays: Map<LocalDate, FlowLevel>, today: LocalDate) {
    CycledTheme {
        CalendarScreen(
            month = month,
            loggedDays = loggedDays,
            today = today,
            firstDayOfWeek = DayOfWeek.SUNDAY,
            onPreviousMonth = {},
            onNextMonth = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarEmptyMonthPreview() {
    PreviewCalendar(YearMonth.of(2026, 11), emptyMap(), nov(14))
}

@Preview(showBackground = true)
@Composable
private fun CalendarLoggedDaysPreview() {
    PreviewCalendar(YearMonth.of(2026, 11), Nov1to5, nov(14))
}

@Preview(showBackground = true)
@Composable
private fun CalendarTodayIsLoggedPreview() {
    PreviewCalendar(YearMonth.of(2026, 11), Nov1to5, nov(3))
}

@Preview(showBackground = true)
@Composable
private fun CalendarSixRowMonthPreview() {
    val aug = (28..31).associate { LocalDate.of(2026, 8, it) to FlowLevel.MEDIUM }
    PreviewCalendar(YearMonth.of(2026, 8), aug, LocalDate.of(2026, 8, 14))
}
