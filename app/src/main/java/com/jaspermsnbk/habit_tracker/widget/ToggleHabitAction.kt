package com.jaspermsnbk.habit_tracker.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.update
import com.jaspermsnbk.habit_tracker.HabitApplication

internal val habitIdKey = ActionParameters.Key<String>("habitId")

/** Toggles a habit's completion for today when its checkbox is tapped in the widget. */
class ToggleHabitAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val habitId = parameters[habitIdKey] ?: return
        val app = context.applicationContext as HabitApplication
        app.repository.toggleToday(habitId)
        HabitWidget().update(context, glanceId)
    }
}
