package com.example.cycled.domain

import com.example.cycled.data.FlowLevel
import com.example.cycled.data.Mood
import com.example.cycled.data.Symptom
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** What was logged on a day besides flow: "no flow" (D7), symptoms and mood. */
data class DayNotes(
    val noFlow: Boolean = false,
    val symptoms: Set<Symptom> = emptySet(),
    val mood: Mood? = null,
)

/** One day with something logged, as listed on Logs. */
data class LoggedDay(
    val date: LocalDate,
    val cycleDay: Int,           // 1 = the cycle's first day
    val flow: FlowLevel?,        // any flow, including Spotting
    val noFlow: Boolean,         // flow "None"; never true when [flow] is set
    val symptoms: Set<Symptom>,
    val mood: Mood?,
)

/**
 * One cycle on Logs: from a period start to the day before the next start, or, for
 * the newest cycle, on to today.
 */
data class CycleLog(
    val start: LocalDate,
    /** Last day (the day before the next start); null for the current cycle. */
    val end: LocalDate?,
    /** Completed cycle: its length. Current cycle: days so far, counting today. */
    val lengthDays: Int,
    /** Days in the cycle with flow other than None (Spotting counts). */
    val periodDays: Int,
    /** Days with flow, "no flow", symptoms or mood, newest first. */
    val days: List<LoggedDay>,
) {
    val isCurrent: Boolean get() = end == null
    val daysLogged: Int get() = days.size
}

/**
 * Splits the log into cycles, newest first (D10). Cycles come from the starts of
 * [periods] on or before [today]: each runs to the day before the next start, the
 * newest to [today]. A day is listed when it has flow ([periodDays]), or "no flow",
 * symptoms or a mood ([notes]). Days before the first start or after [today] belong to
 * no cycle and are left out. With no period started by [today], the list is empty.
 */
fun cycleLogs(
    periods: List<Period>,
    periodDays: Map<LocalDate, FlowLevel>,
    notes: Map<LocalDate, DayNotes>,
    today: LocalDate,
): List<CycleLog> {
    val starts = periods.map { it.start }.filter { !it.isAfter(today) }.distinct().sorted()
    if (starts.isEmpty()) return emptyList()

    val loggedDates = (periodDays.keys + notes.filterValues { it.hasSomething }.keys)
        .filter { !it.isBefore(starts.first()) && !it.isAfter(today) }
        .sortedDescending()

    return starts.mapIndexed { i, start ->
        val nextStart = starts.getOrNull(i + 1)
        val last = nextStart?.minusDays(1) ?: today
        val inCycle = loggedDates.filter { !it.isBefore(start) && !it.isAfter(last) }
        CycleLog(
            start = start,
            end = nextStart?.minusDays(1),
            lengthDays = ChronoUnit.DAYS.between(start, last).toInt() + 1,
            periodDays = inCycle.count { it in periodDays },
            days = inCycle.map { date ->
                val flow = periodDays[date]
                val note = notes[date]
                LoggedDay(
                    date = date,
                    cycleDay = ChronoUnit.DAYS.between(start, date).toInt() + 1,
                    flow = flow,
                    noFlow = flow == null && note?.noFlow == true,
                    symptoms = note?.symptoms.orEmpty(),
                    mood = note?.mood,
                )
            },
        )
    }.reversed()
}

private val DayNotes.hasSomething: Boolean
    get() = noFlow || symptoms.isNotEmpty() || mood != null
