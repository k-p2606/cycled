package com.example.cycled.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.cycled.R

/*
 * Bundled fonts (res/font, SIL OFL 1.1; licences in assets/licenses). google/fonts only
 * ships these as variable fonts, so each weight is an instance of the same file.
 *
 * Bricolage Grotesque has an optical-size axis. Browsers set it to the font size
 * automatically; Android doesn't, so each Bricolage family below pins it to the size
 * it's used at.
 */

// Font(resId, ..., variationSettings) is still marked experimental, though it's been
// stable in practice since Compose 1.3; it's the only way to pick a variable-font instance.
@OptIn(ExperimentalTextApi::class)
private fun bricolage(opticalSize: Float) = FontFamily(
    listOf(FontWeight.Medium, FontWeight.SemiBold).map { weight ->
        Font(
            R.font.bricolage_grotesque,
            weight = weight,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(weight.weight),
                FontVariation.Setting("opsz", opticalSize),
            ),
        )
    },
)

/** Bricolage Grotesque for the cycle day number (optical size at its max, 96). */
val BricolageDisplay = bricolage(96f)

/** Bricolage Grotesque for screen titles (30-34). */
val BricolageTitle = bricolage(32f)

/** Bricolage Grotesque for section titles and other headings (21-28). */
val BricolageHeading = bricolage(22f)

/** Figtree for everything else, 400/500/600. */
val Figtree = figtree()

@OptIn(ExperimentalTextApi::class)
private fun figtree() = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold).map { weight ->
        Font(
            R.font.figtree,
            weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
        )
    },
)

/** Tabular figures everywhere, so dates and counts line up and don't jiggle as they change. */
private const val TABULAR = "tnum"

private fun style(
    family: FontFamily,
    size: TextUnit,
    weight: FontWeight,
    lineHeight: TextUnit,
) = TextStyle(
    fontFamily = family,
    fontSize = size,
    fontWeight = weight,
    lineHeight = lineHeight,
    letterSpacing = 0.sp,
    fontFeatureSettings = TABULAR,
)

/**
 * The handoff's font table mapped onto Material slots:
 *
 * | Slot           | Use                              | Font / size / weight |
 * |----------------|----------------------------------|----------------------|
 * | displayLarge   | Cycle day number                 | Bricolage 112 / 500, line height 0.95 |
 * | headlineLarge  | Screen title                     | Bricolage 34 / 600   |
 * | headlineMedium | Calendar month                   | Bricolage 30 / 600   |
 * | headlineSmall  | Profile name and initial         | Bricolage 28 / 600   |
 * | titleLarge     | Section title (Logs heading)     | Bricolage 22 / 600   |
 * | titleMedium    | Section title (Calendar day)     | Bricolage 21 / 600   |
 * | titleSmall     | Primary button                   | Figtree 17 / 600     |
 * | bodyLarge      | Body, row labels                 | Figtree 16 / 400     |
 * | bodyMedium     | Body, row labels (smaller)       | Figtree 15 / 400     |
 * | bodySmall      | Small labels, legends            | Figtree 13 / 400     |
 * | labelLarge     | Chips, emphasised rows           | Figtree 15 / 500     |
 * | labelMedium    | Small labels (emphasised)        | Figtree 13 / 500     |
 * | labelSmall     | Tab labels (active: SemiBold)    | Figtree 12 / 400     |
 */
val Typography = Typography(
    displayLarge = style(BricolageDisplay, 112.sp, FontWeight.Medium, 0.95.em),
    displayMedium = style(BricolageDisplay, 56.sp, FontWeight.Medium, 1.em),
    displaySmall = style(BricolageTitle, 44.sp, FontWeight.Medium, 1.1.em),
    headlineLarge = style(BricolageTitle, 34.sp, FontWeight.SemiBold, 40.sp),
    headlineMedium = style(BricolageTitle, 30.sp, FontWeight.SemiBold, 36.sp),
    headlineSmall = style(BricolageHeading, 28.sp, FontWeight.SemiBold, 34.sp),
    titleLarge = style(BricolageHeading, 22.sp, FontWeight.SemiBold, 28.sp),
    titleMedium = style(BricolageHeading, 21.sp, FontWeight.SemiBold, 26.sp),
    titleSmall = style(Figtree, 17.sp, FontWeight.SemiBold, 22.sp),
    bodyLarge = style(Figtree, 16.sp, FontWeight.Normal, 22.sp),
    bodyMedium = style(Figtree, 15.sp, FontWeight.Normal, 20.sp),
    bodySmall = style(Figtree, 13.sp, FontWeight.Normal, 18.sp),
    labelLarge = style(Figtree, 15.sp, FontWeight.Medium, 20.sp),
    labelMedium = style(Figtree, 13.sp, FontWeight.Medium, 18.sp),
    labelSmall = style(Figtree, 12.sp, FontWeight.Normal, 16.sp),
)
