package com.example.cycled.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The design's top padding (52-60) measured from the top of the screen, as in the
 * 390 × 844 frame, but never less than [minBelowStatusBar] under the status bar.
 * Adding 60 on top of the status bar left a large empty band on real phones.
 */
@Composable
fun Modifier.screenTopPadding(top: Dp = 60.dp, minBelowStatusBar: Dp = 16.dp): Modifier {
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    return padding(top = maxOf(top, statusBar + minBelowStatusBar))
}
