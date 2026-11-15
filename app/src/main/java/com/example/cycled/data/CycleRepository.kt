package com.example.cycled.data

import com.example.cycled.domain.Period
import com.example.cycled.domain.groupIntoPeriods
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The app's single access point to cycle data.
 *
 * Screens and ViewModels should use this repository instead of talking to the
 * DAOs directly. [now] is the clock used to stamp when a period day was saved.
 */
class CycleRepository(
    private val periodDayDao: PeriodDayDao,
    private val dailyLogDao: DailyLogDao,
    private val now: () -> Instant = { Instant.now() },
) {
    /** All logged period days in date order. */
    fun observePeriodDays(): Flow<List<PeriodDay>> = periodDayDao.observeAll()

    /** The [limit] most recently saved period days, newest save first. */
    fun observeRecentlyLoggedDays(limit: Int): Flow<List<PeriodDay>> =
        periodDayDao.observeRecentlyLogged(limit)

    fun observePeriods(): Flow<List<Period>> =
        periodDayDao.observeAll().map { days -> groupIntoPeriods(days.map { it.date }) }

    /** Saves or updates [date]; either way it counts as logged now. */
    suspend fun logPeriodDay(date: LocalDate, flow: FlowLevel) =
        periodDayDao.upsert(PeriodDay(date, flow, loggedAt = now().toEpochMilli()))

    suspend fun removePeriodDay(date: LocalDate) = periodDayDao.delete(date)

    fun observeLog(date: LocalDate): Flow<DailyLog?> = dailyLogDao.observe(date)

    fun observeLogs(from: LocalDate, to: LocalDate): Flow<List<DailyLog>> =
        dailyLogDao.observeRange(from, to)

    suspend fun saveLog(log: DailyLog) = dailyLogDao.upsert(log)

    suspend fun removeLog(date: LocalDate) = dailyLogDao.delete(date)
}
