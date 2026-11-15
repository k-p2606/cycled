package com.example.cycled.ui

import com.example.cycled.domain.CycleDefaults

/**
 * Messages shown on both Home and Calendar, kept in one place so the wording stays
 * the same. The app keeps its UI text in Kotlin rather than strings.xml.
 */
object CycleMessages {
    /** Home's "Next period" headline; Calendar's legend adds a full stop. */
    const val PERIOD_MAY_BE_LATE = "Your period may be late"

    private const val PLACEHOLDER_CYCLE =
        "a typical ${CycleDefaults.CYCLE_LENGTH_DAYS}-day cycle until you've logged two periods"

    /** Home's "Next period" footnote when the prediction uses the placeholder length. */
    const val PLACEHOLDER_BASIS = "Based on $PLACEHOLDER_CYCLE"

    /** Calendar legend note when the prediction uses the placeholder length. */
    const val PLACEHOLDER_NOTE = "Predictions use $PLACEHOLDER_CYCLE."

    const val ESTIMATES_DISCLAIMER =
        "Estimates based on your logged dates. Not medical advice or contraception."
}
