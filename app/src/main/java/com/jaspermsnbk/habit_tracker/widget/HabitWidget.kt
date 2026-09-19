package com.jaspermsnbk.habit_tracker.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.jaspermsnbk.habit_tracker.HabitApplication
import kotlinx.coroutines.flow.first

/**
 * Today's habits, checkable straight from the home screen. Reads a one-shot snapshot of the
 * repository each time it's (re)provided; [com.jaspermsnbk.habit_tracker.HabitApplication]
 * keeps it fresh by calling [androidx.glance.appwidget.updateAll] whenever habit data changes,
 * and [ToggleHabitAction] refreshes this instance immediately after a tap.
 */
class HabitWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as HabitApplication
        val habits = app.repository.habits.first()

        provideContent {
            Column(modifier = GlanceModifier.fillMaxSize().padding(12.dp)) {
                Text("Today's habits", style = TextStyle(fontWeight = FontWeight.Bold))
                if (habits.isEmpty()) {
                    Text("Add a habit to see it here")
                } else {
                    LazyColumn {
                        items(habits, itemId = { it.id.hashCode().toLong() }) { habit ->
                            CheckBox(
                                checked = habit.doneToday,
                                onCheckedChange = actionRunCallback<ToggleHabitAction>(
                                    actionParametersOf(habitIdKey to habit.id)
                                ),
                                text = habit.name,
                            )
                        }
                    }
                }
            }
        }
    }
}
