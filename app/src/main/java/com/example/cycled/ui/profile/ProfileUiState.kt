package com.example.cycled.ui.profile

import com.example.cycled.data.Settings
import com.example.cycled.domain.Period
import java.time.LocalDate
import java.util.Locale

/** Everything Profile shows (D9). */
data class ProfileUiState(
    /** The name, or "You" when none is set. */
    val displayName: String,
    /** First letter of [displayName], upper case ("Y" by default). */
    val initial: String,
    /** Completed cycles: period starts on or before today, minus the current one. */
    val cyclesLogged: Int,
    val settings: Settings,
) {
    val canDecreaseCycle: Boolean get() = settings.avgCycle > Settings.AVG_CYCLE_RANGE.first
    val canIncreaseCycle: Boolean get() = settings.avgCycle < Settings.AVG_CYCLE_RANGE.last
    val canDecreasePeriod: Boolean get() = settings.avgPeriod > Settings.AVG_PERIOD_RANGE.first
    val canIncreasePeriod: Boolean get() = settings.avgPeriod < Settings.AVG_PERIOD_RANGE.last

    /** "1 cycle logged", "3 cycles logged". */
    val cyclesLine: String get() = if (cyclesLogged == 1) "1 cycle logged" else "$cyclesLogged cycles logged"
}

/** Builds Profile from the settings and the periods from groupIntoPeriods(). */
fun buildProfileState(settings: Settings, periods: List<Period>, today: LocalDate): ProfileUiState {
    val name = settings.name.trim().ifEmpty { "You" }
    val starts = periods.map { it.start }.filter { !it.isAfter(today) }.distinct()
    return ProfileUiState(
        displayName = name,
        initial = name.first().uppercase(Locale.getDefault()),
        cyclesLogged = (starts.size - 1).coerceAtLeast(0),
        settings = settings,
    )
}
