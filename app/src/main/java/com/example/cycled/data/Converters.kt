package com.example.cycled.data

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun localDateToEpochDay(date: LocalDate): Long = date.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    @TypeConverter
    fun symptomsToString(symptoms: Set<Symptom>): String =
        symptoms.sortedBy { it.ordinal }.joinToString(",") { it.name }

    @TypeConverter
    fun stringToSymptoms(value: String): Set<Symptom> =
        if (value.isBlank()) {
            emptySet()
        } else {
            value.split(",").map { Symptom.valueOf(it.trim()) }.toSet()
        }
}
