package com.example.cycled.ui

import com.example.cycled.domain.Phase

/** Display name for a phase, e.g. "Follicular". Shared by Calendar and (from step 3) Home. */
val Phase.displayName: String
    get() = when (this) {
        Phase.MENSTRUAL -> "Menstrual"
        Phase.FOLLICULAR -> "Follicular"
        Phase.OVULATORY -> "Ovulatory"
        Phase.LUTEAL -> "Luteal"
    }
