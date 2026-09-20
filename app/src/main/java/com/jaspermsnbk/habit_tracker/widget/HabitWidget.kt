package com.jaspermsnbk.habit_tracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
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
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.jaspermsnbk.habit_tracker.HabitApplication
import com.jaspermsnbk.habit_tracker.MainActivity
import com.jaspermsnbk.habit_tracker.R
import com.jaspermsnbk.habit_tracker.data.HabitUi

/** The label a widget instance was configured with; absent means "all labels". */
internal val widgetLabelIdKey = stringPreferencesKey("widget_label_id")

/**
 * Home screen widget listing today's habits, each checkable in place.
 *
 * Deliberately mirrors the in-app habit list (ui/HabitListScreen.kt): the same accent ring toggle,
 * the same streak wording and the same seven-dot week, on cards of the same shape. Glance has no
 * Card, border or Material icons, so those are rebuilt here out of what a widget can draw.
 *
 * An `object` rather than a class so [HabitWidgetReceiver], the toggle action and the app all
 * refer to the same widget when they ask Glance to redraw it.
 */
object HabitWidget : GlanceAppWidget() {

    /** The list is scrollable, so draw to whatever size the user resized the widget to. */
    override val sizeMode = SizeMode.Exact

    /** Per instance, so two widgets can show different labels. Written by the config activity. */
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = (context.applicationContext as HabitApplication).repository

        provideContent {
            // Collected inside the composition, so while the widget is on screen it follows the
            // database instead of showing the snapshot it was drawn with.
            val habits by repository.habits.collectAsState(initial = emptyList())
            val labels by repository.labels.collectAsState(initial = emptyList())

            // A label that has since been deleted filters nothing, rather than emptying the widget.
            val label = currentState(widgetLabelIdKey)?.let { labelId ->
                labels.find { it.id == labelId }
            }
            val visible = if (label == null) habits else habits.filter { it.labelId == label.id }

            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        // appWidgetBackground() + cornerRadius() are what let the launcher round
                        // the widget off; without a background it draws straight onto the wallpaper.
                        .appWidgetBackground()
                        .background(GlanceTheme.colors.widgetBackground)
                        .cornerRadius(16.dp)
                        .padding(12.dp),
                ) {
                    Header(
                        labelName = label?.name,
                        done = visible.count { it.doneToday },
                        total = visible.size,
                    )
                    Spacer(GlanceModifier.height(10.dp))
                    when {
                        habits.isEmpty() -> EmptyState("No habits yet.\nTap to add your first one.")
                        visible.isEmpty() -> EmptyState("No habits with this label yet.")
                        else -> LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                            items(visible, itemId = { it.id.hashCode().toLong() }) { habit ->
                                HabitCard(habit)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Title, the label this widget is filtered to, and today's progress. */
@Composable
private fun Header(labelName: String?, done: Int, total: Int) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            text = "Today",
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        labelName?.let {
            Spacer(GlanceModifier.width(6.dp))
            LabelPill(it)
        }
        Spacer(GlanceModifier.defaultWeight())
        if (total > 0) {
            Text(
                text = "$done of $total",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}

/**
 * One habit, laid out like the app's HabitCard: accent toggle, name and label, streak, week dots.
 * Tapping anywhere but the toggle opens the app, where the habit can be edited.
 */
@Composable
private fun HabitCard(habit: HabitUi) {
    val accent = accentOf(habit.color)

    Column(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(12.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(10.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            DoneToggle(habit = habit, accent = accent)
            Spacer(GlanceModifier.width(10.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Text(
                        text = habit.name,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                        // Keeps a long name from pushing the label pill out of the row.
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    habit.labelName?.let { labelName ->
                        Spacer(GlanceModifier.width(6.dp))
                        LabelPill(labelName)
                    }
                }
                Spacer(GlanceModifier.height(3.dp))
                Text(
                    text = if (habit.currentStreak == 0) {
                        "No streak yet"
                    } else {
                        "🔥 ${habit.currentStreak} day streak"
                    },
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
                    maxLines = 1,
                )
                Spacer(GlanceModifier.height(5.dp))
                WeekRow(last7 = habit.last7, accent = accent)
            }
        }
    }
}

/** The app's 40dp toggle: an accent ring, filled with an accent check once today is done. */
@Composable
private fun DoneToggle(habit: HabitUi, accent: Color) {
    val art = if (habit.doneToday) R.drawable.widget_habit_check else R.drawable.widget_habit_ring
    Box(
        modifier = GlanceModifier.size(36.dp).clickable(
            actionRunCallback<ToggleHabitAction>(actionParametersOf(habitIdKey to habit.id))
        ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(art),
            contentDescription = if (habit.doneToday) {
                "${habit.name}, done today"
            } else {
                "${habit.name}, not done today"
            },
            modifier = GlanceModifier.size(32.dp),
            colorFilter = ColorFilter.tint(ColorProvider(accent)),
        )
    }
}

/** A label, as the app's rounded secondary-container pill. */
@Composable
private fun LabelPill(name: String) {
    Text(
        text = name,
        style = TextStyle(color = GlanceTheme.colors.onSecondaryContainer, fontSize = 10.sp),
        maxLines = 1,
        modifier = GlanceModifier
            .background(GlanceTheme.colors.secondaryContainer)
            .cornerRadius(8.dp)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

/** Seven dots for the last week; filled = completed that day, last dot = today. */
@Composable
private fun WeekRow(last7: List<Boolean>, accent: Color) {
    Row {
        last7.forEachIndexed { index, done ->
            if (index > 0) Spacer(GlanceModifier.width(4.dp))
            Box(
                modifier = GlanceModifier
                    .size(10.dp)
                    .cornerRadius(5.dp)
                    .background(
                        if (done) ColorProvider(accent) else GlanceTheme.colors.surfaceVariant
                    ),
            ) {}
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
        )
    }
}

/** Habit colours are stored as hex; fall back to the app's purple if one can't be parsed. */
private fun accentOf(hex: String): Color =
    runCatching { Color(hex.toColorInt()) }.getOrDefault(Color(0xFF6650A4))
