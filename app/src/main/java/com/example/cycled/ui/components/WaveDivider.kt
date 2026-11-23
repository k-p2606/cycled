package com.example.cycled.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cycled.ui.theme.CycledTheme
import com.example.cycled.ui.theme.cycledColors

/** The handoff's wave, drawn in a 390 × 16 box. */
private const val WAVE_PATH =
    "M0 16V9C33 1 65 1 98 8S163 15 195 8 260 1 293 8 358 15 390 8V16Z"
private const val WAVE_WIDTH = 390f
private const val WAVE_HEIGHT = 16f

/**
 * The wavy edge where the sky turns into the lower water panel (Calendar, Logs).
 * Stretched horizontally to the available width; always 16 dp tall. Decorative,
 * so screen readers skip it. Put it directly above a panel of the same [color].
 */
@Composable
fun WaveDivider(modifier: Modifier = Modifier, color: Color = MaterialTheme.cycledColors.water) {
    val path = remember { PathParser().parsePathString(WAVE_PATH).toPath() }
    Canvas(
        modifier
            .fillMaxWidth()
            .height(WAVE_HEIGHT.dp),
    ) {
        scale(scaleX = size.width / WAVE_WIDTH, scaleY = size.height / WAVE_HEIGHT, pivot = Offset.Zero) {
            drawPath(path, color)
        }
    }
}

@Preview(widthDp = 390)
@Composable
private fun WaveDividerPreview() {
    CycledTheme {
        val colors = MaterialTheme.cycledColors
        Column(Modifier.background(colors.sky)) {
            Spacer(Modifier.height(24.dp))
            WaveDivider()
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(colors.water),
            )
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun WaveDividerNarrowPreview() = WaveDividerPreview()
