package com.example.cycled.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.example.cycled.domain.Phase

/**
 * The one place cycle colours come from: Calendar's day cells and legend, and Home's
 * phase drawing. Read them with `MaterialTheme.phaseColors`.
 *
 * Calendar day styling (handoff "Calendar", first match wins):
 * - flow logged: [loggedFill] with the number in [onLoggedFill];
 * - predicted period: a dashed [predictedRing] with the number in [predictedNumber];
 * - otherwise the phase: [fill] (if any) and [ring] (if any), number in [number].
 * Then today gets a [todayRing] and the selected day sits on [selectedBackground]
 * inside a [selectedBorder] (the fill alone is too close to sky to show selection).
 *
 * PhaseColorsTest checks every number against what it's drawn on (4.5:1) and every
 * ring, fill edge and dot that carries meaning against the backgrounds (3:1).
 */
@Immutable
data class PhaseColors(
    /** Screen background behind the grid. */
    val background: Color,
    /** Cell background for the selected day. */
    val selectedBackground: Color,
    /** 1.5 dp border around the selected cell; the main selection indicator. */
    val selectedBorder: Color,
    /** Fill for a day with flow logged. */
    val loggedFill: Color,
    val onLoggedFill: Color,
    /** Dashed ring for a predicted period day. */
    val predictedRing: Color,
    /** Number colour for a predicted period day. */
    val predictedNumber: Color,
    /** Ring around today, replacing any other ring. */
    val todayRing: Color,
    /** Small dot under a day with symptoms or mood logged. */
    val loggedDot: Color,
    private val fills: Map<Phase, Color>,
    private val rings: Map<Phase, Color>,
    private val numbers: Map<Phase, Color>,
    private val accents: Map<Phase, Color>,
    /** Number colour on a day with no style. */
    val defaultNumber: Color,
) {
    /** Solid fill for a day in [phase], or null when that phase has no fill. */
    fun fill(phase: Phase): Color? = fills[phase]

    /** Ring for a day in [phase], or null when that phase has no ring. */
    fun ring(phase: Phase): Color? = rings[phase]

    /** Day number colour for [phase], drawn on [fill] or the background. */
    fun number(phase: Phase): Color = numbers[phase] ?: defaultNumber

    /** The phase's signature colour, for swatches, bars and labels. */
    fun accent(phase: Phase): Color = accents.getValue(phase)
}

/** [PhaseColors] built from the design tokens (handoff D2 and "Calendar"). */
fun phaseColorsFor(c: CycledColors): PhaseColors = PhaseColors(
    background = c.sky,
    selectedBackground = c.selectedCell,
    selectedBorder = c.textMuted,
    loggedFill = c.period,
    onLoggedFill = c.sky,
    predictedRing = c.period,
    predictedNumber = c.periodLight,
    todayRing = c.cream,
    loggedDot = c.fertile,
    fills = mapOf(
        Phase.FOLLICULAR to c.follicular,
        Phase.OVULATORY to c.cream,
    ),
    rings = mapOf(
        Phase.LUTEAL to c.luteal,
    ),
    numbers = mapOf(
        Phase.FOLLICULAR to c.onFollicular,
        Phase.OVULATORY to c.sky,
    ),
    accents = mapOf(
        Phase.MENSTRUAL to c.period,
        Phase.FOLLICULAR to c.follicular,
        Phase.OVULATORY to c.cream,
        Phase.LUTEAL to c.luteal,
    ),
    defaultNumber = c.text,
)

val MaterialTheme.phaseColors: PhaseColors
    @Composable
    @ReadOnlyComposable
    get() = phaseColorsFor(cycledColors)
