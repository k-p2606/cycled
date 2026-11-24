package com.example.cycled.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.cycled.domain.Phase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contrast for the design's cycle colours (handoff D11): every day number passes
 * 4.5:1 against what it's drawn on, and coral, lavender and the dashed ring pass 3:1
 * against sky. Backgrounds are checked both as sky and as the selected cell, since
 * any unfilled day can be selected.
 */
class PhaseColorsTest {

    private val colors = phaseColorsFor(CycledDarkColors)
    private val backgrounds = mapOf(
        "sky" to colors.background,
        "selected cell" to colors.selectedBackground,
    )

    private fun contrast(a: Color, b: Color): Float {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05f) / (lo + 0.05f)
    }

    private fun assertContrast(name: String, fg: Color, bg: Color, min: Float) {
        val ratio = contrast(fg, bg)
        assertTrue("$name: $ratio < $min", ratio >= min)
    }

    @Test
    fun loggedNumber_readsOnCoral() =
        assertContrast("logged number on coral", colors.onLoggedFill, colors.loggedFill, 4.5f)

    @Test
    fun phaseNumbers_readOnTheirFill() {
        for (phase in Phase.entries) {
            val fill = colors.fill(phase)
            if (fill != null) {
                assertContrast("$phase number on its fill", colors.number(phase), fill, 4.5f)
            } else {
                backgrounds.forEach { (name, bg) ->
                    assertContrast("$phase number on $name", colors.number(phase), bg, 4.5f)
                }
            }
        }
    }

    @Test
    fun predictedAndPlainNumbers_readOnBackgrounds() {
        backgrounds.forEach { (name, bg) ->
            assertContrast("predicted number on $name", colors.predictedNumber, bg, 4.5f)
            assertContrast("default number on $name", colors.defaultNumber, bg, 4.5f)
        }
    }

    @Test
    fun coralLavenderAndDashedRing_standOutOnBackgrounds() {
        backgrounds.forEach { (name, bg) ->
            assertContrast("coral fill on $name", colors.loggedFill, bg, 3f)
            assertContrast("dashed ring on $name", colors.predictedRing, bg, 3f)
            assertContrast("lavender ring on $name", colors.ring(Phase.LUTEAL)!!, bg, 3f)
            assertContrast("today ring on $name", colors.todayRing, bg, 3f)
            assertContrast("logged dot on $name", colors.loggedDot, bg, 3f)
        }
    }

    @Test
    fun phaseStyles_matchTheDesign() {
        val c = CycledDarkColors
        assertEquals(c.follicular, colors.fill(Phase.FOLLICULAR))
        assertEquals(Color.White, colors.number(Phase.FOLLICULAR))
        assertEquals(c.cream, colors.fill(Phase.OVULATORY))
        assertEquals(c.sky, colors.number(Phase.OVULATORY))
        assertEquals(c.luteal, colors.ring(Phase.LUTEAL))
        assertEquals(null, colors.fill(Phase.LUTEAL))
        assertEquals(null, colors.fill(Phase.MENSTRUAL))
        assertEquals(c.period, colors.loggedFill)
        assertEquals(c.period, colors.predictedRing)
        assertEquals(c.periodLight, colors.predictedNumber)
    }

    @Test
    fun everyPhaseHasADistinctAccent() {
        assertEquals(4, Phase.entries.map { colors.accent(it) }.distinct().size)
    }

    @Test
    fun follicularTeal_standsOutOnSky_andWhiteReadsOnIt() {
        val teal = colors.fill(Phase.FOLLICULAR)!!
        assertEquals(CycledDarkColors.follicular, teal)
        assertContrast("teal fill on sky", teal, CycledDarkColors.sky, 3f)
        assertContrast("white number on teal", Color.White, teal, 4.5f)
    }

    @Test
    fun selectedCellBorder_standsOutOnSky() {
        assertEquals(CycledDarkColors.textMuted, colors.selectedBorder)
        assertContrast("selected-cell border on sky", colors.selectedBorder, colors.background, 3f)
    }

    @Test
    fun chipOutline_standsOutOnWater() =
        assertContrast("chip outline on water", CycledDarkColors.outline, CycledDarkColors.water, 3f)
}
