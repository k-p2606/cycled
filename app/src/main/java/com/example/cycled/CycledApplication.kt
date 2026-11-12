package com.example.cycled

import android.app.Application
import com.example.cycled.data.CycleRepository
import com.example.cycled.data.CycledDatabase

class CycledApplication : Application() {
    private val database: CycledDatabase by lazy { CycledDatabase.getInstance(this) }

    val repository: CycleRepository by lazy {
        CycleRepository(database.periodDayDao(), database.dailyLogDao())
    }
}
