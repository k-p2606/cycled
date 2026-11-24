package com.example.cycled.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cycled.data.FlowChoice
import com.example.cycled.data.FlowLevel
import com.example.cycled.data.Mood
import com.example.cycled.data.Symptom
import com.example.cycled.domain.AverageCyclePredictor
import com.example.cycled.domain.groupIntoPeriods
import com.example.cycled.domain.monthGrid
import com.example.cycled.ui.CalendarUiState
import com.example.cycled.ui.CycleMessages
import com.example.cycled.ui.CycleViewModel
import com.example.cycled.ui.DayMark
import com.example.cycled.ui.DayPanel
import com.example.cycled.ui.DayStyle
import com.example.cycled.ui.MoodChips
import com.example.cycled.ui.SampleData
import com.example.cycled.ui.SymptomChips
import com.example.cycled.ui.buildCalendarState
import com.example.cycled.ui.components.CycledIcons
import com.example.cycled.ui.components.MultiChoiceChipGroup
import com.example.cycled.ui.components.PrimaryButton
import com.example.cycled.ui.components.SingleChoiceChipGroup
import com.example.cycled.ui.components.TopThenFill
import com.example.cycled.ui.components.WaveDivider
import com.example.cycled.ui.components.screenTopPadding
import com.example.cycled.ui.displayName
import com.example.cycled.ui.label
import com.example.cycled.ui.theme.CycledTheme
import com.example.cycled.ui.theme.cycledColors
import com.example.cycled.ui.theme.phaseColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Calendar, which is also where days are logged (D6). [openDate], when set, is the day
 * another screen asked to open (Home's "Log today", later Logs); it's applied once per
 * visit, so rotating doesn't jump back to it.
 */
@Composable
fun CalendarRoute(viewModel: CycleViewModel, openDate: LocalDate?, modifier: Modifier = Modifier) {
    val state by viewModel.calendarState.collectAsStateWithLifecycle()
    val firstDayOfWeek = remember { WeekFields.of(Locale.getDefault()).firstDayOfWeek }
    // YearMonth is Serializable, so the default saver can put it in the Bundle.
    var month by rememberSaveable { mutableStateOf(YearMonth.from(openDate ?: state.selected.date)) }
    var openDateHandled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(openDate) {
        if (openDate != null && !openDateHandled) {
            viewModel.selectDate(openDate)
            month = YearMonth.from(openDate)
            openDateHandled = true
        }
    }

    CalendarScreen(
        month = month,
        state = state,
        firstDayOfWeek = firstDayOfWeek,
        onPreviousMonth = { month = month.minusMonths(1) },
        onNextMonth = { month = month.plusMonths(1) },
        onSelectDay = viewModel::selectDate,
        onSetFlow = viewModel::setFlow,
        onToggleSymptom = viewModel::toggleSymptom,
        onSetMood = viewModel::setMood,
        modifier = modifier,
    )
}

/**
 * The month grid on the sky, the selected day's log in the water below (scrolling with
 * the page on short screens), and the period button pinned above the tab bar.
 */
@Composable
fun CalendarScreen(
    month: YearMonth,
    state: CalendarUiState,
    firstDayOfWeek: DayOfWeek,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (LocalDate) -> Unit,
    onSetFlow: (LocalDate, FlowChoice?) -> Unit,
    onToggleSymptom: (LocalDate, Symptom) -> Unit,
    onSetMood: (LocalDate, Mood?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.cycledColors
    val locale = Locale.getDefault()
    val panel = state.selected

    Column(modifier.fillMaxSize().background(colors.sky)) {
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(colors.water),
        ) {
            TopThenFill(
                viewportHeight = constraints.maxHeight,
                modifier = Modifier.verticalScroll(rememberScrollState()),
                top = {
                    Column(Modifier.background(colors.sky)) {
                        MonthPart(month, state, firstDayOfWeek, locale, onPreviousMonth, onNextMonth, onSelectDay)
                        WaveDivider()
                    }
                },
                bottom = {
                    LogPanel(
                        panel = panel,
                        locale = locale,
                        onSetFlow = { onSetFlow(panel.date, it) },
                        onToggleSymptom = { onToggleSymptom(panel.date, it) },
                        onSetMood = { onSetMood(panel.date, it) },
                    )
                },
            )
        }
        PeriodButtonBar(panel, locale, onSetFlow = { onSetFlow(panel.date, it) })
    }
}

// ---- Sky: header, weekdays, grid, legend ----

@Composable
private fun MonthPart(
    month: YearMonth,
    state: CalendarUiState,
    firstDayOfWeek: DayOfWeek,
    locale: Locale,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (LocalDate) -> Unit,
) {
    val colors = MaterialTheme.cycledColors
    val weeks = remember(month, firstDayOfWeek) { monthGrid(month, firstDayOfWeek) }

    Column(
        Modifier
            .fillMaxWidth()
            .screenTopPadding(top = 52.dp)
            .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.text,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            IconButton(onClick = onPreviousMonth, modifier = Modifier.size(44.dp)) {
                Icon(CycledIcons.ChevronLeft, contentDescription = "Previous month", tint = colors.text)
            }
            IconButton(onClick = onNextMonth, modifier = Modifier.size(44.dp)) {
                Icon(CycledIcons.ChevronRight, contentDescription = "Next month", tint = colors.text)
            }
        }
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth()) {
            (0L until 7L).map { firstDayOfWeek.plus(it) }.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textDim,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(22.dp)
                        .clearAndSetSemantics { contentDescription = day.getDisplayName(TextStyle.FULL, locale) },
                )
            }
        }

        weeks.forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        // Padding cells (null) stay empty.
                        if (date != null) {
                            DayCell(
                                mark = state.markOn(date),
                                selected = date == state.selected.date,
                                locale = locale,
                                onClick = { onSelectDay(date) },
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Legend(state)
    }
}

/**
 * One day: a 32 dp circle with the number, styled by [DayMark.style], over a 5 dp slot
 * for the logged dot. Today gets a cream ring (any fill is inset so the ring shows);
 * the selected day sits on the selected-cell background.
 */
@Composable
private fun DayCell(mark: DayMark, selected: Boolean, locale: Locale, onClick: () -> Unit) {
    val colors = MaterialTheme.cycledColors
    val phaseColors = MaterialTheme.phaseColors
    val description = remember(mark, locale) { mark.spokenDescription(locale) }

    val style = mark.style
    val fill: Color? = when (style) {
        DayStyle.LOGGED_FLOW -> phaseColors.loggedFill
        DayStyle.OVULATORY, DayStyle.FOLLICULAR -> phaseColors.fill(mark.phase!!)
        else -> null
    }
    val number = when (style) {
        DayStyle.LOGGED_FLOW -> phaseColors.onLoggedFill
        DayStyle.PREDICTED_PERIOD -> phaseColors.predictedNumber
        DayStyle.OVULATORY, DayStyle.FOLLICULAR, DayStyle.LUTEAL -> phaseColors.number(mark.phase!!)
        DayStyle.PLAIN -> phaseColors.defaultNumber
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(SelectedCellShape)
            .background(if (selected) colors.selectedCell else Color.Transparent)
            // The fill is only 1.92:1 against sky, so the border carries the selection.
            .then(if (selected) Modifier.border(1.5.dp, phaseColors.selectedBorder, SelectedCellShape) else Modifier)
            .selectable(selected = selected, onClick = onClick)
            .semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            if (fill != null) {
                Box(
                    Modifier
                        .padding(if (mark.isToday) 3.dp else 0.dp)
                        .fillMaxSize()
                        .background(fill, CircleShape),
                )
            }
            when {
                mark.isToday -> Box(Modifier.fillMaxSize().border(2.dp, phaseColors.todayRing, CircleShape))
                style == DayStyle.PREDICTED_PERIOD -> Box(Modifier.fillMaxSize().dashedCircle(phaseColors.predictedRing))
                style == DayStyle.LUTEAL -> Box(
                    Modifier.fillMaxSize().border(1.5.dp, phaseColors.ring(mark.phase!!)!!, CircleShape),
                )
            }
            Text(
                text = mark.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (mark.isToday) FontWeight.Bold else FontWeight.Medium,
                color = number,
            )
        }
        Box(
            Modifier
                .padding(top = 1.dp)
                .height(5.dp)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            if (mark.hasLoggedDot) Box(Modifier.size(4.dp).background(phaseColors.loggedDot, CircleShape))
        }
    }
}

private val SelectedCellShape = RoundedCornerShape(12.dp)

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

/** E.g. "8 November, today, follicular phase, estimated fertile window, symptoms logged". */
private fun DayMark.spokenDescription(locale: Locale): String = buildString {
    append(date.format(DateTimeFormatter.ofPattern("d MMMM", locale)))
    if (isToday) append(", today")
    loggedFlow?.let { append(", ${it.label.lowercase(locale)} flow logged") }
    if (noFlow) append(", no flow logged")
    if (isPredictedPeriod && loggedFlow == null) append(", predicted period")
    phase?.let { append(", ${it.displayName.lowercase(locale)} phase") }
    if (isFertile) append(", estimated fertile window")
    when {
        hasSymptoms && hasMood -> append(", symptoms and mood logged")
        hasSymptoms -> append(", symptoms logged")
        hasMood -> append(", mood logged")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(state: CalendarUiState) {
    val colors = MaterialTheme.cycledColors
    val phaseColors = MaterialTheme.phaseColors
    val swatch = Modifier.size(11.dp)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            LegendItem("Period") { Box(swatch.background(phaseColors.loggedFill, CircleShape)) }
            LegendItem("Predicted") { Box(swatch.dashedCircle(phaseColors.predictedRing, 1.dp)) }
            LegendItem("Follicular") { Box(swatch.background(colors.follicular, CircleShape)) }
            LegendItem("High tide") { Box(swatch.background(colors.cream, CircleShape)) }
            LegendItem("Luteal") { Box(swatch.border(1.5.dp, colors.luteal, CircleShape)) }
            LegendItem("Logged") {
                Box(swatch, contentAlignment = Alignment.Center) {
                    Box(Modifier.size(4.dp).background(phaseColors.loggedDot, CircleShape))
                }
            }
        }
        val forecast = state.forecast
        if (forecast != null && forecast.isLate) {
            Text("${CycleMessages.PERIOD_MAY_BE_LATE}.", style = MaterialTheme.typography.bodySmall, color = colors.text)
        }
        if (forecast != null && state.isPlaceholderForecast) {
            Text(
                CycleMessages.placeholderNote(forecast.cycleLengthDays),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        }
    }
}

@Composable
private fun LegendItem(label: String, swatch: @Composable () -> Unit) {
    Row(
        modifier = Modifier.height(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        swatch()
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.cycledColors.textMuted)
    }
}

// ---- Water: the selected day's log ----

@Composable
private fun LogPanel(
    panel: DayPanel,
    locale: Locale,
    onSetFlow: (FlowChoice?) -> Unit,
    onToggleSymptom: (Symptom) -> Unit,
    onSetMood: (Mood?) -> Unit,
) {
    val colors = MaterialTheme.cycledColors
    val enabled = !panel.isFuture
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.water)
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                panel.date.format(DateTimeFormatter.ofPattern("EEE d MMM", locale)),
                style = MaterialTheme.typography.titleMedium,
                color = colors.text,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            if (panel.cycleDay != null && panel.tideName != null) {
                Text(
                    "Day ${panel.cycleDay} · ${panel.tideName}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                    color = colors.textMuted,
                )
            }
        }
        if (panel.isFuture) {
            Text(
                "You can log this day once it arrives.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        }
        ChipGroup("Flow") {
            SingleChoiceChipGroup(
                options = FlowChoice.entries,
                selected = panel.flowChoice,
                onSelect = onSetFlow,
                optionLabel = { it.label },
                enabled = enabled,
            )
        }
        ChipGroup("Symptoms") {
            MultiChoiceChipGroup(
                options = SymptomChips,
                selected = panel.symptoms,
                onToggle = onToggleSymptom,
                optionLabel = { it.label },
                enabled = enabled,
            )
        }
        ChipGroup("Mood") {
            SingleChoiceChipGroup(
                options = MoodChips,
                selected = panel.mood,
                onSelect = onSetMood,
                optionLabel = { it.label },
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun ChipGroup(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.cycledColors.textMuted,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

/**
 * The pinned period button: coral "Log period today" when the selected day has no flow
 * (sets Medium), outlined "Period logged today" with a check when it has (clears it).
 * Disabled for future days.
 */
@Composable
private fun PeriodButtonBar(panel: DayPanel, locale: Locale, onSetFlow: (FlowChoice?) -> Unit) {
    val colors = MaterialTheme.cycledColors
    val day = if (panel.isToday) "today" else "on ${panel.date.format(DateTimeFormatter.ofPattern("d MMM", locale))}"
    val logged = panel.loggedFlow != null

    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.water),
    ) {
        HorizontalDivider(thickness = 1.dp, color = colors.dividerWater)
        Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 12.dp)) {
            if (logged) {
                PrimaryButton(
                    text = "Period logged $day",
                    onClick = { onSetFlow(null) },
                    enabled = !panel.isFuture,
                    containerColor = Color.Transparent,
                    contentColor = colors.periodLight,
                    borderColor = colors.period,
                    icon = CycledIcons.Check,
                )
            } else {
                PrimaryButton(
                    text = "Log period $day",
                    onClick = { onSetFlow(FlowChoice.MEDIUM) },
                    enabled = !panel.isFuture,
                    containerColor = colors.period,
                    contentColor = colors.sky,
                    borderColor = colors.period,
                )
            }
        }
    }
}

// ---- Previews: the handoff's sample data (D12) ----

private fun sampleState(
    today: LocalDate = SampleData.today,
    selected: LocalDate = today,
    days: Map<LocalDate, FlowLevel> = SampleData.periodDays,
) = buildCalendarState(
    loggedDays = days,
    periods = groupIntoPeriods(days.keys.toList()),
    today = today,
    predictor = AverageCyclePredictor(),
    dailyLogs = if (days.isEmpty()) emptyMap() else SampleData.dailyLogs.filterKeys { !it.isAfter(today) },
    selected = selected,
)

@Composable
private fun PreviewCalendar(state: CalendarUiState, month: YearMonth = YearMonth.from(state.selected.date)) {
    CycledTheme {
        CalendarScreen(
            month = month,
            state = state,
            firstDayOfWeek = DayOfWeek.SUNDAY,
            onPreviousMonth = {},
            onNextMonth = {},
            onSelectDay = {},
            onSetFlow = { _, _ -> },
            onToggleSymptom = { _, _ -> },
            onSetMood = { _, _ -> },
        )
    }
}

/** Sun 8 Nov, the design's screenshot. */
@Preview(widthDp = 390, heightDp = 768)
@Composable
private fun CalendarTodayPreview() = PreviewCalendar(sampleState())

/** 1 Nov selected: heavy flow, cramps and tired logged; the period button is "logged". */
@Preview(widthDp = 390, heightDp = 768)
@Composable
private fun CalendarLoggedDayPreview() = PreviewCalendar(sampleState(selected = LocalDate.of(2026, 11, 1)))

/** A future day: viewable, chips and button disabled. */
@Preview(widthDp = 390, heightDp = 768)
@Composable
private fun CalendarFutureDayPreview() = PreviewCalendar(sampleState(selected = LocalDate.of(2026, 11, 20)))

/** Wed 2 Dec with no new period: late. */
@Preview(widthDp = 390, heightDp = 768)
@Composable
private fun CalendarLatePreview() = PreviewCalendar(sampleState(today = LocalDate.of(2026, 12, 2)))

@Preview(widthDp = 390, heightDp = 768)
@Composable
private fun CalendarNoDataPreview() = PreviewCalendar(sampleState(days = emptyMap()))

/** Narrow phone at 1.3× font: the page scrolls, the period button stays put. */
@Preview(widthDp = 360, heightDp = 640, fontScale = 1.3f)
@Composable
private fun CalendarNarrowLargeFontPreview() = PreviewCalendar(sampleState())
