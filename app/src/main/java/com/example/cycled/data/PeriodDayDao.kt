package com.example.cycled.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodDayDao {
    @Upsert
    suspend fun upsert(day: PeriodDay)

    @Query("DELETE FROM period_days WHERE date = :date")
    suspend fun delete(date: LocalDate)

    @Query("SELECT * FROM period_days ORDER BY date")
    fun observeAll(): Flow<List<PeriodDay>>

    /** The [limit] most recently saved days, newest save first; ties fall back to the later date. */
    @Query("SELECT * FROM period_days ORDER BY loggedAt DESC, date DESC LIMIT :limit")
    fun observeRecentlyLogged(limit: Int): Flow<List<PeriodDay>>
}
