package com.example.cycled.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cycled.domain.Phase
import com.example.cycled.domain.TideModel
import com.example.cycled.domain.TidePoint
import com.example.cycled.ui.PhaseDays
import com.example.cycled.ui.tideLabel
import com.example.cycled.ui.theme.CycledColors
import com.example.cycled.ui.theme.CycledTheme
import com.example.cycled.ui.theme.cycledColors

private val TideHeight = TideModel.HEIGHT.toFloat().dp

/** Gap kept between neighbouring phase labels; a label that would crowd the previous one is dropped. */
private val LabelGap = 6.dp

/**
 * The cycle drawn as water (handoff "Tide drawing"): two trochoid wave layers, the
 * typical hormone lines, phase labels in the water and the today marker. Geometry
 * comes from [tide] in the design's 390 × 172 box, stretched to the available width;
 * the height stays 172 dp. [markerDay] null hides the marker (no data).
 *
 * Decorative for screen readers: the hero row says where she is, the key says what
 * the lines are.
 */
@Composable
fun TideDrawing(
    tide: TideModel,
    markerDay: Int?,
    phaseDays: List<PhaseDays>,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.cycledColors
    val edge = remember(tide) { tide.waterEdge() }
    val curves = remember(tide) {
        val days = generateSequence(1.0) { it + 0.1 }.takeWhile { it <= tide.cycleLength + 1e-9 }.toList()
        Triple(
            days.map { TidePoint(tide.x(it), TideModel.hormoneY(tide.estrogen(it))) },
            days.map { TidePoint(tide.x(it), TideModel.hormoneY(tide.progesterone(it))) },
            days.map { TidePoint(tide.x(it), TideModel.hormoneY(tide.lh(it))) },
        )
    }
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = colors.textMuted)
    val todayStyle = MaterialTheme.typography.labelSmall.copy(color = colors.cream, fontWeight = FontWeight.SemiBold)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(TideHeight),
    ) {
        val sx = size.width / TideModel.WIDTH.toFloat()
        val sy = size.height / TideModel.HEIGHT.toFloat()
        fun at(x: Double, y: Double) = Offset(x.toFloat() * sx, y.toFloat() * sy)

        // Water: back layer, then front.
        drawPath(waterPath(edge, TideModel.BACK_DX, TideModel.BACK_DY, ::at), colors.waterBack)
        drawPath(waterPath(edge, 0.0, 0.0, ::at), colors.water)

        // Hormone lines (typical shapes, not her levels).
        val (estrogen, progesterone, lh) = curves
        drawPath(linePath(estrogen, ::at), colors.estrogen, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        drawPath(linePath(progesterone, ::at), colors.progesterone, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        drawPath(
            linePath(lh, ::at),
            colors.text,
            style = Stroke(
                1.75.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
            ),
        )

        // Phase labels, centred on each phase; skip one that would crowd its left neighbour.
        var lastRight = Float.NEGATIVE_INFINITY
        phaseDays.forEach { range ->
            val day = tide.labelDay(range.startDay, range.endDay) ?: return@forEach
            val layout = measurer.measure(range.phase.tideLabel, labelStyle)
            val w = layout.size.width.toFloat()
            val centreX = (tide.x(day).toFloat() * sx).coerceIn(w / 2, size.width - w / 2)
            val left = centreX - w / 2
            if (left < lastRight + LabelGap.toPx()) return@forEach
            drawText(layout, topLeft = Offset(left, TideModel.PHASE_LABEL_Y.toFloat() * sy - layout.size.height / 2f))
            lastRight = left + w
        }

        if (markerDay != null) {
            val point = tide.markerPoint(markerDay, edge)
            drawTodayMarker(at(point.x, point.y), TideModel.MARKER_LINE_END_Y.toFloat() * sy, colors)
            val layout = measurer.measure("Today", todayStyle)
            val c = at(point.x, point.y)
            val w = layout.size.width.toFloat()
            drawText(
                layout,
                topLeft = Offset(
                    (c.x - w / 2).coerceIn(0f, size.width - w),
                    (c.y - 24.dp.toPx() - layout.firstBaseline).coerceAtLeast(0f),
                ),
            )
        }
    }
}

private fun waterPath(
    edge: List<TidePoint>,
    dx: Double,
    dy: Double,
    at: (Double, Double) -> Offset,
): Path = Path().apply {
    edge.forEachIndexed { i, p ->
        val o = at(p.x + dx, p.y + dy)
        if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y)
    }
    val bottom = at(0.0, TideModel.HEIGHT).y
    lineTo(at(edge.last().x + dx, 0.0).x, bottom)
    lineTo(at(edge.first().x + dx, 0.0).x, bottom)
    close()
}

private fun linePath(points: List<TidePoint>, at: (Double, Double) -> Offset): Path = Path().apply {
    points.forEachIndexed { i, p ->
        val o = at(p.x, p.y)
        if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y)
    }
}

private fun DrawScope.drawTodayMarker(
    c: Offset,
    lineEndY: Float,
    colors: CycledColors,
) {
    val circleR = 12.dp.toPx()
    if (c.y + circleR < lineEndY) {
        drawLine(
            colors.cream.copy(alpha = 0.6f),
            start = Offset(c.x, c.y + circleR),
            end = Offset(c.x, lineEndY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 4.dp.toPx())),
        )
    }
    drawCircle(colors.cream.copy(alpha = 0.18f), radius = 17.dp.toPx(), center = c)
    drawCircle(colors.sky, radius = circleR, center = c)
    val border = 2.dp.toPx()
    drawCircle(colors.cream, radius = circleR - border / 2, center = c, style = Stroke(border))
    drawCircle(colors.cream, radius = 7.dp.toPx(), center = c)
}

@Preview(widthDp = 390)
@Composable
private fun TideDrawingPreview() {
    CycledTheme {
        TideDrawing(
            tide = TideModel(28, 15),
            markerDay = 9,
            phaseDays = listOf(
                PhaseDays(Phase.MENSTRUAL, 1, 5),
                PhaseDays(Phase.FOLLICULAR, 6, 13),
                PhaseDays(Phase.OVULATORY, 14, 16),
                PhaseDays(Phase.LUTEAL, 17, 28),
            ),
            modifier = Modifier.background(MaterialTheme.cycledColors.sky),
        )
    }
}
