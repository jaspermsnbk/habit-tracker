package com.jaspermsnbk.habit_tracker

import android.app.Application
import androidx.glance.appwidget.updateAll
import com.jaspermsnbk.habit_tracker.data.HabitDatabase
import com.jaspermsnbk.habit_tracker.data.HabitRepository
import com.jaspermsnbk.habit_tracker.data.PreferencesStore
import com.jaspermsnbk.habit_tracker.data.SessionStore
import com.jaspermsnbk.habit_tracker.notifications.ReminderNotifier
import com.jaspermsnbk.habit_tracker.notifications.ReminderScheduler
import com.jaspermsnbk.habit_tracker.widget.HabitWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Holds the app-wide singletons (database, repository, session, preferences, reminders). In
 * Phase 3 this is where the Retrofit API client and DI (Hilt) will be introduced.
 */
class HabitApplication : Application() {
    val repository: HabitRepository by lazy {
        HabitRepository(HabitDatabase.get(this).habitDao())
    }

    val sessionStore: SessionStore by lazy { SessionStore(this) }

    val preferencesStore: PreferencesStore by lazy { PreferencesStore(this) }

    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(this) }

    /** Lives as long as the process, for work that follows app-wide state. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        ReminderNotifier.createChannel(this)

        // Keeps the reminder alarm in step with Settings. The current value arrives on every process
        // start too, which re-arms the alarm after the app was force-stopped.
        appScope.launch {
            preferencesStore.preferences
                .map { it.reminderEnabled to it.reminderTime }
                .distinctUntilChanged()
                .collect { (enabled, time) ->
                    if (enabled) reminderScheduler.schedule(time) else reminderScheduler.cancel()
                }
        }

        // Keeps the home screen widget in sync whenever habits change from inside the app. Room
        // re-emits on any write to the tables it watches, so identical lists are dropped rather
        // than costing a redraw.
        appScope.launch {
            repository.habits
                .distinctUntilChanged()
                .collect { HabitWidget.updateAll(this@HabitApplication) }
        }
    }
}
