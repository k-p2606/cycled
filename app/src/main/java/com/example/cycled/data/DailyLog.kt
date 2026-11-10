package com.example.cycled.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "daily_logs")
data class DailyLog(
    @PrimaryKey val date: LocalDate,
    val symptoms: Set<Symptom> = emptySet(),
    val mood: Mood? = null,
    val note: String? = null,
)
