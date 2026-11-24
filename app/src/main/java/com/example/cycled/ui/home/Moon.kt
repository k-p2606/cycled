package com.example.cycled.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cycled.domain.moonShadowPathData
import com.example.cycled.ui.theme.CycledTheme
import com.example.cycled.ui.theme.cycledColors

/** The moon's drawing box (handoff "Moon"): 164 × 164, centre (82, 82), disc radius 68. */
private const val BOX = 164f
private const val C = 82f
private const val DISC_R = 68f

private val PatchColor = Color(0xFFE7DBC2)
private val PatchLightColor = Color(0xFFEADFC8)
private val CraterColor = Color(0xFFDCCBAA)
private val CraterInnerColor = Color(0xFFE6D8BC)

/** (cx, cy, rx, ry, colour) */
private val Patches = listOf(
    Patch(70f, 70f, 19f, 14f, PatchColor),
    Patch(104f, 94f, 15f, 10f, PatchColor),
    Patch(84f, 122f, 13f, 7f, PatchLightColor),
)

/** About ten craters, r 3-10; the three largest get a lighter inner circle. */
private val Craters = listOf(
    Crater(58f, 96f, 10f),
    Crater(106f, 58f, 8f),
    Crater(98f, 120f, 7f),
    Crater(118f, 98f, 5.5f),
    Crater(52f, 62f, 5f),
    Crater(80f, 44f, 4f),
    Crater(70f, 134f, 4f),
    Crater(128f, 76f, 3.5f),
    Crater(40f, 84f, 3f),
    Crater(88f, 88f, 3f),
)

private data class Patch(val cx: Float, val cy: Float, val rx: Float, val ry: Float, val color: Color)
private data class Crater(val cx: Float, val cy: Float, val r: Float)

/**
 * The personal moon: new on cycle day 1, full at ovulation (D3). [phase] is 0..1
 * (0 = new, 0.5 = full). Decorative: screen readers get the hero row's text instead.
 * Draws into the largest centred square that fits.
 */
@Composable
fun Moon(phase: Double, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.cycledColors
    val shadow = remember(phase) {
        PathParser().parsePathString(moonShadowPathData(phase, C.toDouble(), C.toDouble(), DISC_R.toDouble())).toPath()
    }
    Canvas(modifier) {
        val side = size.minDimension
        translate((size.width - side) / 2, (size.height - side) / 2) {
            scale(side / BOX, pivot = Offset.Zero) {
                val centre = Offset(C, C)
                drawCircle(colors.dividerSky, radius = 79f, center = centre, style = Stroke(1f))
                drawCircle(colors.cream.copy(alpha = 0.07f), radius = 72f, center = centre, style = Stroke(6f))
                drawCircle(colors.cream, radius = DISC_R, center = centre)
                Patches.forEach { drawEllipse(it.cx, it.cy, it.rx, it.ry, it.color) }
                Craters.sortedByDescending { it.r }.forEachIndexed { i, crater ->
                    drawCircle(CraterColor, radius = crater.r, center = Offset(crater.cx, crater.cy))
                    if (i < 3) {
                        drawCircle(
                            CraterInnerColor,
                            radius = crater.r * 0.6f,
                            center = Offset(crater.cx + 1.5f, crater.cy + 1.5f),
                        )
                    }
                }
                drawPath(shadow, colors.moonDark, alpha = 0.93f)
            }
        }
    }
}

private fun DrawScope.drawEllipse(cx: Float, cy: Float, rx: Float, ry: Float, color: Color) =
    drawOval(color, topLeft = Offset(cx - rx, cy - ry), size = Size(2 * rx, 2 * ry))

@Preview(backgroundColor = 0xFF0A1C29, showBackground = true, widthDp = 560)
@Composable
private fun MoonPhasesPreview() {
    CycledTheme {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(0.0, 0.125, 0.25, 0.3, 0.5, 0.625, 0.75, 0.9).forEach {
                Moon(it, Modifier.size(64.dp))
            }
        }
    }
}
