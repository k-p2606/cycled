package com.example.cycled.ui.home

import com.example.cycled.domain.RecCategory
import com.example.cycled.domain.Recommendation
import com.example.cycled.ui.ComingUpKind
import com.example.cycled.ui.ComingUpRow
import com.example.cycled.ui.CycleMessages
import com.example.cycled.ui.DateSpan
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Home's text formatting, kept pure so it can be unit tested. */
internal object HomeFormat {

    /** "Sunday, 8 Nov". */
    fun dateLine(today: LocalDate, locale: Locale): String =
        today.format(DateTimeFormatter.ofPattern("EEEE, d MMM", locale))

    /** "Sat 28 Nov" for a single day; "9–14 Nov" or "29 Nov – 4 Dec" for a range. */
    fun rowDate(row: ComingUpRow, locale: Locale): String {
        val (start, end) = row.dates
        if (start == end) return start.format(DateTimeFormatter.ofPattern("EEE d MMM", locale))
        val dayMonth = DateTimeFormatter.ofPattern("d MMM", locale)
        return if (start.month == end.month && start.year == end.year) {
            "${start.dayOfMonth}–${end.format(dayMonth)}"
        } else {
            "${start.format(dayMonth)} – ${end.format(dayMonth)}"
        }
    }

    /** "Likely 25 Nov – 1 Dec. Estimates from your logged dates, ..." (D4). */
    fun rangeLine(range: DateSpan, locale: Locale): String {
        val dayMonth = DateTimeFormatter.ofPattern("d MMM", locale)
        return "Likely ${range.start.format(dayMonth)} – ${range.end.format(dayMonth)}. " +
            CycleMessages.HOME_DISCLAIMER
    }

    fun rowLabel(kind: ComingUpKind): String = when (kind) {
        ComingUpKind.NEXT_PERIOD -> "Next period"
        ComingUpKind.FERTILE_WINDOW -> "Fertile window"
        ComingUpKind.HIGH_TIDE -> "High tide"
    }

    /** "Next period, Saturday 28 November, in 20 days"; ranges read "9 to 14 November". */
    fun rowSpoken(row: ComingUpRow, locale: Locale): String {
        val (start, end) = row.dates
        val full = DateTimeFormatter.ofPattern("d MMMM", locale)
        val dates = when {
            start == end -> start.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale))
            start.month == end.month && start.year == end.year -> "${start.dayOfMonth} to ${end.format(full)}"
            else -> "${start.format(full)} to ${end.format(full)}"
        }
        return "${rowLabel(row.kind)}, $dates, ${row.until}"
    }

    /** What the hero row reads as: "Cycle day 9, Flood tide, follicular phase". */
    fun heroSpoken(cycleDay: Int, tideName: String, subline: String, locale: Locale): String =
        "Cycle day $cycleDay, $tideName, ${subline.replaceFirstChar { it.lowercase(locale) }}"

    /** "For today" section and sheet title. */
    const val TIPS_TITLE = "For today"

    /** The line under the tips, on Home and in the sheet. */
    const val TIPS_DISCLAIMER = "General wellbeing tips, not medical advice. If pain or low mood is severe " +
        "or stops you doing everyday things, talk to a clinician."

    /** "Body", "Fitness", "Health", "Mind", "Food". */
    fun categoryLabel(category: RecCategory): String = when (category) {
        RecCategory.BODY -> "Body"
        RecCategory.FITNESS -> "Fitness"
        RecCategory.HEALTH -> "Health"
        RecCategory.MIND -> "Mind"
        RecCategory.FOOD -> "Food"
    }

    /** "Body tip: Heat for cramps. Try a hot water bottle ..." */
    fun tipSpoken(tip: Recommendation): String = "${categoryLabel(tip.category)} tip: ${tip.title}. ${tip.body}"
}
