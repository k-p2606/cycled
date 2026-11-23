package com.example.cycled.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Every colour token from the design handoff (docs/design/handoff.md, "Design tokens").
 * Read them with `MaterialTheme.cycledColors`. The common ones are also mapped into
 * the Material colour scheme (see Theme.kt), so stock components look right too.
 */
@Immutable
data class CycledColors(
    /** Screen background, tab bar, dark text on light fills. */
    val sky: Color,
    /** Lower panels, front wave layer. */
    val water: Color,
    /** Back wave layer behind the front one. */
    val waterBack: Color,
    /** Unlit side of the moon; draw it at 93% opacity. */
    val moonDark: Color,
    /** Big numbers, primary button, active tab, ovulation days, selected chips. */
    val cream: Color,
    /** Body text. */
    val text: Color,
    /** Secondary text on water. */
    val textMuted: Color,
    /** Secondary text on sky, inactive tabs. */
    val textDim: Color,
    /** Row lines on sky. */
    val dividerSky: Color,
    /** Row lines on water. */
    val dividerWater: Color,
    /** Unselected chip borders. */
    val outline: Color,
    /** Selected calendar day background. */
    val selectedCell: Color,
    /** Period days, Log period button, period dot. */
    val period: Color,
    /** Predicted-period number, "Period logged" text. */
    val periodLight: Color,
    /** Follicular day fill (white text on it). */
    val follicular: Color,
    /** Luteal day ring. */
    val luteal: Color,
    /** Fertile window dot, logged-day dot. */
    val fertile: Color,
    /** Estrogen line. */
    val estrogen: Color,
    /** Progesterone line. */
    val progesterone: Color,
    /** Day number on a follicular fill. */
    val onFollicular: Color,
)

/** The app's only palette: the design is dark-only. */
val CycledDarkColors = CycledColors(
    sky = Color(0xFF0A1C29),
    water = Color(0xFF134257),
    waterBack = Color(0xFF0F3347),
    moonDark = Color(0xFF17303E),
    cream = Color(0xFFF4EAD5),
    text = Color(0xFFEEF3F2),
    textMuted = Color(0xFFC4D5DB),
    textDim = Color(0xFFA9BFC7),
    dividerSky = Color(0xFF24414F),
    dividerWater = Color(0xFF2B5B70),
    outline = Color(0xFF4A7A8E),
    selectedCell = Color(0xFF1C4E64),
    period = Color(0xFFFF8A75),
    periodLight = Color(0xFFFFB3A5),
    follicular = Color(0xFF1F6A70),
    luteal = Color(0xFFB9A6E8),
    fertile = Color(0xFF8BE0D6),
    estrogen = Color(0xFFFFC46B),
    progesterone = Color(0xFFC9B8F5),
    onFollicular = Color.White,
)

val LocalCycledColors = staticCompositionLocalOf { CycledDarkColors }

val MaterialTheme.cycledColors: CycledColors
    @Composable
    @ReadOnlyComposable
    get() = LocalCycledColors.current
