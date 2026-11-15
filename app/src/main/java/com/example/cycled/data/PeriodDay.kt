package com.example.cycled.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "period_days")
data class PeriodDay(
    @PrimaryKey val date: LocalDate,
    val flow: FlowLevel,
    /**
     * When this day was last saved, in epoch milliseconds. Set by the repository on
     * every save or update. Rows from before schema version 2 have 0.
     */
    @ColumnInfo(defaultValue = "0") val loggedAt: Long = 0L,
)
