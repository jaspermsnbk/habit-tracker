package com.example.habit_tracker_2.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider

/** A fresh, throwaway database per test. Remember to close it. */
fun inMemoryDatabase(): HabitDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), HabitDatabase::class.java)
        .allowMainThreadQueries()
        .build()

/** Preferences for a test; Robolectric gives each test fresh shared preferences. */
fun testPreferences(): PreferencesStore = PreferencesStore(ApplicationProvider.getApplicationContext())
