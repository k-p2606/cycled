package com.example.cycled.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cycled.ui.PhaseBarModel
import com.example.cycled.ui.PhaseSegment
import com.example.cycled.ui.displayName
import com.example.cycled.ui.theme.phaseColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val BarHeight = 12.dp
private val SegmentGap = 2.dp
private val MarkerHeight = 7.dp

/** Phases other than the current one are drawn faded. */
private const val MutedAlpha = 0.4f

/**
 * The cycle as a bar of phase segments with a marker over today, then one row per
 * phase with its approximate dates, then "Day X of about Y". Dates are kept off the
 * bar itself because narrow segments would make labels collide.
 */
@Composable
fun PhaseBar(model: PhaseBarModel, modifier: Modifier = Modifier) {
    val phaseColors = MaterialTheme.phaseColors
    val markerColor = MaterialTheme.colorScheme.onSurface
    val locale = Locale.getDefault()
    val description = remember(model, locale) { model.spokenDescription(locale) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // The marker and the bar are read out as one element.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics { contentDescription = description },
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Canvas(Modifier.fillMaxWidth().height(MarkerHeight)) {
                // A small downward triangle over the middle of today.
                val x = model.todayX(size.width, SegmentGap.toPx())
                val half = size.height * 0.7f
                drawPath(
                    Path().apply {
                        moveTo(x - half, 0f)
                        lineTo(x + half, 0f)
                        lineTo(x, size.height)
                        close()
                    },
                    color = markerColor,
                )
            }
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(BarHeight)
                    .clip(RoundedCornerShape(50)),
            ) {
                val spans = model.segmentSpans(size.width, SegmentGap.toPx())
                model.segments.forEachIndexed { i, segment ->
                    val (start, end) = spans[i]
                    val color = phaseColors.accent(segment.phase)
                        .copy(alpha = if (segment.phase == model.currentPhase) 1f else MutedAlpha)
                    drawRect(color, topLeft = Offset(start, 0f), size = Size(end - start, size.height))
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            model.segments.forEach { segment ->
                PhaseRow(segment, isCurrent = segment.phase == model.currentPhase, locale = locale)
            }
        }

        Text(
            "Day ${model.cycleDay} of about ${model.cycleLength}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PhaseRow(segment: PhaseSegment, isCurrent: Boolean, locale: Locale) {
    val phaseColors = MaterialTheme.phaseColors
    val name = segment.phase.displayName
    val spoken = buildString {
        append("$name phase, about ${longRange(segment.start, segment.end, locale)}")
        if (isCurrent) append(", current")
    }

    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(12.dp)
                .background(
                    phaseColors.accent(segment.phase).copy(alpha = if (isCurrent) 1f else MutedAlpha),
                    RoundedCornerShape(3.dp),
                ),
        )
        Text(
            "$name · ~${shortRange(segment.start, segment.end, locale)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

/** "Day 15 of about 30. Estimated ovulatory phase, November 15 to 17. Next period predicted around December 1." */
private fun PhaseBarModel.spokenDescription(locale: Locale): String {
    val current = segments.first { it.phase == currentPhase }
    val phaseName = currentPhase.displayName.lowercase(locale)
    return "Day $cycleDay of about $cycleLength. " +
        "Estimated $phaseName phase, ${longRange(current.start, current.end, locale)}. " +
        "Next period predicted around ${nextStart.format(DateTimeFormatter.ofPattern("MMMM d", locale))}."
}

/** "Nov 15", "Nov 15 – 17" or "Nov 29 – Dec 3". */
private fun shortRange(start: LocalDate, end: LocalDate, locale: Locale): String =
    dateRange(start, end, locale, monthPattern = "MMM", separator = " – ")

/** "November 15", "November 15 to 17" or "November 29 to December 3". */
private fun longRange(start: LocalDate, end: LocalDate, locale: Locale): String =
    dateRange(start, end, locale, monthPattern = "MMMM", separator = " to ")

private fun dateRange(
    start: LocalDate,
    end: LocalDate,
    locale: Locale,
    monthPattern: String,
    separator: String,
): String {
    val full = DateTimeFormatter.ofPattern("$monthPattern d", locale)
    return when {
        start == end -> start.format(full)
        start.month == end.month && start.year == end.year ->
            start.format(full) + separator + end.dayOfMonth
        else -> start.format(full) + separator + end.format(full)
    }
}
