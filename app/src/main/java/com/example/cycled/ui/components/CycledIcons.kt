package com.example.cycled.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cycled.ui.theme.CycledTheme
import com.example.cycled.ui.theme.cycledColors

/**
 * The app's own line icons (handoff "Tab bar"): 24 dp, 1.8 stroke, round caps.
 * Drawn in black; tint them with `Icon(tint = ...)`.
 */
object CycledIcons {

    /** Two stacked wavy lines. */
    val Home: ImageVector = lineIcon(
        "Home",
        strokes = listOf(
            "M3 9.5c2-2.2 4-2.2 6 0s4 2.2 6 0 4-2.2 6 0",
            "M3 15.5c2-2.2 4-2.2 6 0s4 2.2 6 0 4-2.2 6 0",
        ),
    )

    /** Rounded rectangle with two tabs on top. */
    val Calendar: ImageVector = lineIcon(
        "Calendar",
        strokes = listOf(
            "M6 5.5h12a2.5 2.5 0 0 1 2.5 2.5v10.5a2.5 2.5 0 0 1-2.5 2.5H6a2.5 2.5 0 0 1-2.5-2.5V8A2.5 2.5 0 0 1 6 5.5z",
            "M8 3v4.5",
            "M16 3v4.5",
            "M3.5 10.5h17",
        ),
    )

    /** Three lines with a dot before each. */
    val Logs: ImageVector = lineIcon(
        "Logs",
        strokes = listOf("M9 6.5h11", "M9 12h11", "M9 17.5h11"),
        fills = listOf(dot(4.5f, 6.5f), dot(4.5f, 12f), dot(4.5f, 17.5f)),
    )

    /** Head and shoulders. */
    val Profile: ImageVector = lineIcon(
        "Profile",
        strokes = listOf(
            "M12 4a3.75 3.75 0 1 1 0 7.5a3.75 3.75 0 1 1 0-7.5z",
            "M4.5 20.5c0-3.9 3.4-6.5 7.5-6.5s7.5 2.6 7.5 6.5",
        ),
    )

    /** A tick, e.g. "Period logged". */
    val Check: ImageVector = lineIcon("Check", strokes = listOf("M5 12.5l4.5 4.5L19 7.5"))

    /** Chevrons for month navigation. */
    val ChevronLeft: ImageVector = lineIcon("ChevronLeft", strokes = listOf("M14.5 6l-6 6 6 6"))
    val ChevronRight: ImageVector = lineIcon("ChevronRight", strokes = listOf("M9.5 6l6 6-6 6"))
}

private const val STROKE_WIDTH = 1.8f
private const val DOT_RADIUS = 1.25f

/** A filled circle as SVG path data. */
private fun dot(cx: Float, cy: Float, r: Float = DOT_RADIUS) =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

private fun lineIcon(
    name: String,
    strokes: List<String>,
    fills: List<String> = emptyList(),
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    strokes.forEach { data ->
        addPath(
            pathData = addPathNodes(data),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    fills.forEach { data -> addPath(pathData = addPathNodes(data), fill = SolidColor(Color.Black)) }
}.build()

@Preview(showBackground = true, backgroundColor = 0xFF0A1C29)
@Composable
private fun CycledIconsPreview() {
    CycledTheme {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(
                CycledIcons.Home, CycledIcons.Calendar, CycledIcons.Logs, CycledIcons.Profile,
                CycledIcons.Check, CycledIcons.ChevronLeft, CycledIcons.ChevronRight,
            ).forEach { Icon(it, contentDescription = null, tint = MaterialTheme.cycledColors.cream) }
        }
    }
}
