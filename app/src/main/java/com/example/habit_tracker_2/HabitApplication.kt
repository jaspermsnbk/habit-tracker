package com.example.habit_tracker_2

import android.app.Application
import com.example.habit_tracker_2.data.HabitDatabase
import com.example.habit_tracker_2.data.HabitRepository
import com.example.habit_tracker_2.data.SessionStore

/**
 * Holds the app-wide singletons (database, repository, session). In Phase 3 this is where
 * the Retrofit API client and DI (Hilt) will be introduced.
 */
class HabitApplication : Application() {
    val repository: HabitRepository by lazy {
        HabitRepository(HabitDatabase.get(this).habitDao())
    }

    val sessionStore: SessionStore by lazy { SessionStore(this) }
}
