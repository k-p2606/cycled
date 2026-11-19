package com.example.cycled.ui.calendar

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cycled.data.FlowLevel
import com.example.cycled.domain.AverageCyclePredictor
import com.example.cycled.domain.Phase
import com.example.cycled.domain.groupIntoPeriods
import com.example.cycled.domain.monthGrid
import com.example.cycled.ui.CalendarUiState
import com.example.cycled.ui.CycleMessages
import com.example.cycled.ui.CycleViewModel
import com.example.cycled.ui.DayMark
import com.example.cycled.ui.buildCalendarState
import com.example.cycled.ui.displayName
import com.example.cycled.ui.theme.CycledTheme
import com.example.cycled.ui.theme.phaseColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun CalendarRoute(viewModel: CycleViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.calendarState.collectAsStateWithLifecycle()
    val firstDayOfWeek = remember { WeekFields.of(Locale.getDefault()).firstDayOfWeek }
    // YearMonth is Serializable, so the default saver can put it in the Bundle.
    var month by rememberSaveable { mutableStateOf(YearMonth.from(viewModel.today())) }

    CalendarScreen(
        month = month,
        state = state,
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
    state: CalendarUiState,
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
                                // Padding cells (null) stay empty, with no tint.
                                if (date != null) DayCell(mark = state.markOn(date))
                            }
                        }
                    }
                }
            }

            Legend(state = state, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/**
 * One day in the grid, drawn from [mark] in layers (later on top): phase tint,
 * predicted-period outline, logged fill, day number, fertile dot, today's ring.
 */
@Composable
private fun DayCell(mark: DayMark, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val phaseColors = MaterialTheme.phaseColors
    val logged = mark.loggedFlow != null
    val description = remember(mark) { mark.spokenDescription(Locale.getDefault()) }
    // Inset the circle when today so the ring stays visible around it.
    val circleInset = if (mark.isToday) 4.dp else 0.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        // 1. Phase tint, with a 1dp gap to its neighbours so phases read as blocks.
        if (mark.phase != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(1.dp)
                    .background(phaseColors.tint(mark.phase), RoundedCornerShape(6.dp)),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
                .padding(circleInset),
        ) {
            // 2. Predicted period day: a dashed outline, so it reads as a guess.
            if (mark.isPredictedPeriod && !logged) {
                Box(Modifier.fillMaxSize().dashedCircle(phaseColors.predictedOutline))
            }
            // 3. Logged day: a solid circle covering any tint or outline under it.
            if (logged) {
                Box(Modifier.fillMaxSize().background(phaseColors.loggedFill, CircleShape))
            }
        }

        Text(
            text = mark.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (logged) phaseColors.onLoggedFill else colors.onSurface,
        )

        // 4. Fertile window: a small dot under the number, on any day it applies to.
        if (mark.isFertile) {
            FertileDot(
                color = if (logged) phaseColors.onLoggedFill else phaseColors.fertileDot,
                modifier = Modifier.offset(y = 12.dp),
            )
        }

        // 5. Today: the ring, always on top.
        if (mark.isToday) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(3.dp)
                    .border(2.dp, colors.primary, CircleShape),
            )
        }
    }
}

@Composable
private fun FertileDot(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(5.dp).background(color, CircleShape))
}

private fun Modifier.dashedCircle(color: Color, width: Dp = 1.5.dp): Modifier = drawBehind {
    val stroke = width.toPx()
    drawCircle(
        color = color,
        radius = size.minDimension / 2 - stroke / 2,
        style = Stroke(
            width = stroke,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.5.dp.toPx())),
        ),
    )
}

/** E.g. "November 16, today, estimated ovulatory phase, estimated fertile window". */
private fun DayMark.spokenDescription(locale: Locale): String = buildString {
    append(date.format(DateTimeFormatter.ofPattern("MMMM d", locale)))
    if (isToday) append(", today")
    loggedFlow?.let { append(", logged, ${it.spokenLabel} flow") }
    if (isPredictedPeriod && loggedFlow == null) append(", predicted period")
    phase?.let { append(", estimated ${it.displayName.lowercase(locale)} phase") }
    if (isFertile) append(", estimated fertile window")
}

private val FlowLevel.spokenLabel: String
    get() = when (this) {
        FlowLevel.SPOTTING -> "spotting"
        FlowLevel.LIGHT -> "light"
        FlowLevel.MEDIUM -> "medium"
        FlowLevel.HEAVY -> "heavy"
    }

@Composable
private fun Legend(state: CalendarUiState, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val phaseColors = MaterialTheme.phaseColors
    val forecast = state.forecast
    val swatchSize = 20.dp

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider()

        if (forecast != null) {
            Phase.entries.forEach { phase ->
                LegendRow(label = "${phase.displayName} phase (estimated)") {
                    Box(
                        Modifier
                            .size(swatchSize)
                            .background(phaseColors.tint(phase), RoundedCornerShape(4.dp)),
                    )
                }
            }
        }

        LegendRow(label = "Logged period") {
            Box(Modifier.size(swatchSize).background(phaseColors.loggedFill, CircleShape))
        }

        if (forecast != null) {
            LegendRow(label = "Predicted period") {
                Box(Modifier.size(swatchSize).dashedCircle(phaseColors.predictedOutline))
            }
            LegendRow(label = "Fertile window (estimated)") {
                Box(Modifier.size(swatchSize), contentAlignment = Alignment.Center) {
                    FertileDot(phaseColors.fertileDot)
                }
            }
            LegendNote("The fertile window can overlap other phases.")
            if (state.isPlaceholderForecast) {
                LegendNote(CycleMessages.placeholderNote(forecast.cycleLengthDays))
            }
            if (forecast.isLate) {
                LegendNote("${CycleMessages.PERIOD_MAY_BE_LATE}.", color = colors.onSurface)
            }
        }

        LegendNote(CycleMessages.ESTIMATES_DISCLAIMER)
    }
}

@Composable
private fun LegendRow(label: String, swatch: @Composable () -> Unit) {
    Row(
        // The swatch is decorative; the label says what it means.
        modifier = Modifier.semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        swatch()
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LegendNote(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = color)
}

// ---- Previews: fixed dates, state built with buildCalendarState(). ----

private fun date(month: Int, day: Int) = LocalDate.of(2026, month, day)

private fun loggedRange(from: LocalDate, to: LocalDate, flow: FlowLevel = FlowLevel.MEDIUM) =
    generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.associateWith { flow }

private fun previewState(logged: Map<LocalDate, FlowLevel>, today: LocalDate): CalendarUiState =
    buildCalendarState(logged, groupIntoPeriods(logged.keys.toList()), today, AverageCyclePredictor())

/** Oct 4-8 and Nov 1-5: 28-day cycle, predicted Nov 29 - Dec 3, fertile Nov 10-16. */
private val TwoPeriods = loggedRange(date(10, 4), date(10, 8)) + mapOf(
    date(11, 1) to FlowLevel.LIGHT,
    date(11, 2) to FlowLevel.HEAVY,
    date(11, 3) to FlowLevel.MEDIUM,
    date(11, 4) to FlowLevel.LIGHT,
    date(11, 5) to FlowLevel.SPOTTING,
)

@Composable
private fun PreviewCalendar(month: YearMonth, state: CalendarUiState, darkTheme: Boolean = false) {
    // Dynamic colour off so previews show the app's own colour schemes.
    CycledTheme(darkTheme = darkTheme, dynamicColor = false) {
        CalendarScreen(
            month = month,
            state = state,
            firstDayOfWeek = DayOfWeek.SUNDAY,
            onPreviousMonth = {},
            onNextMonth = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun CalendarCurrentCyclePreview() {
    PreviewCalendar(YearMonth.of(2026, 11), previewState(TwoPeriods, date(11, 13)))
}

/** Oct 10-14 and Nov 8-12: predicted Dec 6-10, all inside December. */
@Preview(showBackground = true, heightDp = 900)
@Composable
private fun CalendarPredictedMonthPreview() {
    val logged = loggedRange(date(10, 10), date(10, 14)) + loggedRange(date(11, 8), date(11, 12))
    PreviewCalendar(YearMonth.of(2026, 12), previewState(logged, date(11, 20)))
}

/** Predicted from Nov 29, today Dec 2: Nov 29 - Dec 1 are no longer marked. */
@Preview(showBackground = true, heightDp = 900)
@Composable
private fun CalendarLatePreview() {
    PreviewCalendar(YearMonth.of(2026, 12), previewState(TwoPeriods, date(12, 2)))
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun CalendarPlaceholderPreview() {
    PreviewCalendar(YearMonth.of(2026, 11), previewState(loggedRange(date(11, 1), date(11, 5)), date(11, 13)))
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun CalendarNoDataPreview() {
    PreviewCalendar(YearMonth.of(2026, 11), previewState(emptyMap(), date(11, 13)))
}

@Preview(showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CalendarCurrentCycleDarkPreview() {
    PreviewCalendar(YearMonth.of(2026, 11), previewState(TwoPeriods, date(11, 13)), darkTheme = true)
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun CalendarTodayIsLoggedPreview() {
    PreviewCalendar(YearMonth.of(2026, 11), previewState(TwoPeriods, date(11, 3)))
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun CalendarSixRowMonthPreview() {
    val aug = loggedRange(date(8, 28), date(8, 31))
    PreviewCalendar(YearMonth.of(2026, 8), previewState(aug, date(8, 14)))
}
