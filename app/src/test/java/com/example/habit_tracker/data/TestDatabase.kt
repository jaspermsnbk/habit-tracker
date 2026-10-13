package com.jaspermsnbk.habit_tracker.data

import androidx.compose.ui.test.junit4.ComposeTestRule
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

/**
 * Waits for [condition], letting the main thread run between checks. Room finishes queries on a
 * background thread and its coroutines resume on the main thread, so a write made of several
 * queries, or a screen built from several flows, only settles if the main thread gets turns
 * while the test waits.
 */
fun ComposeTestRule.waitUntilSettled(timeoutMillis: Long = 5_000, condition: () -> Boolean) {
    waitUntil(timeoutMillis) {
        Thread.sleep(25)
        waitForIdle()
        condition()
    }
}
