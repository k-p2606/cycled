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

    @Upsert
    suspend fun upsertAll(days: List<PeriodDay>)

    @Query("DELETE FROM period_days WHERE date = :date")
    suspend fun delete(date: LocalDate)

    @Query("DELETE FROM period_days")
    suspend fun deleteAll()

    @Query("SELECT * FROM period_days ORDER BY date")
    fun observeAll(): Flow<List<PeriodDay>>
}
