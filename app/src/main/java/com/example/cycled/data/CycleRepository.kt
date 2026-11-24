package com.example.cycled.data

import com.example.cycled.domain.Period
import com.example.cycled.domain.groupIntoPeriods
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The app's single access point to cycle data.
 *
 * Screens and ViewModels should use this repository instead of talking to the
 * DAOs directly. [now] is the clock used to stamp when a period day was saved.
 * [inTransaction] runs a block in one database transaction; the app passes Room's
 * withTransaction, tests can run the block directly.
 */
class CycleRepository(
    private val periodDayDao: PeriodDayDao,
    private val dailyLogDao: DailyLogDao,
    private val now: () -> Instant = { Instant.now() },
    private val inTransaction: suspend (suspend () -> Unit) -> Unit = { it() },
) {
    /** Serialises the read-modify-write edits below, so quick taps can't lose an update. */
    private val editLock = Mutex()

    /** All logged period days in date order. */
    fun observePeriodDays(): Flow<List<PeriodDay>> = periodDayDao.observeAll()

    fun observePeriods(): Flow<List<Period>> =
        periodDayDao.observeAll().map { days -> groupIntoPeriods(days.map { it.date }) }

    /** Saves or updates [date]; either way it counts as logged now. */
    suspend fun logPeriodDay(date: LocalDate, flow: FlowLevel) =
        periodDayDao.upsert(PeriodDay(date, flow, loggedAt = now().toEpochMilli()))

    suspend fun removePeriodDay(date: LocalDate) = periodDayDao.delete(date)

    /** Every daily log (symptoms, mood, note, "no flow") in date order. */
    fun observeAllLogs(): Flow<List<DailyLog>> = dailyLogDao.observeAll()

    fun observeLog(date: LocalDate): Flow<DailyLog?> = dailyLogDao.observe(date)

    fun observeLogs(from: LocalDate, to: LocalDate): Flow<List<DailyLog>> =
        dailyLogDao.observeRange(from, to)

    suspend fun saveLog(log: DailyLog) = dailyLogDao.upsert(log)

    suspend fun removeLog(date: LocalDate) = dailyLogDao.delete(date)

    /**
     * Sets [date]'s flow from a Flow chip (D7):
     * - Light / Medium / Heavy: saves that PeriodDay (replacing any Spotting) and clears "no flow".
     * - None: deletes the PeriodDay and sets "no flow".
     * - null (the selected chip tapped again, or "Period logged" tapped): deletes the
     *   PeriodDay and clears "no flow".
     */
    suspend fun setFlow(date: LocalDate, choice: FlowChoice?) = editLock.withLock {
        val level = choice?.level
        if (level != null) logPeriodDay(date, level) else periodDayDao.delete(date)
        editLogLocked(date) { it.copy(noFlow = choice == FlowChoice.NONE) }
    }

    /** Adds [symptom] to [date]'s log, or removes it if it's already there. */
    suspend fun toggleSymptom(date: LocalDate, symptom: Symptom) = editLock.withLock {
        editLogLocked(date) {
            it.copy(symptoms = if (symptom in it.symptoms) it.symptoms - symptom else it.symptoms + symptom)
        }
    }

    /** Sets [date]'s mood; null clears it. */
    suspend fun setMood(date: LocalDate, mood: Mood?) = editLock.withLock {
        editLogLocked(date) { it.copy(mood = mood) }
    }

    /** Deletes every period day and daily log ("Delete all my data", D9). */
    suspend fun deleteAll() = editLock.withLock {
        inTransaction {
            periodDayDao.deleteAll()
            dailyLogDao.deleteAll()
        }
    }

    /** Replaces everything with [periodDays] and [logs] in one go (debug "Load sample data", D12). */
    suspend fun replaceAll(periodDays: Map<LocalDate, FlowLevel>, logs: Collection<DailyLog>) = editLock.withLock {
        val stamp = now().toEpochMilli()
        inTransaction {
            periodDayDao.deleteAll()
            dailyLogDao.deleteAll()
            periodDayDao.upsertAll(periodDays.map { (date, flow) -> PeriodDay(date, flow, loggedAt = stamp) })
            dailyLogDao.upsertAll(logs.toList())
        }
    }

    /** Applies [change] to [date]'s log (an empty one if missing); deletes the row once it's empty. */
    private suspend fun editLogLocked(date: LocalDate, change: (DailyLog) -> DailyLog) {
        val before = dailyLogDao.get(date)
        val after = change(before ?: DailyLog(date))
        when {
            after.isEmpty -> if (before != null) dailyLogDao.delete(date)
            after != before -> dailyLogDao.upsert(after)
        }
    }
}
