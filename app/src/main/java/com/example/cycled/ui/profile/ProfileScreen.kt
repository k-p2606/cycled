package com.example.cycled.ui.profile

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cycled.BuildConfig
import com.example.cycled.data.Settings
import com.example.cycled.domain.Period
import com.example.cycled.ui.SampleData
import com.example.cycled.ui.components.ListRow
import com.example.cycled.ui.components.screenTopPadding
import com.example.cycled.ui.theme.CycledTheme
import com.example.cycled.ui.theme.cycledColors

@Composable
fun ProfileRoute(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProfileScreen(
        state = state,
        onAvgCycleChange = viewModel::setAvgCycle,
        onAvgPeriodChange = viewModel::setAvgPeriod,
        onRemindPeriodChange = viewModel::setRemindPeriod,
        onRemindDailyChange = viewModel::setRemindDaily,
        onDeleteAllData = viewModel::deleteAllData,
        // Debug builds only (D12): the row isn't shown in release builds.
        onLoadSampleData = if (BuildConfig.DEBUG) viewModel::loadSampleData else null,
        modifier = modifier,
    )
}

/**
 * A plain settings page on the sky (handoff "Profile"). [state] null = still loading.
 * [onLoadSampleData] null hides the debug-only "Load sample data" row.
 */
@Composable
fun ProfileScreen(
    state: ProfileUiState?,
    onAvgCycleChange: (Int) -> Unit,
    onAvgPeriodChange: (Int) -> Unit,
    onRemindPeriodChange: (Boolean) -> Unit,
    onRemindDailyChange: (Boolean) -> Unit,
    onDeleteAllData: () -> Unit,
    onLoadSampleData: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.cycledColors
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var confirmSample by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxSize()
            .background(colors.sky)
            .verticalScroll(rememberScrollState())
            .screenTopPadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
    ) {
        if (state == null) return@Column
        val s = state.settings

        Header(state)
        Spacer(Modifier.height(28.dp))

        Section("My cycle") {
            ListRow(label = "Average cycle", minHeight = 52.dp) {
                Stepper(
                    value = s.avgCycle,
                    what = "average cycle",
                    canDecrease = state.canDecreaseCycle,
                    canIncrease = state.canIncreaseCycle,
                    onChange = onAvgCycleChange,
                )
            }
            ListRow(label = "Average period", minHeight = 52.dp) {
                Stepper(
                    value = s.avgPeriod,
                    what = "average period",
                    canDecrease = state.canDecreasePeriod,
                    canIncrease = state.canIncreasePeriod,
                    onChange = onAvgPeriodChange,
                )
            }
        }
        Section("Reminders") {
            SwitchRow("Period coming up", s.remindPeriod, onRemindPeriodChange)
            SwitchRow("Daily log nudge", s.remindDaily, onRemindDailyChange)
        }
        Section("Your data") {
            ListRow(
                label = "Export my data",
                labelColor = colors.textDim,
                modifier = Modifier.semantics(mergeDescendants = true) { disabled() },
            ) {
                Text("Coming soon", style = MaterialTheme.typography.bodyMedium, color = colors.textDim)
            }
            ListRow(
                label = "Delete all my data",
                labelColor = colors.period,
                minHeight = 48.dp,
                onClick = { confirmDelete = true },
            )
            if (onLoadSampleData != null) {
                ListRow(
                    label = "Load sample data (debug)",
                    labelColor = colors.textMuted,
                    onClick = { confirmSample = true },
                )
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            text = "Delete all your logged days and settings? This can't be undone.",
            confirmLabel = "Delete",
            onConfirm = { confirmDelete = false; onDeleteAllData() },
            onDismiss = { confirmDelete = false },
        )
    }
    if (confirmSample && onLoadSampleData != null) {
        ConfirmDialog(
            text = "Replace all logged days with the design's sample data? Settings are kept.",
            confirmLabel = "Replace",
            onConfirm = { confirmSample = false; onLoadSampleData() },
            onDismiss = { confirmSample = false },
        )
    }
}

@Composable
private fun Header(state: ProfileUiState) {
    val colors = MaterialTheme.cycledColors
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(60.dp)
                .background(colors.cream, CircleShape)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            Text(state.initial, style = MaterialTheme.typography.headlineSmall, color = colors.sky)
        }
        Column {
            Text(
                state.displayName,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.text,
                modifier = Modifier.semantics { heading() },
            )
            Text(state.cyclesLine, style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(bottom = 20.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.cycledColors.textMuted,
            modifier = Modifier
                .padding(bottom = 6.dp)
                .semantics { heading() },
        )
        content()
    }
}

/** − / "28 days" / +: 44 dp buttons that disable at the range ends. */
@Composable
private fun Stepper(value: Int, what: String, canDecrease: Boolean, canIncrease: Boolean, onChange: (Int) -> Unit) {
    val colors = MaterialTheme.cycledColors
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton("−", "Decrease $what", canDecrease) { onChange(value - 1) }
        Text(
            if (value == 1) "1 day" else "$value days",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 64.dp),
        )
        StepButton("+", "Increase $what", canIncrease) { onChange(value + 1) }
    }
}

@Composable
private fun StepButton(symbol: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .alpha(if (enabled) 1f else 0.35f),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            symbol,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.cycledColors.cream,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

/** The whole row is the switch's tap target; TalkBack reads it as one switch. */
@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListRow(
        label = label,
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
    ) {
        SwitchVisual(checked)
    }
}

/** 52 × 30 track inside a 56 × 44 area, 24 dp knob, 3 dp inset (handoff "Switch"). */
@Composable
private fun SwitchVisual(checked: Boolean) {
    val colors = MaterialTheme.cycledColors
    val knobX by animateDpAsState(if (checked) 25.dp else 3.dp, label = "knob")
    Box(
        Modifier
            .size(width = 56.dp, height = 44.dp)
            .semantics { stateDescription = if (checked) "On" else "Off" },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = 52.dp, height = 30.dp)
                .background(if (checked) colors.cream else colors.dividerSky, CircleShape),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .offset(x = knobX)
                    .size(24.dp)
                    .background(if (checked) colors.sky else colors.textMuted, CircleShape),
            )
        }
    }
}

@Composable
private fun ConfirmDialog(text: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.cycledColors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.water,
        textContentColor = colors.text,
        text = { Text(text, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(confirmLabel, color = colors.period, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Cancel", color = colors.text, style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}

// ---- Previews ----

private fun previewState(settings: Settings = Settings(), periods: List<Period> = SampleData.periods) =
    buildProfileState(settings, periods, SampleData.today)

@Composable
private fun PreviewProfile(state: ProfileUiState?, debug: Boolean = false) {
    CycledTheme {
        ProfileScreen(
            state = state,
            onAvgCycleChange = {},
            onAvgPeriodChange = {},
            onRemindPeriodChange = {},
            onRemindDailyChange = {},
            onDeleteAllData = {},
            onLoadSampleData = if (debug) ({}) else null,
        )
    }
}

/** Defaults with the sample history: "You", 3 cycles logged. */
@Preview(widthDp = 390, heightDp = 768)
@Composable
private fun ProfileDefaultsPreview() = PreviewProfile(previewState())

/** A name, both averages at their range ends (− or + disabled), switches flipped, debug row. */
@Preview(widthDp = 390, heightDp = 768)
@Composable
private fun ProfileRangeEndsPreview() = PreviewProfile(
    previewState(Settings(name = "Maya", avgCycle = 45, avgPeriod = 2, remindPeriod = false, remindDaily = true)),
    debug = true,
)

@Preview(widthDp = 390, heightDp = 768)
@Composable
private fun ProfileNoDataPreview() = PreviewProfile(previewState(periods = emptyList()))

@Preview(widthDp = 360, heightDp = 640, fontScale = 1.3f)
@Composable
private fun ProfileNarrowLargeFontPreview() = PreviewProfile(previewState(Settings(name = "Maya")), debug = true)

@Preview(widthDp = 390, heightDp = 400)
@Composable
private fun DeleteDialogPreview() {
    CycledTheme {
        ConfirmDialog("Delete all your logged days and settings? This can't be undone.", "Delete", {}, {})
    }
}
