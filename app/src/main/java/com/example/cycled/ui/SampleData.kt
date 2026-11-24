package com.example.cycled.ui

import com.example.cycled.domain.Period
import java.time.LocalDate

/**
 * The handoff's sample history (D12), for previews only (and later a debug-only
 * "Load sample data" action). Never real data.
 */
object SampleData {
    /** "Today" in the design: Sun 8 Nov 2026, cycle day 9. */
    val today: LocalDate = LocalDate.of(2026, 11, 8)

    /** Period starts 9 Aug, 7 Sep, 3 Oct and 31 Oct, five days each. */
    val periods: List<Period> = listOf(
        Period(LocalDate.of(2026, 8, 9), LocalDate.of(2026, 8, 13)),
        Period(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 11)),
        Period(LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 7)),
        Period(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 4)),
    )
}
