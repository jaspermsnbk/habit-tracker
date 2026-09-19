package com.jaspermsnbk.habit_tracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.CheckboxDefaults
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.jaspermsnbk.habit_tracker.HabitApplication
import com.jaspermsnbk.habit_tracker.MainActivity
import com.jaspermsnbk.habit_tracker.data.HabitUi

/**
 * Home screen widget listing today's habits, each checkable in place.
 *
 * An `object` rather than a class so [HabitWidgetReceiver], the toggle action and the app all
 * refer to the same widget when they ask Glance to redraw it.
 */
object HabitWidget : GlanceAppWidget() {

    /** The list is scrollable, so draw to whatever size the user resized the widget to. */
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = (context.applicationContext as HabitApplication).repository

        provideContent {
            // Collected inside the composition, so while the widget is on screen it follows the
            // database instead of showing the snapshot it was drawn with.
            val habits by repository.habits.collectAsState(initial = emptyList())

            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        // appWidgetBackground() + cornerRadius() are what let the launcher round
                        // the widget off; without a background it draws straight onto the wallpaper.
                        .appWidgetBackground()
                        .background(GlanceTheme.colors.widgetBackground)
                        .cornerRadius(16.dp)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Header(done = habits.count { it.doneToday }, total = habits.size)
                    Spacer(GlanceModifier.height(8.dp))
                    if (habits.isEmpty()) {
                        EmptyState()
                    } else {
                        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                            items(habits, itemId = { it.id.hashCode().toLong() }) { habit ->
                                HabitRow(habit)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Title plus today's progress. Tapping it opens the app. */
@Composable
private fun Header(done: Int, total: Int) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            text = "Today",
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            ),
            modifier = GlanceModifier.defaultWeight(),
        )
        if (total > 0) {
            Text(
                text = "$done/$total",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}

/** One habit: its colour, its checkbox, and its streak once there is one. */
@Composable
private fun HabitRow(habit: HabitUi) {
    val accent = ColorProvider(accentOf(habit.color))
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Box(modifier = GlanceModifier.size(8.dp).cornerRadius(4.dp).background(accent)) {}
        Spacer(GlanceModifier.width(8.dp))
        CheckBox(
            checked = habit.doneToday,
            onCheckedChange = actionRunCallback<ToggleHabitAction>(
                actionParametersOf(habitIdKey to habit.id)
            ),
            modifier = GlanceModifier.defaultWeight(),
            text = habit.name,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp),
            // A checkbox takes plain colours: handed GlanceTheme's resource-backed providers it
            // throws, and the whole widget falls back to "can't show content".
            colors = CheckboxDefaults.colors(
                checkedColor = accentOf(habit.color),
                uncheckedColor = UncheckedOutline,
            ),
            maxLines = 1,
        )
        if (habit.currentStreak > 0) {
            Text(
                text = "${habit.currentStreak}d",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Tap to add your first habit",
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
        )
    }
}

/** One outline grey, since a checkbox can't take a themed colour that follows day and night. */
private val UncheckedOutline = Color(0xFF8A8791)

/** Habit colours are stored as hex; fall back to the app's purple if one can't be parsed. */
private fun accentOf(hex: String): Color =
    runCatching { Color(hex.toColorInt()) }.getOrDefault(Color(0xFF6650A4))
