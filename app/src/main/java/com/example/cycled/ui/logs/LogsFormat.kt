package com.example.cycled.ui.logs

import com.example.cycled.data.FlowLevel
import com.example.cycled.domain.CycleLog
import com.example.cycled.domain.CycleStats
import com.example.cycled.domain.LoggedDay
import com.example.cycled.ui.label
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Logs' text, kept pure so it can be unit tested (handoff "Logs"). */
internal object LogsFormat {

    private fun dayMonth(locale: Locale) = DateTimeFormatter.ofPattern("d MMM", locale)

    private fun days(n: Int) = if (n == 1) "1 day" else "$n days"

    /** Tab label and subline: "Current" / "Day 9", or "3 Oct" / "28 days". */
    fun tab(cycle: CycleLog, locale: Locale): Pair<String, String> =
        if (cycle.isCurrent) {
            "Current" to "Day ${cycle.lengthDays}"
        } else {
            cycle.start.format(dayMonth(locale)) to days(cycle.lengthDays)
        }

    /** "Current cycle, day 9" or "Cycle from 3 October, 28 days". */
    fun tabSpoken(cycle: CycleLog, locale: Locale): String =
        if (cycle.isCurrent) {
            "Current cycle, day ${cycle.lengthDays}"
        } else {
            "Cycle from ${cycle.start.format(DateTimeFormatter.ofPattern("d MMMM", locale))}, ${days(cycle.lengthDays)}"
        }

    /** "31 Oct – today" or "3 Oct – 30 Oct". */
    fun heading(cycle: CycleLog, locale: Locale): String {
        val end = cycle.end?.format(dayMonth(locale)) ?: "today"
        return "${cycle.start.format(dayMonth(locale))} – $end"
    }

    /** "Day 9 so far · period 5 days · 5 days logged" or "28-day cycle · period 5 days · 8 days logged". */
    fun subline(cycle: CycleLog): String {
        val length = if (cycle.isCurrent) "Day ${cycle.lengthDays} so far" else "${cycle.lengthDays}-day cycle"
        return "$length · period ${days(cycle.periodDays)} · ${days(cycle.daysLogged)} logged"
    }

    /** "Heavy flow", "Spotting", "No flow", or null when no flow was logged. */
    fun flowLine(day: LoggedDay): String? = when {
        day.flow == FlowLevel.SPOTTING -> "Spotting"
        day.flow != null -> "${day.flow.label} flow"
        day.noFlow -> "No flow"
        else -> null
    }

    /** "Cramps · Tired · Mood: low", or null when neither was logged. */
    fun detailLine(day: LoggedDay, locale: Locale): String? {
        val parts = day.symptoms.sortedBy { it.ordinal }.map { it.label } +
            listOfNotNull(day.mood?.let { "Mood: ${it.label.lowercase(locale)}" })
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }

    /** "Wednesday 4 November, light flow, mood: calm, day 5 of the cycle". */
    fun rowSpoken(day: LoggedDay, locale: Locale): String = buildList {
        add(day.date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale)))
        flowLine(day)?.let { add(it.lowercase(locale)) }
        detailLine(day, locale)?.let { add(it.replace(" · ", ", ").lowercase(locale)) }
        add("day ${day.cycleDay} of the cycle")
    }.joinToString(", ")

    /** The summary row's items, value over label: "28 days" / "avg cycle", ... Null stats are left out. */
    fun summaryItems(stats: CycleStats): List<Pair<String, String>> = listOfNotNull(
        days(stats.averageCycleDays) to "avg cycle",
        stats.averagePeriodDays?.let { days(it) to "avg period" },
        stats.variationDays?.let { "±${days(it)}" to "varies by" },
    )

    /**
     * The summary as one sentence: "Over your last 3 cycles: average cycle 28 days,
     * average period 5 days, varies by about 1 day."
     */
    fun summarySpoken(stats: CycleStats): String {
        val over = if (stats.cyclesUsed == 1) "Over your last cycle" else "Over your last ${stats.cyclesUsed} cycles"
        val parts = listOfNotNull(
            "average cycle ${days(stats.averageCycleDays)}",
            stats.averagePeriodDays?.let { "average period ${days(it)}" },
            stats.variationDays?.let { if (it == 0) "varies by less than a day" else "varies by about ${days(it)}" },
        )
        return "$over: ${parts.joinToString(", ")}."
    }

    /** "4" and "Wed". */
    fun dateNumber(date: LocalDate): String = date.dayOfMonth.toString()
    fun weekday(date: LocalDate, locale: Locale): String = date.format(DateTimeFormatter.ofPattern("EEE", locale))
}
