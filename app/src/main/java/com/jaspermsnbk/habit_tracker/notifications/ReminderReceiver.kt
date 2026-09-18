package com.jaspermsnbk.habit_tracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jaspermsnbk.habit_tracker.HabitApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Shows the daily reminder when its alarm fires, then schedules tomorrow's. Also re-arms the
 * alarm after a reboot, an app update, or a change to the clock or time zone, since Android
 * drops or misplaces alarms then.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as HabitApplication
        val preferences = app.preferencesStore.preferences.value
        if (!preferences.reminderEnabled) return

        app.reminderScheduler.schedule(preferences.reminderTime)
        if (intent.action != ACTION_REMIND) return

        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val habitsLeft = app.repository.habits.first().filterNot { it.doneToday }
                ReminderNotifier.show(context, habitsLeft)
            } finally {
                result.finish()
            }
        }
    }

    companion object {
        const val ACTION_REMIND = "com.jaspermsnbk.habit_tracker_2.action.REMIND"
    }
}
