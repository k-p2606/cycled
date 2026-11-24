package com.example.cycled.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [PeriodDay::class, DailyLog::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        // 1 -> 2: adds period_days.loggedAt (existing rows get 0).
        AutoMigration(from = 1, to = 2),
        // 2 -> 3: adds daily_logs.noFlow (existing rows get false).
        AutoMigration(from = 2, to = 3),
    ],
)
@TypeConverters(Converters::class)
abstract class CycledDatabase : RoomDatabase() {
    abstract fun periodDayDao(): PeriodDayDao
    abstract fun dailyLogDao(): DailyLogDao

    companion object {
        private const val DATABASE_NAME = "cycled.db"

        @Volatile
        private var instance: CycledDatabase? = null

        fun getInstance(context: Context): CycledDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CycledDatabase::class.java,
                    DATABASE_NAME,
                ).build().also { instance = it }
            }
    }
}
