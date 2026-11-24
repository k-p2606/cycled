package com.example.cycled.ui

import com.example.cycled.domain.Phase

/** Display name for a phase, e.g. "Follicular". Shared by Calendar and Home. */
val Phase.displayName: String
    get() = when (this) {
        Phase.MENSTRUAL -> "Menstrual"
        Phase.FOLLICULAR -> "Follicular"
        Phase.OVULATORY -> "Ovulatory"
        Phase.LUTEAL -> "Luteal"
    }

/** The phase as a tide (handoff D2), e.g. "Flood tide". */
val Phase.tideName: String
    get() = when (this) {
        Phase.MENSTRUAL -> "Low water"
        Phase.FOLLICULAR -> "Flood tide"
        Phase.OVULATORY -> "High water"
        Phase.LUTEAL -> "Ebb tide"
    }

/** Tide name with no phase: on or after the predicted start, or late (D2). */
const val SLACK_WATER = "Slack water"

/** Short label under the tide drawing, e.g. "Period", "Ovulation". */
val Phase.tideLabel: String
    get() = when (this) {
        Phase.MENSTRUAL -> "Period"
        Phase.FOLLICULAR -> "Follicular"
        Phase.OVULATORY -> "Ovulation"
        Phase.LUTEAL -> "Luteal"
    }
