package com.example.cycled.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cycled.data.DailyLog
import com.example.cycled.data.Mood
import com.example.cycled.data.Symptom
import com.example.cycled.domain.AverageCyclePredictor
import com.example.cycled.domain.Recommendation
import com.example.cycled.domain.groupIntoPeriods
import com.example.cycled.domain.TideModel
import com.example.cycled.ui.ComingUpKind
import com.example.cycled.ui.ComingUpRow
import com.example.cycled.ui.CycleViewModel
import com.example.cycled.ui.HomeUiState
import com.example.cycled.ui.SampleData
import com.example.cycled.ui.buildHomeState
import com.example.cycled.ui.components.ListRow
import com.example.cycled.ui.components.PrimaryButton
import com.example.cycled.ui.components.TopThenFill
import com.example.cycled.ui.components.screenTopPadding
import com.example.cycled.ui.theme.CycledTheme
import com.example.cycled.ui.theme.cycledColors
import java.time.LocalDate
import java.util.Locale

@Composable
fun HomeRoute(viewModel: CycleViewModel, onLogTodayClick: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.homeState.collectAsStateWithLifecycle()
    var showAllTips by rememberSaveable { mutableStateOf(false) }
    HomeScreen(
        state = state,
        today = viewModel.today(),
        onLogTodayClick = onLogTodayClick,
        showAllTips = showAllTips,
        onSeeAllTips = { showAllTips = true },
        onDismissTips = { showAllTips = false },
        modifier = modifier,
    )
}

/**
 * Home (handoff "Home"): today's cycle day and moon on the sky, the cycle drawn as a
 * tide, then a water panel with the hormone key, three coming-up dates, the likely
 * range and disclaimer, and "Log today". Scrolls when the screen is too short; when it
 * isn't, the water panel stretches to the bottom and spreads its items out.
 *
 * [today] is used for the date line when there's no forecast; with one, it's the
 * state's own date. [showAllTips] opens the "See all" sheet with one tip per category.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    today: LocalDate,
    onLogTodayClick: () -> Unit,
    modifier: Modifier = Modifier,
    showAllTips: Boolean = false,
    onSeeAllTips: () -> Unit = {},
    onDismissTips: () -> Unit = {},
) {
    val colors = MaterialTheme.cycledColors
    if (state == HomeUiState.Loading) {
        Box(modifier.fillMaxSize().background(colors.sky))
        return
    }
    val ready = state as? HomeUiState.Ready
    val locale = Locale.getDefault()

    BoxWithConstraints(modifier.fillMaxSize().background(colors.water)) {
        val viewportHeight = constraints.maxHeight
        TopThenFill(
            viewportHeight = viewportHeight,
            modifier = Modifier.verticalScroll(rememberScrollState()),
            top = { SkyPart(ready, ready?.today ?: today, locale) },
            bottom = { WaterPanel(ready, locale, onLogTodayClick, onSeeAllTips) },
        )
    }
    if (showAllTips && ready != null && ready.allRecommendations.isNotEmpty()) {
        AllTipsSheet(listOfNotNull(ready.lateTip) + ready.allRecommendations, onDismissTips)
    }
}

@Composable
private fun SkyPart(state: HomeUiState.Ready?, today: LocalDate, locale: Locale) {
    val colors = MaterialTheme.cycledColors
    val tide = remember(state?.cycleLengthDays, state?.ovulationCycleDay) {
        if (state != null) {
            TideModel(state.cycleLengthDays, state.ovulationCycleDay)
        } else {
            TideModel(HomeUiState.NO_DATA_CYCLE_LENGTH, HomeUiState.NO_DATA_OVULATION_DAY)
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.sky)
            .screenTopPadding(),
    ) {
        Column(Modifier.padding(horizontal = 24.dp)) {
            Text(
                HomeFormat.dateLine(today, locale),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textDim,
            )
            Hero(state, locale)
        }
        Spacer(Modifier.height(8.dp))
        TideDrawing(
            tide = tide,
            markerDay = state?.markerCycleDay,
            phaseDays = state?.phaseDays.orEmpty(),
        )
    }
}

/** Cycle day + moon, tide name and subline, read out as one line. */
@Composable
private fun Hero(state: HomeUiState.Ready?, locale: Locale) {
    val colors = MaterialTheme.cycledColors
    val dayText = state?.status?.cycleDay?.toString() ?: "–"
    val title = state?.tideName ?: "Log your first period"
    val spoken = if (state != null) {
        HomeFormat.heroSpoken(state.status.cycleDay, state.tideName, state.phaseSubline, locale)
    } else {
        "No cycle day yet. Log your first period"
    }

    Column(Modifier.clearAndSetSemantics { contentDescription = spoken }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Cycle day", style = MaterialTheme.typography.bodyMedium, color = colors.textDim)
                Text(dayText, style = MaterialTheme.typography.displayLarge, color = colors.cream)
            }
            // The moon takes what's left, up to 164 dp, so a big day number at large font still fits.
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                Moon(
                    phase = state?.moonPhase ?: 0.0,
                    modifier = Modifier
                        .widthIn(max = 164.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.headlineLarge, color = colors.text)
        if (state != null) {
            Text(state.phaseSubline, style = MaterialTheme.typography.bodyLarge, color = colors.textMuted)
        }
    }
}

@Composable
private fun WaterPanel(
    state: HomeUiState.Ready?,
    locale: Locale,
    onLogTodayClick: () -> Unit,
    onSeeAllTips: () -> Unit,
) {
    val colors = MaterialTheme.cycledColors
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.water)
            .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 18.dp),
        verticalArrangement = EvenlyWithMinGap,
    ) {
        HormoneKey()
        if (state != null) {
            Column {
                state.comingUp.forEach { ComingUpListRow(it, locale) }
            }
            if (state.recommendations.isNotEmpty()) {
                ForToday(state.recommendations, onSeeAllTips)
            }
            Text(
                HomeFormat.rangeLine(state.likelyRange, locale),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        }
        PrimaryButton(text = "Log today", onClick = onLogTodayClick)
    }
}

/** Spreads items evenly (gaps above, between and below) but never closer than 14 dp. */
private val EvenlyWithMinGap = object : Arrangement.Vertical {
    override val spacing = 14.dp

    override fun Density.arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
        if (sizes.isEmpty()) return
        val minGap = spacing.roundToPx()
        val free = totalSize - sizes.sum()
        val gap = maxOf(minGap, free / (sizes.size + 1))
        var y = ((free - gap * (sizes.size - 1)) / 2).coerceAtLeast(0)
        sizes.forEachIndexed { i, size ->
            outPositions[i] = y
            y += size + gap
        }
    }
}

/** Swatches for the three lines, with "Typical pattern" pushed right (D8). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HormoneKey() {
    val colors = MaterialTheme.cycledColors
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = "Lines show a typical hormone pattern, not your levels: " +
                    "estrogen, progesterone and LH"
            },
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        KeyItem("Estrogen") { LineSwatch(colors.estrogen) }
        KeyItem("Progesterone") { LineSwatch(colors.progesterone) }
        KeyItem("LH") { LineSwatch(colors.text, dashed = true) }
        Text(
            "Typical pattern",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun KeyItem(label: String, swatch: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        swatch()
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.cycledColors.textMuted)
    }
}

@Composable
private fun LineSwatch(color: Color, dashed: Boolean = false) {
    Canvas(Modifier.size(width = 16.dp, height = 2.dp)) {
        drawLine(
            color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = size.height,
            pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())) else null,
        )
    }
}

@Composable
private fun ComingUpListRow(row: ComingUpRow, locale: Locale) {
    val colors = MaterialTheme.cycledColors
    val dot = when (row.kind) {
        ComingUpKind.NEXT_PERIOD -> colors.period
        ComingUpKind.FERTILE_WINDOW -> colors.fertile
        ComingUpKind.HIGH_TIDE -> colors.cream
    }
    val spoken = HomeFormat.rowSpoken(row, locale)
    ListRow(
        label = HomeFormat.rowLabel(row.kind),
        dividerColor = colors.dividerWater,
        minHeight = 50.dp,
        modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
        leading = { Box(Modifier.size(9.dp).background(dot, CircleShape)) },
    ) {
        Text(HomeFormat.rowDate(row, locale), style = MaterialTheme.typography.bodyMedium, color = colors.text)
        Text(
            row.until,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (row.isPassed) FontWeight.Normal else FontWeight.SemiBold,
            color = if (row.isPassed) colors.textMuted else colors.cream,
        )
    }
}

/** "For today": heading with "See all", the top tips, and the wellbeing line. */
@Composable
private fun ForToday(tips: List<Recommendation>, onSeeAll: () -> Unit) {
    val colors = MaterialTheme.cycledColors
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                HomeFormat.TIPS_TITLE,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            TextButton(
                onClick = onSeeAll,
                modifier = Modifier.heightIn(min = 44.dp),
                contentPadding = PaddingValues(start = 12.dp, end = 0.dp),
            ) {
                Text("See all", style = MaterialTheme.typography.labelMedium, color = colors.cream)
            }
        }
        TipRows(tips)
    }
}

/** Tip rows, then the wellbeing line. Shared by Home and the sheet. */
@Composable
private fun TipRows(tips: List<Recommendation>) {
    val colors = MaterialTheme.cycledColors
    Column {
        tips.forEach { TipRow(it) }
        HorizontalDivider(thickness = 1.dp, color = colors.dividerWater)
        Text(
            HomeFormat.TIPS_DISCLAIMER,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

/** Category over title and body; read out as "Body tip: <title>. <body>". */
@Composable
private fun TipRow(tip: Recommendation) {
    val colors = MaterialTheme.cycledColors
    val spoken = HomeFormat.tipSpoken(tip)
    ListRow(
        dividerColor = colors.dividerWater,
        minHeight = 50.dp,
        verticalPadding = 10.dp,
        modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(HomeFormat.categoryLabel(tip.category), style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            Text(tip.title, style = MaterialTheme.typography.labelLarge, color = colors.text)
            Text(
                tip.body,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, lineHeight = 19.sp),
                color = colors.textMuted,
            )
        }
    }
}

/** "See all": the late tip (when late), then one tip per category, on the water colour; scrolls if needed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllTipsSheet(tips: List<Recommendation>, onDismiss: () -> Unit) {
    val colors = MaterialTheme.cycledColors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.water,
        contentColor = colors.text,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.outline) },
    ) {
        AllTipsContent(tips)
    }
}

@Composable
private fun AllTipsContent(tips: List<Recommendation>) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
            .navigationBarsPadding(),
    ) {
        Text(
            HomeFormat.TIPS_TITLE,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.cycledColors.text,
            modifier = Modifier
                .padding(bottom = 12.dp)
                .semantics { heading() },
        )
        TipRows(tips)
    }
}

// ---- Previews: the handoff's sample history (D12) with the rule-based predictor ----

private fun sampleState(today: LocalDate, logs: Map<LocalDate, DailyLog> = SampleData.dailyLogs) = buildHomeState(
    periods = groupIntoPeriods(SampleData.periodDays.keys.filter { !it.isAfter(today) }),
    today = today,
    predictor = AverageCyclePredictor(),
    dailyLogs = logs.filterKeys { !it.isAfter(today) },
)

@Composable
private fun PreviewHome(state: HomeUiState, today: LocalDate, showAllTips: Boolean = false) {
    CycledTheme { HomeScreen(state = state, today = today, onLogTodayClick = {}, showAllTips = showAllTips) }
}

/** Sun 8 Nov: cycle day 9, flood tide (the design's screenshot). */
@Preview(widthDp = 390, heightDp = 768)
@Composable
private fun HomeSampleDay9Preview() = PreviewHome(sampleState(SampleData.today), SampleData.today)

/** Sat 14 Nov: predicted ovulation, high water, full moon. */
@Preview(widthDp = 390, heightDp = 768)
@Composable
private fun HomeOvulationDayPreview() {
    val today = LocalDate.of(2026, 11, 14)
    PreviewHome(sampleState(today), today)
}

/** Fri 20 Nov: luteal, ebb tide; fertile window and high tide passed. */
@Preview(widthDp = 390, heightDp = 768)
@Composable
private fun HomeLutealPreview() {
    val today = LocalDate.of(2026, 11, 20)
    PreviewHome(sampleState(today), today)
}

/** Wed 2 Dec: predicted 28 Nov, so late: slack water, new moon, marker at the end. */
@Preview(widthDp = 390, heightDp = 768)
@Composable
private fun HomeLatePreview() {
    val today = LocalDate.of(2026, 12, 2)
    PreviewHome(sampleState(today), today)
}

@Preview(widthDp = 390, heightDp = 768)
@Composable
private fun HomeNoDataPreview() = PreviewHome(HomeUiState.NoData, SampleData.today)

/** Narrow phone at 1.3× font: the moon shrinks to fit and the screen scrolls. */
@Preview(widthDp = 360, heightDp = 720, fontScale = 1.3f)
@Composable
private fun HomeNarrowLargeFontPreview() = PreviewHome(sampleState(SampleData.today), SampleData.today)

/** The whole page at 360 dp and 1.3× font, unclipped. */
@Preview(widthDp = 360, heightDp = 1200, fontScale = 1.3f)
@Composable
private fun HomeNarrowLargeFontFullPreview() = PreviewHome(sampleState(SampleData.today), SampleData.today)

/** Mon 2 Nov: menstrual (cycle day 3) with Cramps and Tired logged today. */
@Preview(widthDp = 390, heightDp = 1100)
@Composable
private fun HomeMenstrualSymptomsPreview() {
    val today = LocalDate.of(2026, 11, 2)
    val logs = SampleData.dailyLogs + (today to DailyLog(today, symptoms = setOf(Symptom.CRAMPS, Symptom.FATIGUE)))
    PreviewHome(sampleState(today, logs), today)
}

/** Late (Wed 2 Dec), full page: the late tip leads "For today". */
@Preview(widthDp = 390, heightDp = 1100)
@Composable
private fun HomeLateTipsPreview() {
    val today = LocalDate.of(2026, 12, 2)
    PreviewHome(sampleState(today), today)
}

/** Low mood on 3 of the last 7 days: the "talk to someone" tip takes the Mind slot. */
@Preview(widthDp = 390, heightDp = 1100)
@Composable
private fun HomeLowMoodPreview() {
    val today = SampleData.today
    val low = listOf(1L, 3L, 5L).associate { back ->
        val date = today.minusDays(back)
        date to DailyLog(date, mood = Mood.LOW)
    }
    PreviewHome(sampleState(today, SampleData.dailyLogs + low), today)
}

/**
 * "See all" open. Android Studio draws a ModalBottomSheet only in interactive mode,
 * so [HomeAllTipsContentPreview] shows the sheet's content on its own as well.
 */
@Preview(widthDp = 390, heightDp = 768)
@Composable
private fun HomeSheetOpenPreview() = PreviewHome(sampleState(SampleData.today), SampleData.today, showAllTips = true)

@Preview(widthDp = 390, heightDp = 768)
@Composable
private fun HomeAllTipsContentPreview() = PreviewAllTips(SampleData.today)

/** "See all" when late (Wed 2 Dec): the late tip sits above the five general tips. */
@Preview(widthDp = 390, heightDp = 900)
@Composable
private fun HomeAllTipsLatePreview() = PreviewAllTips(LocalDate.of(2026, 12, 2))

/** The late sheet at 360 dp and 1.3× text: every body shows in full. */
@Preview(widthDp = 360, heightDp = 1300, fontScale = 1.3f)
@Composable
private fun HomeAllTipsLateNarrowPreview() = PreviewAllTips(LocalDate.of(2026, 12, 2))

@Composable
private fun PreviewAllTips(today: LocalDate) {
    val state = sampleState(today) as HomeUiState.Ready
    CycledTheme {
        Box(Modifier.background(MaterialTheme.cycledColors.water).padding(top = 24.dp)) {
            AllTipsContent(listOfNotNull(state.lateTip) + state.allRecommendations)
        }
    }
}
