package com.example.cycled.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "daily_logs")
data class DailyLog(
    @PrimaryKey val date: LocalDate,
    val symptoms: Set<Symptom> = emptySet(),
    val mood: Mood? = null,
    val note: String? = null,
    /**
     * "Checked, not bleeding": the Flow chip None (D7). Never true on a day that has a
     * PeriodDay. Rows from before schema version 3 have false.
     */
    @ColumnInfo(defaultValue = "0") val noFlow: Boolean = false,
) {
    /** Nothing left worth keeping: the row can be deleted. */
    val isEmpty: Boolean
        get() = symptoms.isEmpty() && mood == null && note.isNullOrEmpty() && !noFlow
}
