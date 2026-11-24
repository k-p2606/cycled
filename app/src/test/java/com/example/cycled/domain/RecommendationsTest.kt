package com.example.cycled.domain

import com.example.cycled.data.Mood
import com.example.cycled.data.Symptom
import com.example.cycled.ui.displayName
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationsTest {

    private val today: LocalDate = LocalDate.of(2026, 11, 8)
    private val catalog = RecommendationCatalog
    private val byId = catalog.associateBy { it.id }

    private fun ctx(
        phase: Phase? = Phase.FOLLICULAR,
        isLate: Boolean = false,
        symptoms: Set<Symptom> = emptySet(),
        mood: Mood? = null,
        lowMoodDays: Int = 0,
    ) = RecContext(phase, isLate, symptoms, mood, lowMoodDays, today)

    private fun recs(ctx: RecContext) = recommendationsFor(ctx, catalog)

    private fun ids(result: RecResult) = result.byCategory.mapValues { it.value.id }

    private fun phaseTips(phase: Phase) = RecCategory.entries.associateWith { "${phase.name.lowercase()}-${it.name.lowercase()}" }

    private val generalTips = RecCategory.entries.associateWith { "general-${it.name.lowercase()}" }

    // ---- Phases ----

    @Test
    fun everyPhase_givesItsOwnTipInEveryCategory() {
        for (phase in Phase.entries) {
            val result = recs(ctx(phase))
            assertEquals(RecCategory.entries, result.byCategory.keys.toList())
            assertEquals(phaseTips(phase), ids(result))
            result.byCategory.forEach { (category, tip) -> assertEquals(category, tip.category) }
            assertNull(result.lateTip)
        }
    }

    // ---- Symptoms and moods ----

    @Test
    fun symptom_beatsOnlyItsOwnCategorysPhaseTip() {
        for (phase in Phase.entries) {
            for (tip in catalog.filter { it.symptoms.isNotEmpty() || it.moods.isNotEmpty() }) {
                val result = recs(ctx(phase, symptoms = tip.symptoms, mood = tip.moods.firstOrNull()))
                val expected = phaseTips(phase) + (tip.category to tip.id)
                assertEquals("${tip.id} in $phase", expected, ids(result))
            }
        }
    }

    @Test
    fun severalSymptomsInOneCategory_highestPriorityWins() {
        val result = recs(ctx(symptoms = setOf(Symptom.ACNE, Symptom.BREAST_TENDERNESS, Symptom.CRAMPS)))
        assertEquals("symptom-cramps", result.byCategory.getValue(RecCategory.BODY).id)
    }

    @Test
    fun symptomsAcrossCategories_eachTakesItsOwnSlot() {
        val result = recs(ctx(symptoms = setOf(Symptom.CRAMPS, Symptom.HEADACHE, Symptom.BLOATING)))
        assertEquals(
            phaseTips(Phase.FOLLICULAR) + mapOf(
                RecCategory.BODY to "symptom-cramps",
                RecCategory.HEALTH to "symptom-headache",
                RecCategory.FOOD to "symptom-bloating",
            ),
            ids(result),
        )
        assertEquals(setOf("symptom-cramps", "symptom-headache", "symptom-bloating"), result.top3.map { it.id }.toSet())
    }

    @Test
    fun symptomWithNoTip_changesNothing() {
        assertEquals(phaseTips(Phase.LUTEAL), ids(recs(ctx(Phase.LUTEAL, symptoms = setOf(Symptom.NAUSEA), mood = Mood.CALM))))
    }

    @Test
    fun neverTwoTipsInOneCategory() {
        val contexts = Phase.entries.map<Phase, Phase?> { it }.plus(null).flatMap { phase ->
            listOf(
                ctx(phase, symptoms = Symptom.entries.toSet(), mood = Mood.LOW, lowMoodDays = 7, isLate = phase == null),
                ctx(phase, symptoms = Symptom.entries.toSet(), mood = Mood.IRRITABLE),
                ctx(phase),
            )
        }
        for (c in contexts) {
            val result = recs(c)
            result.byCategory.forEach { (category, tip) -> assertEquals(category, tip.category) }
            val top = result.top3.filter { it.rule != RecRule.LATE }.map { it.category }
            assertEquals(top.distinct(), top)
            assertTrue(result.top3.size <= 3)
        }
    }

    // ---- No phase and late ----

    @Test
    fun noPhase_givesFiveGeneralTips() {
        val result = recs(ctx(phase = null))
        assertEquals(generalTips, ids(result))
        assertNull(result.lateTip)
    }

    @Test
    fun late_givesFiveGeneralTipsPlusTheLateTip() {
        val result = recs(ctx(phase = null, isLate = true))
        assertEquals(generalTips, ids(result))
        assertEquals("late-period", result.lateTip?.id)
        assertEquals("late-period", result.top3.first().id)
    }

    @Test
    fun noPhase_symptomStillBeatsTheGeneralTip() {
        val result = recs(ctx(phase = null, isLate = true, symptoms = setOf(Symptom.CRAMPS)))
        assertEquals(generalTips + (RecCategory.BODY to "symptom-cramps"), ids(result))
        assertEquals(listOf("symptom-cramps", "late-period"), result.top3.take(2).map { it.id })
    }

    // ---- Low mood ----

    @Test
    fun lowMoodThreeOfSeven_addsTheNudgeInTheMindSlot() {
        val result = recs(ctx(lowMoodDays = 3))
        assertEquals(phaseTips(Phase.FOLLICULAR) + (RecCategory.MIND to "low-mood-nudge"), ids(result))
        assertEquals("low-mood-nudge", result.top3.first().id)
    }

    @Test
    fun lowMoodTwoOfSeven_doesNot() {
        val result = recs(ctx(lowMoodDays = 2))
        assertEquals(phaseTips(Phase.FOLLICULAR), ids(result))
        assertFalse(result.top3.any { it.id == "low-mood-nudge" })
    }

    @Test
    fun nudge_beatsTheLowMoodTip() {
        val result = recs(ctx(mood = Mood.LOW, lowMoodDays = 4))
        assertEquals("low-mood-nudge", result.byCategory.getValue(RecCategory.MIND).id)
    }

    @Test
    fun top3_matchesThenNudgeOrLateThenCategoryOrder() {
        val result = recs(ctx(phase = null, isLate = true, symptoms = setOf(Symptom.BLOATING), lowMoodDays = 3))
        assertEquals(listOf("symptom-bloating", "late-period", "low-mood-nudge"), result.top3.map { it.id })

        // No matches or rules: equal priority, so category order.
        assertEquals(listOf("luteal-body", "luteal-fitness", "luteal-health"), recs(ctx(Phase.LUTEAL)).top3.map { it.id })
    }

    // ---- Determinism ----

    @Test
    fun sameContext_sameResult_whateverTheCatalogOrder() {
        val c = ctx(Phase.MENSTRUAL, symptoms = setOf(Symptom.CRAMPS, Symptom.ACNE, Symptom.HEADACHE), mood = Mood.IRRITABLE)
        val first = recs(c)
        repeat(5) { assertEquals(first, recs(c)) }
        assertEquals(first, recommendationsFor(c, catalog.reversed()))
        assertEquals(first, recommendationsFor(c, catalog.shuffled(kotlin.random.Random(7))))
    }

    @Test
    fun priorityTie_goesToTheSmallerId() {
        val a = Recommendation("b-tip", RecCategory.BODY, "B", "B.", symptoms = setOf(Symptom.CRAMPS))
        val b = Recommendation("a-tip", RecCategory.BODY, "A", "A.", symptoms = setOf(Symptom.CRAMPS))
        assertEquals("a-tip", recommendationsFor(ctx(symptoms = setOf(Symptom.CRAMPS)), listOf(a, b)).byCategory.getValue(RecCategory.BODY).id)
    }

    // ---- Context from logged notes ----

    @Test
    fun recContext_recentIsTodayOrYesterday() {
        val notes = mapOf(
            today to DayNotes(symptoms = setOf(Symptom.CRAMPS)),
            today.minusDays(1) to DayNotes(symptoms = setOf(Symptom.HEADACHE), mood = Mood.IRRITABLE),
            today.minusDays(2) to DayNotes(symptoms = setOf(Symptom.ACNE), mood = Mood.LOW),
        )
        val c = recContext(Phase.LUTEAL, isLate = false, notes, today)
        assertEquals(setOf(Symptom.CRAMPS, Symptom.HEADACHE), c.recentSymptoms)
        assertEquals(Mood.IRRITABLE, c.recentMood)
        assertEquals(1, c.lowMoodDaysLast7)
    }

    @Test
    fun recContext_todaysMoodWins_andLowDaysCountTheLastSeven() {
        val notes = (0L..9L).associate { today.minusDays(it) to DayNotes(mood = Mood.LOW) } +
            (today to DayNotes(mood = Mood.CALM))
        val c = recContext(null, isLate = true, notes, today)
        assertEquals(Mood.CALM, c.recentMood)
        assertEquals(6, c.lowMoodDaysLast7)   // days 1-6 back; today is calm, days 7-9 are too old
    }

    // ---- Catalog ----

    @Test
    fun catalog_isValid() {
        assertEquals(catalog.size, catalog.map { it.id }.toSet().size)
        for (tip in catalog) {
            assertTrue(tip.id, tip.title.isNotBlank() && tip.body.isNotBlank())
            assertTrue("${tip.id}: \"${tip.title}\"", tip.title.trim().split(Regex("\\s+")).size <= 6)
        }
        // One per phase per category, one per design symptom, Low and Irritable, plus the rules.
        for (phase in Phase.entries) for (category in RecCategory.entries) {
            assertEquals(1, catalog.count { phase in it.phases && it.category == category })
        }
        val designSymptoms = setOf(Symptom.CRAMPS, Symptom.HEADACHE, Symptom.BLOATING, Symptom.FATIGUE, Symptom.BREAST_TENDERNESS, Symptom.ACNE)
        assertEquals(designSymptoms, catalog.flatMap { it.symptoms }.toSet())
        assertEquals(setOf(Mood.LOW, Mood.IRRITABLE), catalog.flatMap { it.moods }.toSet())
        assertEquals(RecCategory.entries.toSet(), catalog.filter { it.rule == RecRule.GENERAL }.map { it.category }.toSet())
        assertEquals(1, catalog.count { it.rule == RecRule.LATE })
        assertEquals(RecCategory.MIND, catalog.single { it.rule == RecRule.LOW_MOOD }.category)
    }

    @Test
    fun catalog_bodiesAreAtMost110Characters() {
        for (tip in catalog) {
            assertTrue("${tip.id} body is ${tip.body.length} characters", tip.body.length <= 110)
        }
    }

    @Test
    fun catalog_everyEntryIsReachable() {
        for (tip in catalog) {
            val c = when (tip.rule) {
                RecRule.GENERAL -> ctx(phase = null)
                RecRule.LATE -> ctx(phase = null, isLate = true)
                RecRule.LOW_MOOD -> ctx(lowMoodDays = LOW_MOOD_NUDGE_DAYS)
                RecRule.TARGETED -> ctx(
                    phase = tip.phases.firstOrNull() ?: Phase.FOLLICULAR,
                    symptoms = tip.symptoms,
                    mood = tip.moods.firstOrNull(),
                )
            }
            val result = recs(c)
            assertTrue("${tip.id} is never chosen", tip in result.byCategory.values || tip == result.lateTip)
        }
    }

    @Test
    fun catalog_textAvoidsBannedTopics() {
        // Word starts, so "weight" also catches "weights"; "mg" and "pill" must be whole words ("pillowcases" is fine).
        val banned = listOf(
            "ibuprofen", "paracetamol", "acetaminophen", "aspirin", "naproxen", "mg", "dose", "pill",
            "detox", "boost", "hormone", "calorie", "kcal", "weight", "diet",
            "contracept", "fertil", "conceive", "pregnan",
        )
        val wholeWords = setOf("mg", "pill")
        for (tip in catalog) {
            val text = "${tip.title} ${tip.body}".lowercase()
            for (word in banned) {
                val pattern = if (word in wholeWords) Regex("\\b${word}s?\\b") else Regex("\\b$word")
                assertFalse("${tip.id} mentions \"$word\"", pattern.containsMatchIn(text))
            }
        }
    }

    @Test
    fun catalog_phaseNamesMatchDisplayNames() {
        for (tip in catalog) for (phase in Phase.entries) {
            val name = phase.displayName
            val text = "${tip.title} ${tip.body}"
            if (text.contains(name, ignoreCase = true)) assertTrue("${tip.id} writes $phase as \"$name\"", text.contains("$name phase"))
        }
    }
}
