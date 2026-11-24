package com.example.cycled.ui

import com.example.cycled.data.FlowChoice
import com.example.cycled.data.FlowLevel
import com.example.cycled.data.Mood
import com.example.cycled.data.Symptom

/*
 * Chip labels and the chips' order, from the handoff's Calendar log panel. The design's
 * Tired, Tender and Bright are the existing FATIGUE, BREAST_TENDERNESS and HAPPY values
 * (no enum changes; stored names stay the same). Values without a chip (back pain,
 * nausea, cravings, anxious) stay valid in old rows; they just have no chip.
 */

val FlowChoice.label: String
    get() = when (this) {
        FlowChoice.NONE -> "None"
        FlowChoice.LIGHT -> "Light"
        FlowChoice.MEDIUM -> "Medium"
        FlowChoice.HEAVY -> "Heavy"
    }

val FlowLevel.label: String
    get() = when (this) {
        FlowLevel.SPOTTING -> "Spotting"
        FlowLevel.LIGHT -> "Light"
        FlowLevel.MEDIUM -> "Medium"
        FlowLevel.HEAVY -> "Heavy"
    }

/** Symptom chips, in the design's order. */
val SymptomChips: List<Symptom> = listOf(
    Symptom.CRAMPS,
    Symptom.HEADACHE,
    Symptom.BLOATING,
    Symptom.FATIGUE,
    Symptom.BREAST_TENDERNESS,
    Symptom.ACNE,
)

/** Mood chips, in the design's order. */
val MoodChips: List<Mood> = listOf(Mood.CALM, Mood.LOW, Mood.IRRITABLE, Mood.HAPPY)

val Symptom.label: String
    get() = when (this) {
        Symptom.CRAMPS -> "Cramps"
        Symptom.HEADACHE -> "Headache"
        Symptom.BLOATING -> "Bloating"
        Symptom.FATIGUE -> "Tired"
        Symptom.BACK_PAIN -> "Back pain"
        Symptom.NAUSEA -> "Nausea"
        Symptom.ACNE -> "Acne"
        Symptom.BREAST_TENDERNESS -> "Tender"
        Symptom.CRAVINGS -> "Cravings"
    }

val Mood.label: String
    get() = when (this) {
        Mood.HAPPY -> "Bright"
        Mood.CALM -> "Calm"
        Mood.IRRITABLE -> "Irritable"
        Mood.ANXIOUS -> "Anxious"
        Mood.LOW -> "Low"
    }
