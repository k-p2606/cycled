package com.example.cycled.domain

import com.example.cycled.data.Mood
import com.example.cycled.data.Symptom
import com.example.cycled.domain.RecCategory.BODY
import com.example.cycled.domain.RecCategory.FITNESS
import com.example.cycled.domain.RecCategory.FOOD
import com.example.cycled.domain.RecCategory.HEALTH
import com.example.cycled.domain.RecCategory.MIND

/*
 * DRAFT TEXT: every title and body below is a placeholder to be rewritten before release.
 *
 * Text rules: general wellbeing only; no medication names or doses; no hormone-boosting
 * or detox claims; no calorie, weight or diet numbers; nothing about contraception or
 * fertility planning. Phase names match Phase.displayName (Menstrual, Follicular,
 * Ovulatory, Luteal). Titles are at most 6 words; bodies 1-2 short sentences.
 */

private fun phase(id: String, category: RecCategory, phase: Phase, title: String, body: String) =
    Recommendation(id, category, title, body, phases = setOf(phase))

private fun symptom(id: String, category: RecCategory, symptom: Symptom, title: String, body: String, priority: Int = 1) =
    Recommendation(id, category, title, body, symptoms = setOf(symptom), priority = priority)

private fun mood(id: String, mood: Mood, title: String, body: String) =
    Recommendation(id, MIND, title, body, moods = setOf(mood), priority = 1)

private fun general(id: String, category: RecCategory, title: String, body: String) =
    Recommendation(id, category, title, body, rule = RecRule.GENERAL)

/** Every tip the app can show (35). */
val RecommendationCatalog: List<Recommendation> = listOf(
    // ---- Menstrual ----
    phase("menstrual-body", BODY, Phase.MENSTRUAL, "Keep warm and comfortable",
        "A warm layer or a hot water bottle can feel soothing. Wear whatever feels easy today."),
    phase("menstrual-fitness", FITNESS, Phase.MENSTRUAL, "Gentle movement counts",
        "A slow walk or light stretching is plenty. Rest is fine too."),
    phase("menstrual-health", HEALTH, Phase.MENSTRUAL, "Notice how your period feels",
        "Logging flow helps you spot what's usual for you. Mention anything unusual to a clinician."),
    phase("menstrual-mind", MIND, Phase.MENSTRUAL, "Lower the bar today",
        "It's okay to do less. Pick one small thing and let the rest wait."),
    phase("menstrual-food", FOOD, Phase.MENSTRUAL, "Warm, simple meals",
        "Soups, stews and warm drinks can be comforting. Eat regularly and keep water nearby."),

    // ---- Follicular ----
    phase("follicular-body", BODY, Phase.FOLLICULAR, "Try something new",
        "Many people feel fresher in the Follicular phase. A good time for a new routine."),
    phase("follicular-fitness", FITNESS, Phase.FOLLICULAR, "Build up your workouts",
        "If you feel up to it, add a little more pace or time. Listen to your body."),
    phase("follicular-health", HEALTH, Phase.FOLLICULAR, "Plan the week ahead",
        "Book that check-up or errand you've been putting off while energy feels steady."),
    phase("follicular-mind", MIND, Phase.FOLLICULAR, "Start a small project",
        "Curiosity often runs higher now. Jot down an idea and take the first step."),
    phase("follicular-food", FOOD, Phase.FOLLICULAR, "Colour on the plate",
        "Fresh vegetables, fruit and whole grains make easy, balanced meals."),

    // ---- Ovulatory ----
    phase("ovulatory-body", BODY, Phase.OVULATORY, "Notice your energy",
        "Some people feel their most energetic around now. Make room for things you enjoy."),
    phase("ovulatory-fitness", FITNESS, Phase.OVULATORY, "Enjoy an active day",
        "A longer walk, a class or a game with friends. Warm up and cool down well."),
    phase("ovulatory-health", HEALTH, Phase.OVULATORY, "Stay hydrated",
        "Keep a water bottle with you, especially if you're moving more."),
    phase("ovulatory-mind", MIND, Phase.OVULATORY, "Reach out to someone",
        "A good day to catch up with a friend or share plans with people you trust."),
    phase("ovulatory-food", FOOD, Phase.OVULATORY, "Light, fresh meals",
        "Salads, fruit and simple proteins can keep you going on busy days."),

    // ---- Luteal ----
    phase("luteal-body", BODY, Phase.LUTEAL, "Ease into evenings",
        "A warm shower or a few stretches can help you wind down."),
    phase("luteal-fitness", FITNESS, Phase.LUTEAL, "Steady, moderate movement",
        "Yoga, swimming or a brisk walk. Ease off if you feel tired."),
    phase("luteal-health", HEALTH, Phase.LUTEAL, "Protect your sleep",
        "Keep a regular bedtime and dim screens an hour before. Sleep can feel lighter now."),
    phase("luteal-mind", MIND, Phase.LUTEAL, "Be kind to yourself",
        "Feelings can run stronger in the Luteal phase. Give yourself extra space."),
    phase("luteal-food", FOOD, Phase.LUTEAL, "Regular meals and snacks",
        "Eating at steady times can help with cravings. Keep easy snacks to hand."),

    // ---- Symptoms (the design's six) ----
    symptom("symptom-cramps", BODY, Symptom.CRAMPS, "Heat for cramps", "Try a hot water bottle on your lower belly or back. Gentle stretches may help too.", priority = 3),
    symptom("symptom-tender", BODY, Symptom.BREAST_TENDERNESS, "Comfort for tenderness", "A soft, supportive bra and loose clothes may feel better.", priority = 2),
    symptom("symptom-acne", BODY, Symptom.ACNE, "Go gentle on skin", "Wash with a mild cleanser and avoid picking. Clean pillowcases help."),
    symptom("symptom-headache", HEALTH, Symptom.HEADACHE, "Rest and water for headaches", "Drink water and rest somewhere quiet. See a clinician if headaches are severe or new."),
    symptom("symptom-bloating", FOOD, Symptom.BLOATING, "Easy on bloating", "Smaller meals, eaten slowly, can feel easier. A short walk after eating may help."),
    symptom("symptom-tired", FITNESS, Symptom.FATIGUE, "Rest is training too", "Swap a hard workout for a walk or stretching. An early night can help."),

    // ---- Moods ----
    mood("mood-low", Mood.LOW, "Small steps on low days", "Get some daylight and do one small thing you enjoy. Low days pass."),
    mood("mood-irritable", Mood.IRRITABLE, "Give yourself some space", "A short walk or a few slow breaths can take the edge off."),

    // ---- Rules ----
    Recommendation("low-mood-nudge", MIND, "Talk to someone you trust",
        "You've logged a few low days. Talking to a friend, family member or clinician can help.",
        rule = RecRule.LOW_MOOD),
    Recommendation("late-period", HEALTH, "Your period may be late",
        "Periods can be a few days late. If it's more than about a week, consider a test or talk to a clinician.",
        rule = RecRule.LATE),

    // ---- General (no phase) ----
    general("general-body", BODY, "Check in with your body", "Notice what feels good today and what doesn't. Logging it helps over time."),
    general("general-fitness", FITNESS, "Move in a way you enjoy", "A walk, a dance in the kitchen or a stretch all count."),
    general("general-health", HEALTH, "Keep up the basics", "Drink water, get daylight and aim for regular sleep."),
    general("general-mind", MIND, "Take a calm minute", "Pause for a few slow breaths. It's a small reset you can do anywhere."),
    general("general-food", FOOD, "Eat regularly", "Regular meals with some variety help keep energy steady."),
)
