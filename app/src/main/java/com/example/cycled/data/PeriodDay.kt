package com.example.cycled.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "period_days")
data class PeriodDay(
    @PrimaryKey val date: LocalDate,
    val flow: FlowLevel,
)
