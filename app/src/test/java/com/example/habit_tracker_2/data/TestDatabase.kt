package com.example.habit_tracker_2.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider

/**
 * A fresh, throwaway database per test. Remember to close it.
 *
 * @param synchronous run queries on the calling thread instead of Room's background threads. Use it
 *   for screens whose tests end before their first query would finish, so closing the database
 *   can't race a query still in flight.
 */
fun inMemoryDatabase(synchronous: Boolean = false): HabitDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), HabitDatabase::class.java)
        .allowMainThreadQueries()
        .apply {
            if (synchronous) {
                setQueryExecutor { it.run() }
                setTransactionExecutor { it.run() }
            }
        }
        .build()

/** Preferences for a test; Robolectric gives each test fresh shared preferences. */
fun testPreferences(): PreferencesStore = PreferencesStore(ApplicationProvider.getApplicationContext())
