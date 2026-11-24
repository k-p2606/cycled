package com.example.cycled.ui.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cycled.data.FlowLevel
import com.example.cycled.domain.CycleLog
import com.example.cycled.domain.CycleStats
import com.example.cycled.domain.LoggedDay
import com.example.cycled.domain.groupIntoPeriods
import com.example.cycled.ui.CycleViewModel
import com.example.cycled.ui.LogsUiState
import com.example.cycled.ui.SampleData
import com.example.cycled.ui.buildLogsState
import com.example.cycled.ui.components.WaveDivider
import com.example.cycled.ui.components.screenTopPadding
import com.example.cycled.ui.theme.CycledTheme
import com.example.cycled.ui.theme.cycledColors
import java.time.LocalDate
import java.util.Locale

/** Logs tab (D10). [onOpenDay] opens Calendar on a day (tapping a row). */
@Composable
fun LogsRoute(viewModel: CycleViewModel, onOpenDay: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.logsState.collectAsStateWithLifecycle()
    // The chosen tab, by its cycle's start (epoch day); null = the current cycle.
    var selectedStart by rememberSaveable { mutableStateOf<Long?>(null) }
    LogsScreen(
        state = state,
        selectedStart = selectedStart?.let { LocalDate.ofEpochDay(it) },
        onSelectCycle = { selectedStart = it.toEpochDay() },
        onOpenDay = onOpenDay,
        modifier = modifier,
    )
}

/**
 * Every logged day, one cycle at a time, newest first. View-only: tapping a day opens
 * it on Calendar. [selectedStart] picks the cycle tab; when null or no longer a cycle
 * start, the current cycle is shown.
 */
@Composable
fun LogsScreen(
    state: LogsUiState,
    selectedStart: LocalDate?,
    onSelectCycle: (LocalDate) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.cycledColors
    val locale = Locale.getDefault()
    val cycles = (state as? LogsUiState.Ready)?.cycles.orEmpty()
    val summary = (state as? LogsUiState.Ready)?.summary
    val selected = cycles.firstOrNull { it.start == selectedStart } ?: cycles.firstOrNull()

    Column(modifier.fillMaxSize().background(colors.sky)) {
        Text(
            "Logs",
            style = MaterialTheme.typography.headlineLarge,
            color = colors.text,
            modifier = Modifier
                .screenTopPadding()
                .padding(horizontal = 24.dp)
                .semantics { heading() },
        )
        if (summary != null) {
            Spacer(Modifier.height(10.dp))
            StatsSummary(summary)
        }
        if (cycles.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            CycleTabs(cycles, selected, locale, onSelectCycle)
        }
        Spacer(Modifier.height(16.dp))
        WaveDivider()

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(colors.water),
        ) {
            when {
                state == LogsUiState.Loading -> Unit
                selected == null -> EmptyText("Nothing logged yet.")
                else -> CycleDays(selected, locale, onOpenDay)
            }
        }
    }
}

/**
 * Average cycle, average period and variation, value over label. Wraps onto a second
 * line rather than clipping on narrow screens or large text. Read out as one sentence.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatsSummary(stats: CycleStats) {
    val colors = MaterialTheme.cycledColors
    val spoken = LogsFormat.summarySpoken(stats)
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clearAndSetSemantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LogsFormat.summaryItems(stats).forEach { (value, label) ->
            Column {
                Text(value, style = MaterialTheme.typography.titleLarge, color = colors.cream)
                Text(label, style = MaterialTheme.typography.bodySmall, color = colors.textDim)
            }
        }
    }
}

/** The sideways-scrolling cycle tabs, bleeding to the screen edges. */
@Composable
private fun CycleTabs(
    cycles: List<CycleLog>,
    selected: CycleLog?,
    locale: Locale,
    onSelect: (LocalDate) -> Unit,
) {
    val colors = MaterialTheme.cycledColors
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(cycles, key = { it.start.toEpochDay() }) { cycle ->
            val isSelected = cycle == selected
            val (label, sub) = LogsFormat.tab(cycle, locale)
            val spoken = LogsFormat.tabSpoken(cycle, locale)
            val shape = RoundedCornerShape(16.dp)
            Column(
                modifier = Modifier
                    .widthIn(min = 84.dp)
                    .heightIn(min = 56.dp)
                    .clip(shape)
                    .background(if (isSelected) colors.cream else Color.Transparent)
                    .border(1.dp, if (isSelected) colors.cream else colors.outline, shape)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(cycle.start) })
                    .clearAndSetSemantics { contentDescription = spoken }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val textColor = if (isSelected) colors.sky else colors.text
                Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = textColor)
                Text(
                    sub,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor,
                    modifier = Modifier.alpha(0.8f),
                )
            }
        }
    }
}

@Composable
private fun CycleDays(cycle: CycleLog, locale: Locale, onOpenDay: (LocalDate) -> Unit) {
    val colors = MaterialTheme.cycledColors
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 24.dp),
    ) {
        item(key = "heading") {
            Column(Modifier.padding(bottom = 12.dp)) {
                Text(
                    LogsFormat.heading(cycle, locale),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.text,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    LogsFormat.subline(cycle),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                    color = colors.textMuted,
                )
            }
        }
        if (cycle.days.isEmpty()) {
            item(key = "empty") { EmptyText("Nothing logged in this cycle yet.", padded = false) }
        }
        items(cycle.days, key = { it.date.toEpochDay() }) { day ->
            DayRow(day, locale, onClick = { onOpenDay(day.date) })
        }
    }
}

/** Date over weekday, flow and details, cycle day; one tap target that opens Calendar. */
@Composable
private fun DayRow(day: LoggedDay, locale: Locale, onClick: () -> Unit) {
    val colors = MaterialTheme.cycledColors
    val flow = LogsFormat.flowLine(day)
    val detail = LogsFormat.detailLine(day, locale)
    val spoken = LogsFormat.rowSpoken(day, locale)

    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Open in calendar", onClick = onClick)
            .semantics { contentDescription = spoken },
    ) {
        HorizontalDivider(thickness = 1.dp, color = colors.dividerWater)
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(vertical = 8.dp)
                .clearAndSetSemantics {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(LogsFormat.dateNumber(day.date), style = MaterialTheme.typography.titleLarge, color = colors.text)
                Text(LogsFormat.weekday(day.date, locale), style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                when {
                    flow != null -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            if (day.flow != null) Box(Modifier.size(7.dp).background(colors.period, CircleShape))
                            Text(flow, style = MaterialTheme.typography.labelLarge, color = colors.text)
                        }
                        if (detail != null) {
                            Text(detail, style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = colors.textMuted)
                        }
                    }
                    // No flow logged: symptoms and mood become the main line.
                    detail != null -> Text(detail, style = MaterialTheme.typography.labelLarge, color = colors.text)
                }
            }
            Text(
                "Day ${day.cycleDay}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun EmptyText(text: String, padded: Boolean = true) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.cycledColors.textMuted,
        modifier = if (padded) Modifier.padding(horizontal = 24.dp, vertical = 8.dp) else Modifier.padding(vertical = 8.dp),
    )
}

// ---- Previews: the handoff's sample data (D12) ----

private fun sampleState(
    today: LocalDate = SampleData.today,
    days: Map<LocalDate, FlowLevel> = SampleData.periodDays,
    withLogs: Boolean = true,
) = buildLogsState(
    periods = groupIntoPeriods(days.keys.toList()),
    periodDays = days,
    dailyLogs = if (withLogs) SampleData.dailyLogs else emptyMap(),
    today = today,
)

@Composable
private fun PreviewLogs(state: LogsUiState, selectedStart: LocalDate? = null) {
    CycledTheme { LogsScreen(state = state, selectedStart = selectedStart, onSelectCycle = {}, onOpenDay = {}) }
}

/** The design's screenshot: current cycle from 31 Oct, day 9. */
@Preview(widthDp = 390, heightDp = 768)
@Composable
private fun LogsCurrentCyclePreview() = PreviewLogs(sampleState())

/** An older cycle: 3 Oct – 30 Oct, including days with only symptoms or mood. */
@Preview(widthDp = 390, heightDp = 768)
@Composable
private fun LogsOlderCyclePreview() = PreviewLogs(sampleState(), selectedStart = LocalDate.of(2026, 10, 3))

/**
 * The per-cycle empty state. cycleLogs() never builds an empty cycle from real data
 * (a cycle's first day always has flow), so this one is made by hand.
 */
@Preview(widthDp = 390, heightDp = 768)
@Composable
private fun LogsEmptyCyclePreview() {
    val start = LocalDate.of(2026, 11, 8)
    val empty = CycleLog(start = start, end = null, lengthDays = 1, periodDays = 0, days = emptyList())
    PreviewLogs(LogsUiState.Ready(cycles = listOf(empty), today = start))
}

/** One completed cycle (3 Oct – 30 Oct) and the current one: no variation yet, so two items. */
@Preview(widthDp = 390, heightDp = 768)
@Composable
private fun LogsOneCompletedCyclePreview() =
    PreviewLogs(sampleState(days = SampleData.periodDays.filterKeys { it >= LocalDate.of(2026, 10, 1) }))

@Preview(widthDp = 390, heightDp = 768)
@Composable
private fun LogsNoDataPreview() = PreviewLogs(sampleState(days = emptyMap(), withLogs = false))

@Preview(widthDp = 360, heightDp = 640, fontScale = 1.3f)
@Composable
private fun LogsNarrowLargeFontPreview() = PreviewLogs(sampleState())
