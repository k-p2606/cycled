package com.example.cycled.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.cycled.domain.Phase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhaseColorsTest {

    // The app's built-in (non-dynamic) schemes, as in Theme.kt.
    private val light = phaseColorsFor(lightColorScheme(primary = Purple40, secondary = PurpleGrey40, tertiary = Pink40))
    private val dark = phaseColorsFor(darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80))
    private val lightText = lightColorScheme().onSurface
    private val darkText = darkColorScheme().onSurface

    private fun contrast(a: Color, b: Color): Float {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05f) / (lo + 0.05f)
    }

    private fun assertMarkersStandOut(colors: PhaseColors, text: Color, theme: String) {
        for (phase in Phase.entries) {
            val tint = colors.tint(phase)
            fun check(name: String, c: Color, min: Float) {
                val ratio = contrast(c, tint)
                assertTrue("$theme $name on $phase tint: $ratio", ratio >= min)
            }
            check("day number", text, 4.5f)
            check("logged fill", colors.loggedFill, 3f)
            check("predicted outline", colors.predictedOutline, 3f)
            check("fertile dot", colors.fertileDot, 3f)
        }
        val dotOnLogged = contrast(colors.onLoggedFill, colors.loggedFill)
        assertTrue("$theme fertile dot on logged fill: $dotOnLogged", dotOnLogged >= 3f)
    }

    @Test
    fun light_markersStandOutOnEveryTint() = assertMarkersStandOut(light, lightText, "light")

    @Test
    fun dark_markersStandOutOnEveryTint() = assertMarkersStandOut(dark, darkText, "dark")

    @Test
    fun everyPhaseHasADistinctTint() {
        for (colors in listOf(light, dark)) {
            assertEquals(4, Phase.entries.map { colors.tint(it) }.distinct().size)
        }
    }
}
