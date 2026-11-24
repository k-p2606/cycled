package com.example.cycled.ui

import com.example.cycled.data.DailyLog
import com.example.cycled.data.Mood
import com.example.cycled.data.Symptom
import com.example.cycled.domain.AverageCyclePredictor
import com.example.cycled.domain.Phase
import com.example.cycled.domain.RecCategory
import com.example.cycled.domain.groupIntoPeriods
import com.example.cycled.ui.home.HomeFormat
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Home's "For today" tips from buildHomeState(), with the handoff's sample history. */
class HomeRecommendationsTest {

    private val predictor = AverageCyclePredictor()

    private fun home(today: LocalDate, logs: Map<LocalDate, DailyLog> = SampleData.dailyLogs): HomeUiState.Ready =
        buildHomeState(
            periods = groupIntoPeriods(SampleData.periodDays.keys.filter { !it.isAfter(today) }),
            today = today,
            predictor = predictor,
            dailyLogs = logs,
        ) as HomeUiState.Ready

    private fun lowMood(today: LocalDate, vararg daysBack: Long) =
        daysBack.associate { today.minusDays(it) to DailyLog(today.minusDays(it), mood = Mood.LOW) }

    private val HomeUiState.Ready.topIds get() = recommendations.map { it.id }
    private val HomeUiState.Ready.allIds get() = allRecommendations.map { it.id }

    @Test
    fun ready_follicularDay9_givesPhaseTips() {
        val state = home(SampleData.today)

        assertEquals(Phase.FOLLICULAR, state.currentPhase)
        assertEquals(listOf("follicular-body", "follicular-fitness", "follicular-health"), state.topIds)
        assertEquals(
            listOf("follicular-body", "follicular-fitness", "follicular-health", "follicular-mind", "follicular-food"),
            state.allIds,
        )
        assertEquals(RecCategory.entries, state.allRecommendations.map { it.category })
    }

    @Test
    fun ready_menstrualWithCrampsAndTired_putsSymptomTipsFirst() {
        val today = LocalDate.of(2026, 11, 2)
        val logs = SampleData.dailyLogs + (today to DailyLog(today, symptoms = setOf(Symptom.CRAMPS, Symptom.FATIGUE)))
        val state = home(today, logs)

        assertEquals(Phase.MENSTRUAL, state.currentPhase)
        assertEquals(listOf("symptom-cramps", "symptom-tired", "menstrual-health"), state.topIds)
        assertEquals(
            listOf("symptom-cramps", "symptom-tired", "menstrual-health", "menstrual-mind", "menstrual-food"),
            state.allIds,
        )
    }

    @Test
    fun ready_symptomsOlderThanYesterday_dontCount() {
        val today = SampleData.today
        val logs = mapOf(today.minusDays(2) to DailyLog(today.minusDays(2), symptoms = setOf(Symptom.CRAMPS)))
        assertEquals("follicular-body", home(today, logs).allRecommendations.first().id)
    }

    @Test
    fun late_givesTheLateTipFirstAndGeneralTips() {
        val state = home(LocalDate.of(2026, 12, 2))

        assertTrue(state.isLate)
        assertEquals(listOf("late-period", "general-body", "general-fitness"), state.topIds)
        assertEquals(
            listOf("general-body", "general-fitness", "general-health", "general-mind", "general-food"),
            state.allIds,
        )
        assertEquals("late-period", state.lateTip?.id)
    }

    @Test
    fun notLate_hasNoLateTip() {
        assertEquals(null, home(SampleData.today).lateTip)
    }

    @Test
    fun lowMoodThreeOfSevenDays_addsTheNudge_twoDoesNot() {
        val today = SampleData.today
        // Days 2+ back, so no low mood counts as "recent" (that would bring in the Low mood tip).
        assertEquals("low-mood-nudge", home(today, lowMood(today, 2, 4, 6)).topIds.first())
        assertEquals("follicular-mind", home(today, lowMood(today, 2, 4)).allRecommendations[3].id)
        // 7 days back is outside the window.
        assertEquals("follicular-mind", home(today, lowMood(today, 2, 4, 7)).allRecommendations[3].id)
    }

    @Test
    fun noData_hasNoTips() {
        assertEquals(HomeUiState.NoData, buildHomeState(emptyList(), SampleData.today, predictor, SampleData.dailyLogs))
    }

    @Test
    fun tipSpoken() {
        val tip = home(SampleData.today).recommendations.first()
        assertEquals("Body tip: ${tip.title}. ${tip.body}", HomeFormat.tipSpoken(tip))
    }
}
