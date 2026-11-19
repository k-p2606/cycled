package com.example.cycled.domain

/**
 * Population cycle values, kept in one place. Cycle and period lengths are the medians
 * of the train people in the Marquette dataset (predictions/model.py, session 11), used
 * until the user has logged enough of their own data.
 */
object CycleDefaults {
    const val CYCLE_LENGTH_DAYS = 29
    const val PERIOD_LENGTH_DAYS = 5
    // Marquette median luteal 13 + 1 (day 1 = first day of bleeding; see session 11b).
    const val DAYS_FROM_OVULATION_TO_NEXT_START = 14
    const val MIN_VALID_CYCLE_DAYS = 15
    const val MAX_VALID_CYCLE_DAYS = 60
    const val MAX_CYCLES_USED = 6

    /** Smallest ± range around a prediction once the range comes from the standard deviation. */
    const val MIN_RANGE_HALF_WIDTH_DAYS = 3
}
