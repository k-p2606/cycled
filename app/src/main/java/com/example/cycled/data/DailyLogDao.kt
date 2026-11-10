package com.example.cycled.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLogDao {
    @Upsert
    suspend fun upsert(log: DailyLog)

    @Query("DELETE FROM daily_logs WHERE date = :date")
    suspend fun delete(date: LocalDate)

    @Query("SELECT * FROM daily_logs WHERE date = :date")
    fun observe(date: LocalDate): Flow<DailyLog?>

    @Query("SELECT * FROM daily_logs WHERE date BETWEEN :from AND :to ORDER BY date")
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<DailyLog>>
}
