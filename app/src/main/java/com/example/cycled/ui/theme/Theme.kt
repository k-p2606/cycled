package com.example.cycled.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * The design tokens mapped onto Material roles, so stock components (Text, Button,
 * Switch, dialogs) pick up the palette without per-call colours.
 */
fun cycledColorScheme(c: CycledColors): ColorScheme = darkColorScheme(
    primary = c.cream,
    onPrimary = c.sky,
    primaryContainer = c.selectedCell,
    onPrimaryContainer = c.cream,
    inversePrimary = c.water,
    secondary = c.fertile,
    onSecondary = c.sky,
    secondaryContainer = c.water,
    onSecondaryContainer = c.text,
    tertiary = c.luteal,
    onTertiary = c.sky,
    tertiaryContainer = c.waterBack,
    onTertiaryContainer = c.text,
    background = c.sky,
    onBackground = c.text,
    surface = c.sky,
    onSurface = c.text,
    surfaceVariant = c.water,
    onSurfaceVariant = c.textDim,
    surfaceTint = c.cream,
    inverseSurface = c.cream,
    inverseOnSurface = c.sky,
    error = c.period,
    onError = c.sky,
    errorContainer = c.period,
    onErrorContainer = c.sky,
    outline = c.outline,
    outlineVariant = c.dividerSky,
    surfaceBright = c.selectedCell,
    surfaceDim = c.sky,
    surfaceContainerLowest = c.sky,
    surfaceContainerLow = c.waterBack,
    surfaceContainer = c.waterBack,
    surfaceContainerHigh = c.water,
    surfaceContainerHighest = c.selectedCell,
)

/**
 * The app theme. The design is dark-only and uses its own palette, so there is no
 * light scheme and no dynamic colour (D11).
 */
@Composable
fun CycledTheme(content: @Composable () -> Unit) {
    val colors = CycledDarkColors
    CompositionLocalProvider(LocalCycledColors provides colors) {
        MaterialTheme(
            colorScheme = cycledColorScheme(colors),
            typography = Typography,
            content = content,
        )
    }
}
