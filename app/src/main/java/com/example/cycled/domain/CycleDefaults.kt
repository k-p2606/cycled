package com.example.cycled.domain

/**
 * Population cycle values, kept in one place. Cycle, period and luteal lengths are
 * the medians of the train people in the Marquette dataset (predictions/model.py,
 * session 11), used until the user has logged enough of their own data.
 */
object CycleDefaults {
    const val CYCLE_LENGTH_DAYS = 29
    const val PERIOD_LENGTH_DAYS = 5
    const val LUTEAL_LENGTH_DAYS = 13
    const val MIN_VALID_CYCLE_DAYS = 15
    const val MAX_VALID_CYCLE_DAYS = 60
    const val MAX_CYCLES_USED = 6

    /** Smallest ± range around a prediction once the range comes from the standard deviation. */
    const val MIN_RANGE_HALF_WIDTH_DAYS = 3
}
