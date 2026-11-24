package com.example.cycled.ui

import com.example.cycled.data.DailyLog
import com.example.cycled.data.FlowLevel
import com.example.cycled.domain.Period
import com.example.cycled.domain.groupIntoPeriods
import java.time.LocalDate

/**
 * The handoff's sample history (D12), for previews only (and later a debug-only
 * "Load sample data" action). Never real data.
 */
object SampleData {
    /** "Today" in the design: Sun 8 Nov 2026, cycle day 9. */
    val today: LocalDate = LocalDate.of(2026, 11, 8)

    /** The handoff's CSV: date, flow, symptoms (;-separated), mood. Design names (Tired, Tender, Bright). */
    private const val CSV = """
2026-11-04,Light,,Calm
2026-11-03,Medium,,
2026-11-02,Heavy,,
2026-11-01,Heavy,Cramps;Tired,
2026-10-31,Medium,Cramps,Low
2026-10-27,,Tender,Low
2026-10-22,,Bloating,Irritable
2026-10-15,,Headache,
2026-10-07,Light,,
2026-10-06,Light,,
2026-10-05,Medium,,
2026-10-04,Heavy,Cramps;Tired,Low
2026-10-03,Medium,Cramps,
2026-09-28,,Bloating;Acne,
2026-09-20,,,Bright
2026-09-11,Light,,
2026-09-10,Light,,
2026-09-09,Medium,Tired,
2026-09-08,Heavy,,
2026-09-07,Heavy,Cramps,
2026-08-30,,Headache,Irritable
2026-08-13,Light,,
2026-08-12,Light,,
2026-08-11,Medium,,
2026-08-10,Heavy,Cramps,
2026-08-09,Medium,,
"""

    private val rows = CSV.trim().lines().map { it.split(",") }

    /** Flow logged per day. */
    val periodDays: Map<LocalDate, FlowLevel> = rows
        .filter { it[1].isNotEmpty() }
        .associate { LocalDate.parse(it[0]) to FlowLevel.valueOf(it[1].uppercase()) }

    /** Symptoms and mood per day. */
    val dailyLogs: Map<LocalDate, DailyLog> = rows
        .filter { it[2].isNotEmpty() || it[3].isNotEmpty() }
        .associate { row ->
            val date = LocalDate.parse(row[0])
            val symptoms = row[2].split(";").filter { it.isNotEmpty() }
                .map { name -> SymptomChips.first { it.label == name } }.toSet()
            val mood = row[3].takeIf { it.isNotEmpty() }?.let { name -> MoodChips.first { it.label == name } }
            date to DailyLog(date, symptoms = symptoms, mood = mood)
        }

    /** Period starts 9 Aug, 7 Sep, 3 Oct and 31 Oct, five days each. */
    val periods: List<Period> = groupIntoPeriods(periodDays.keys.toList())
}
