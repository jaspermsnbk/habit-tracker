package com.jaspermsnbk.habit_tracker.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.jaspermsnbk.habit_tracker.HabitApplication

internal val habitIdKey = ActionParameters.Key<String>("habitId")

class ToggleHabitAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val habitId = parameters[habitIdKey] ?: return
        val app = context.applicationContext as HabitApplication
        app.repository.toggleToday(habitId)
        HabitWidget.update(context, glanceId) // refresh this instance immediately
    }
}
