package com.example.cycled.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.example.cycled.domain.Phase

/**
 * The one place cycle colours come from: Calendar's day cells and legend, and Home's
 * phase bar. Read them with `MaterialTheme.phaseColors`.
 *
 * Each phase has a strong [accent] and a soft [tint]: the accent mixed into the
 * theme's surface colour, so tints follow the theme (including dynamic colour)
 * while staying distinct from each other. Menstrual and ovulatory accents are the
 * app's own Pink and Purple; follicular (green) and luteal (amber) are added so
 * neighbouring phases always differ in hue.
 *
 * Markers drawn on top of tints (logged fill, predicted outline, fertile dot) use
 * tone-40 colours in light theme and tone-80 in dark, giving at least 4.5:1 against
 * every tint with the app's built-in colour schemes. PhaseColorsTest checks this.
 */
@Immutable
data class PhaseColors(
    private val tints: Map<Phase, Color>,
    private val accents: Map<Phase, Color>,
    /** Fill for a logged period day. Draw the day number in [onLoggedFill]. */
    val loggedFill: Color,
    val onLoggedFill: Color,
    /** Dashed outline for a predicted period day. */
    val predictedOutline: Color,
    /** Fertile-window dot on an unlogged day; use [onLoggedFill] on a logged day. */
    val fertileDot: Color,
) {
    /** Soft background for [phase]; text in onSurface reads on it. */
    fun tint(phase: Phase): Color = tints.getValue(phase)

    /** Strong colour for [phase], e.g. for a label, outline or selected segment. */
    fun accent(phase: Phase): Color = accents.getValue(phase)
}

private val LightAccents = mapOf(
    Phase.MENSTRUAL to Pink40,
    Phase.FOLLICULAR to Color(0xFF3B7A57),
    Phase.OVULATORY to Purple40,
    Phase.LUTEAL to Color(0xFF8C6D1F),
)

private val DarkAccents = mapOf(
    Phase.MENSTRUAL to Pink80,
    Phase.FOLLICULAR to Color(0xFF9AD4B0),
    Phase.OVULATORY to Purple80,
    Phase.LUTEAL to Color(0xFFE6C379),
)

private const val LIGHT_TINT_FRACTION = 0.22f
private const val DARK_TINT_FRACTION = 0.25f

/** Builds [PhaseColors] for [scheme]; dark or light is read from its surface colour. */
fun phaseColorsFor(scheme: ColorScheme): PhaseColors {
    val dark = scheme.surface.luminance() < 0.5f
    val accents = if (dark) DarkAccents else LightAccents
    val fraction = if (dark) DARK_TINT_FRACTION else LIGHT_TINT_FRACTION
    return PhaseColors(
        tints = accents.mapValues { (_, accent) -> lerp(scheme.surface, accent, fraction) },
        accents = accents,
        loggedFill = scheme.primary,
        onLoggedFill = scheme.onPrimary,
        predictedOutline = accents.getValue(Phase.MENSTRUAL),
        fertileDot = scheme.primary,
    )
}

val MaterialTheme.phaseColors: PhaseColors
    @Composable
    @ReadOnlyComposable
    get() = phaseColorsFor(colorScheme)
