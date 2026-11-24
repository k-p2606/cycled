package com.example.cycled.domain

import com.example.cycled.data.Mood
import com.example.cycled.data.Symptom
import java.time.LocalDate

/** Tip categories, in display order. The order also breaks ties in [RecResult.top3]. */
enum class RecCategory { BODY, FITNESS, HEALTH, MIND, FOOD }

/** How a tip gets chosen, besides matching phases, symptoms or moods. */
enum class RecRule {
    /** Chosen by its [Recommendation.phases], [Recommendation.symptoms] or [Recommendation.moods]. */
    TARGETED,

    /** Fills its category when there is no phase (late, no data, predicted start day). */
    GENERAL,

    /** Added when the period is late. Extra: it doesn't take a category slot. */
    LATE,

    /** Takes the MIND slot after 3 or more low-mood days in the last 7. */
    LOW_MOOD,
}

data class Recommendation(
    val id: String,
    val category: RecCategory,
    val title: String,
    val body: String,
    val phases: Set<Phase> = emptySet(),
    val symptoms: Set<Symptom> = emptySet(),
    val moods: Set<Mood> = emptySet(),
    /** Higher wins within a category; ties go to the smaller id. */
    val priority: Int = 0,
    val rule: RecRule = RecRule.TARGETED,
)

data class RecContext(
    /** Null when late, with no data, or on the predicted start day. */
    val phase: Phase?,
    val isLate: Boolean,
    /** Symptoms logged today or yesterday. */
    val recentSymptoms: Set<Symptom>,
    /** Today's mood, else yesterday's. */
    val recentMood: Mood?,
    /** Days in the last 7 (today included) with mood [Mood.LOW]. */
    val lowMoodDaysLast7: Int,
    val today: LocalDate,
)

data class RecResult(
    /** Up to three tips to show first. */
    val top3: List<Recommendation>,
    /** At most one tip per category, in [RecCategory] order. */
    val byCategory: Map<RecCategory, Recommendation>,
    /** The late-period tip, when late; not part of [byCategory]. */
    val lateTip: Recommendation?,
)

/** Low-mood days in the last 7 from which the MIND slot suggests talking to someone. */
const val LOW_MOOD_NUDGE_DAYS = 3

/**
 * Builds a [RecContext] from logged notes. "Recent" means logged today or yesterday;
 * a mood logged today wins over yesterday's.
 */
fun recContext(phase: Phase?, isLate: Boolean, notes: Map<LocalDate, DayNotes>, today: LocalDate): RecContext {
    val recent = listOfNotNull(notes[today], notes[today.minusDays(1)])
    return RecContext(
        phase = phase,
        isLate = isLate,
        recentSymptoms = recent.flatMap { it.symptoms }.toSet(),
        recentMood = recent.firstNotNullOfOrNull { it.mood },
        lowMoodDaysLast7 = (0L..6L).count { notes[today.minusDays(it)]?.mood == Mood.LOW },
        today = today,
    )
}

/**
 * Picks tips for [ctx] from [catalog]. Deterministic: the same context and catalog
 * always give the same result.
 *
 * Per category, the first of these that has a tip wins:
 * 1. MIND only: the low-mood nudge, after [LOW_MOOD_NUDGE_DAYS] low days in the last 7.
 * 2. A tip matching a recent symptom or mood.
 * 3. The current phase's tip.
 * 4. A general tip (always used when there is no phase, and as a fallback).
 * Within a step the highest priority wins, then the smallest id.
 *
 * When late, the late tip is added on top of the five. [RecResult.top3] puts symptom
 * and mood matches first, then the low-mood or late tip, then by priority, then by
 * category order.
 */
fun recommendationsFor(ctx: RecContext, catalog: List<Recommendation>): RecResult {
    val lowMood = ctx.lowMoodDaysLast7 >= LOW_MOOD_NUDGE_DAYS

    val byCategory = linkedMapOf<RecCategory, Recommendation>()
    for (category in RecCategory.entries) {
        val inCategory = catalog.filter { it.category == category }
        val targeted = inCategory.filter { it.rule == RecRule.TARGETED }
        val pick = (if (lowMood && category == RecCategory.MIND) best(inCategory.filter { it.rule == RecRule.LOW_MOOD }) else null)
            ?: best(targeted.filter { it.matches(ctx) })
            ?: ctx.phase?.let { phase -> best(targeted.filter { phase in it.phases && !it.isSymptomOrMoodTip }) }
            ?: best(inCategory.filter { it.rule == RecRule.GENERAL })
        if (pick != null) byCategory[category] = pick
    }

    val lateTip = if (ctx.isLate) best(catalog.filter { it.rule == RecRule.LATE }) else null

    val top3 = (byCategory.values + listOfNotNull(lateTip))
        .sortedWith(
            compareBy<Recommendation> { if (it.matches(ctx)) 0 else 1 }
                .thenBy { if (it.rule == RecRule.LOW_MOOD || it.rule == RecRule.LATE) 0 else 1 }
                .thenByDescending { it.priority }
                .thenBy { it.category.ordinal }
                .thenBy { it.id },
        )
        .take(3)

    return RecResult(top3 = top3, byCategory = byCategory, lateTip = lateTip)
}

private val Recommendation.isSymptomOrMoodTip: Boolean get() = symptoms.isNotEmpty() || moods.isNotEmpty()

/** True for a targeted tip matching a recent symptom or mood. */
private fun Recommendation.matches(ctx: RecContext): Boolean =
    rule == RecRule.TARGETED &&
        (symptoms.any { it in ctx.recentSymptoms } || (ctx.recentMood != null && ctx.recentMood in moods))

private fun best(tips: List<Recommendation>): Recommendation? =
    tips.minWithOrNull(compareByDescending<Recommendation> { it.priority }.thenBy { it.id })
