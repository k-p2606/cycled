package com.example.cycled.ui

/**
 * Messages shown on both Home and Calendar, kept in one place so the wording stays
 * the same. The app keeps its UI text in Kotlin rather than strings.xml.
 */
object CycleMessages {
    /** Home's "Next period" headline; Calendar's legend adds a full stop. */
    const val PERIOD_MAY_BE_LATE = "Your period may be late"

    /** [cycleLengthDays] is the predicted cycle length: next start minus the last period start. */
    private fun placeholderCycle(cycleLengthDays: Int) =
        "a typical $cycleLengthDays-day cycle until you've logged two periods"

    /** Home's "Next period" footnote when the prediction is not based on the user's cycles yet. */
    fun placeholderBasis(cycleLengthDays: Int) = "Based on ${placeholderCycle(cycleLengthDays)}"

    /** Calendar legend note when the prediction is not based on the user's cycles yet. */
    fun placeholderNote(cycleLengthDays: Int) = "Predictions use ${placeholderCycle(cycleLengthDays)}."

    /** Home's subline on the predicted start day itself (no phase, not late yet). */
    const val PERIOD_EXPECTED_TODAY = "Your period is expected today"

    /** Home's line under the coming-up rows, after "Likely <earliest> – <latest>. " (D4). */
    const val HOME_DISCLAIMER = "Estimates from your logged dates, not contraception or medical advice."
}
