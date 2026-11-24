package com.example.cycled.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints

/**
 * For a scrolling screen of a sky part over a water panel: places [top], then [bottom]
 * stretched to at least the rest of [viewportHeight] (px), so the water reaches the
 * bottom on tall screens and the whole page scrolls on short ones. Put
 * `verticalScroll` in [modifier].
 */
@Composable
fun TopThenFill(
    viewportHeight: Int,
    modifier: Modifier,
    top: @Composable () -> Unit,
    bottom: @Composable () -> Unit,
) {
    Layout(contents = listOf(top, bottom), modifier = modifier) { (topParts, bottomParts), constraints ->
        val width = constraints.maxWidth
        val topPlaced = topParts.map { it.measure(Constraints(minWidth = width, maxWidth = width)) }
        val topHeight = topPlaced.sumOf { it.height }
        val fill = (viewportHeight - topHeight).coerceAtLeast(0)
        val bottomPlaced = bottomParts.map {
            it.measure(Constraints(minWidth = width, maxWidth = width, minHeight = fill))
        }
        layout(width, topHeight + bottomPlaced.sumOf { it.height }) {
            var y = 0
            (topPlaced + bottomPlaced).forEach { it.place(0, y); y += it.height }
        }
    }
}
