package com.example.cycled.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cycled.ui.theme.CycledTheme
import com.example.cycled.ui.theme.cycledColors

val ChipHeight = 44.dp
private val ChipGap = 6.dp
/** Side padding for chips sized to their label (Symptoms). */
private val ChipSidePadding = 14.dp
/** Side padding for chips that fill a grid cell (Flow, Mood); the label is centred anyway. */
private val GridChipSidePadding = 6.dp
private const val DISABLED_ALPHA = 0.4f

/**
 * A choice chip (handoff "Choice chip"): at least 44 dp tall, fully rounded,
 * Figtree 15/500. Off: transparent with an outline border. On: cream fill, sky text.
 *
 * With [multiSelect] false it's one option of a pick-one group: TalkBack reads it as
 * a radio button, "selected" / "not selected". With [multiSelect] true it's a
 * pick-any option: read as a checkbox, "checked" / "not checked".
 *
 * Long labels wrap rather than truncate, so the chip grows at large font sizes.
 */
@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    multiSelect: Boolean = false,
    horizontalPadding: Dp = ChipSidePadding,
) {
    val colors = MaterialTheme.cycledColors
    val interaction = if (multiSelect) {
        Modifier.toggleable(value = selected, enabled = enabled, role = Role.Checkbox, onValueChange = { onClick() })
    } else {
        Modifier.selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
    }
    Box(
        modifier = modifier
            .heightIn(min = ChipHeight)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(CircleShape)
            .background(if (selected) colors.cream else Color.Transparent)
            .border(1.dp, if (selected) colors.cream else colors.outline, CircleShape)
            .then(interaction)
            .padding(horizontal = horizontalPadding, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.sky else colors.text,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * A pick-one group (Flow, Mood) in a grid with 6 dp gaps: [columns] wide, or fewer
 * when the longest label wouldn't fit on one line (narrow phones, large fonts), so
 * words never break mid-way. Tapping the selected chip again clears it: [onSelect]
 * gets null.
 */
@Composable
fun <T> SingleChoiceChipGroup(
    options: List<T>,
    selected: T?,
    onSelect: (T?) -> Unit,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    columns: Int = 4,
) {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelLarge
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val widest = remember(options, style, density) {
            options.maxOfOrNull { measurer.measure(optionLabel(it), style).size.width } ?: 0
        }
        val needed = with(density) { widest.toDp() } + GridChipSidePadding * 2 + 4.dp
        val fits = (columns downTo 1).firstOrNull { c -> (maxWidth - ChipGap * (c - 1)) / c >= needed } ?: 1
        // Four chips go 4 → 2 → 1 per row, so rows stay even.
        val cols = when {
            fits >= columns -> columns
            fits >= 2 -> 2
            else -> 1
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(ChipGap),
        ) {
            options.chunked(cols).forEach { row ->
                // Equal heights across the row even if one label wraps.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(ChipGap),
                ) {
                    row.forEach { option ->
                        ChoiceChip(
                            label = optionLabel(option),
                            selected = option == selected,
                            onClick = { onSelect(if (option == selected) null else option) },
                            enabled = enabled,
                            horizontalPadding = GridChipSidePadding,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                    repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** A pick-any group (Symptoms): chips sized to their labels, wrapping, 6 dp gaps. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> MultiChoiceChipGroup(
    options: List<T>,
    selected: Set<T>,
    onToggle: (T) -> Unit,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ChipGap),
        verticalArrangement = Arrangement.spacedBy(ChipGap),
    ) {
        options.forEach { option ->
            ChoiceChip(
                label = optionLabel(option),
                selected = option in selected,
                onClick = { onToggle(option) },
                enabled = enabled,
                multiSelect = true,
            )
        }
    }
}

// ---- Previews ----

private val FlowOptions = listOf("None", "Light", "Medium", "Heavy")
private val SymptomOptions = listOf("Cramps", "Headache", "Bloating", "Tired", "Tender", "Acne")
private val MoodOptions = listOf("Calm", "Low", "Irritable", "Bright")

@Composable
private fun ChipGroupsSample(enabled: Boolean = true) {
    var flow by remember { mutableStateOf<String?>("Medium") }
    var symptoms by remember { mutableStateOf(setOf("Cramps", "Tired")) }
    var mood by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier
            .background(MaterialTheme.cycledColors.water)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SingleChoiceChipGroup(FlowOptions, flow, { flow = it }, { it }, enabled = enabled)
        MultiChoiceChipGroup(
            SymptomOptions, symptoms,
            { s -> symptoms = if (s in symptoms) symptoms - s else symptoms + s }, { it },
            enabled = enabled,
        )
        SingleChoiceChipGroup(MoodOptions, mood, { mood = it }, { it }, enabled = enabled)
    }
}

@Preview(widthDp = 390)
@Composable
private fun ChipGroupsPreview() {
    CycledTheme { ChipGroupsSample() }
}

@Preview(widthDp = 360, fontScale = 1.5f)
@Composable
private fun ChipGroupsNarrowLargeFontPreview() {
    CycledTheme { ChipGroupsSample() }
}

@Preview(widthDp = 390)
@Composable
private fun ChipGroupsDisabledPreview() {
    CycledTheme { ChipGroupsSample(enabled = false) }
}
