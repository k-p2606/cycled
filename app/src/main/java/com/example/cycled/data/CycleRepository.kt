package com.example.cycled.data

import com.example.cycled.domain.Period
import com.example.cycled.domain.groupIntoPeriods
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The app's single access point to cycle data.
 *
 * Screens and ViewModels should use this repository instead of talking to the
 * DAOs directly.
 */
class CycleRepository(
    private val periodDayDao: PeriodDayDao,
    private val dailyLogDao: DailyLogDao,
) {
    fun observePeriodDays(): Flow<List<PeriodDay>> = periodDayDao.observeAll()

    fun observePeriods(): Flow<List<Period>> =
        periodDayDao.observeAll().map { days -> groupIntoPeriods(days.map { it.date }) }

    suspend fun logPeriodDay(date: LocalDate, flow: FlowLevel) =
        periodDayDao.upsert(PeriodDay(date, flow))

    suspend fun removePeriodDay(date: LocalDate) = periodDayDao.delete(date)

    fun observeLog(date: LocalDate): Flow<DailyLog?> = dailyLogDao.observe(date)

    fun observeLogs(from: LocalDate, to: LocalDate): Flow<List<DailyLog>> =
        dailyLogDao.observeRange(from, to)

    suspend fun saveLog(log: DailyLog) = dailyLogDao.upsert(log)

    suspend fun removeLog(date: LocalDate) = dailyLogDao.delete(date)
}
