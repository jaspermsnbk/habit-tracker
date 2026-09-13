package com.example.habit_tracker_2.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.temporal.WeekFields
import java.util.Locale

enum class ThemeMode { System, Light, Dark }

/**
 * The choices made in Settings.
 *
 * @param firstDayOfWeek the day weeks start on, or null to follow the phone's region
 * @param trendsRange the name of the range Trends opens on, or null for its default
 * @param reminderEnabled whether to send a daily reminder about habits not done yet
 * @param reminderTime when that reminder goes out
 */
data class AppPreferences(
    val theme: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = true,
    val firstDayOfWeek: DayOfWeek? = null,
    val trendsRange: String? = null,
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime = DEFAULT_REMINDER_TIME,
)

/** 8 PM: late enough that most habits are done, early enough to still do the rest. */
val DEFAULT_REMINDER_TIME: LocalTime = LocalTime.of(20, 0)

/** The day weeks start on: the user's choice, or else the [locale]'s. */
fun AppPreferences.weekStart(locale: Locale = Locale.getDefault()): DayOfWeek =
    firstDayOfWeek ?: WeekFields.of(locale).firstDayOfWeek

/**
 * Saves Settings choices on the device. [preferences] always holds the current values,
 * so screens start with what was saved rather than a placeholder.
 */
class PreferencesStore(context: Context) {

    private val prefs = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(load())

    val preferences: StateFlow<AppPreferences> = state.asStateFlow()

    fun setTheme(theme: ThemeMode) = save { putString(KEY_THEME, theme.name) }

    fun setDynamicColor(enabled: Boolean) = save { putBoolean(KEY_DYNAMIC_COLOR, enabled) }

    fun setFirstDayOfWeek(day: DayOfWeek?) = save {
        if (day == null) remove(KEY_WEEK_START) else putString(KEY_WEEK_START, day.name)
    }

    fun setTrendsRange(rangeName: String) = save { putString(KEY_TRENDS_RANGE, rangeName) }

    fun setReminderEnabled(enabled: Boolean) = save { putBoolean(KEY_REMINDER_ENABLED, enabled) }

    fun setReminderTime(time: LocalTime) = save { putInt(KEY_REMINDER_MINUTE, time.hour * 60 + time.minute) }

    private fun save(changes: SharedPreferences.Editor.() -> Unit) {
        prefs.edit(action = changes)
        state.value = load()
    }

    // Values this version doesn't recognize fall back to the defaults.
    private fun load() = AppPreferences(
        theme = prefs.getString(KEY_THEME, null)
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.System,
        dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, true),
        firstDayOfWeek = prefs.getString(KEY_WEEK_START, null)
            ?.let { runCatching { DayOfWeek.valueOf(it) }.getOrNull() },
        trendsRange = prefs.getString(KEY_TRENDS_RANGE, null),
        reminderEnabled = prefs.getBoolean(KEY_REMINDER_ENABLED, false),
        reminderTime = prefs.getInt(KEY_REMINDER_MINUTE, -1)
            .takeIf { it in 0 until 24 * 60 }
            ?.let { LocalTime.of(it / 60, it % 60) }
            ?: DEFAULT_REMINDER_TIME,
    )

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
        const val KEY_WEEK_START = "week_start"
        const val KEY_TRENDS_RANGE = "trends_range"
        const val KEY_REMINDER_ENABLED = "reminder_enabled"
        const val KEY_REMINDER_MINUTE = "reminder_minute_of_day"
    }
}
