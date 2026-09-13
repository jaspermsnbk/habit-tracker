package com.example.habit_tracker_2.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class PreferencesStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun freshInstall_usesDefaults() {
        assertEquals(AppPreferences(), PreferencesStore(context).preferences.value)
    }

    @Test
    fun changes_arePublishedRightAway() {
        val store = PreferencesStore(context)

        store.setTheme(ThemeMode.Dark)
        store.setDynamicColor(false)
        store.setFirstDayOfWeek(DayOfWeek.MONDAY)
        store.setTrendsRange("Year")

        assertEquals(
            AppPreferences(
                theme = ThemeMode.Dark,
                dynamicColor = false,
                firstDayOfWeek = DayOfWeek.MONDAY,
                trendsRange = "Year",
            ),
            store.preferences.value,
        )
    }

    @Test
    fun changes_persistAcrossInstances() {
        PreferencesStore(context).apply {
            setTheme(ThemeMode.Light)
            setFirstDayOfWeek(DayOfWeek.SATURDAY)
        }

        val preferences = PreferencesStore(context).preferences.value
        assertEquals(ThemeMode.Light, preferences.theme)
        assertEquals(DayOfWeek.SATURDAY, preferences.firstDayOfWeek)
    }

    @Test
    fun clearingWeekStart_followsTheRegionAgain() {
        val store = PreferencesStore(context)
        store.setFirstDayOfWeek(DayOfWeek.MONDAY)

        store.setFirstDayOfWeek(null)

        assertNull(store.preferences.value.firstDayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, store.preferences.value.weekStart(Locale.US))
        assertEquals(DayOfWeek.MONDAY, store.preferences.value.weekStart(Locale.UK))
    }

    @Test
    fun reminder_isOffAt8pmByDefault_andPersists() {
        assertEquals(false, PreferencesStore(context).preferences.value.reminderEnabled)
        assertEquals(LocalTime.of(20, 0), PreferencesStore(context).preferences.value.reminderTime)

        PreferencesStore(context).apply {
            setReminderEnabled(true)
            setReminderTime(LocalTime.of(7, 45))
        }

        val preferences = PreferencesStore(context).preferences.value
        assertEquals(true, preferences.reminderEnabled)
        assertEquals(LocalTime.of(7, 45), preferences.reminderTime)
    }

    @Test
    fun chosenWeekStart_overridesTheRegion() {
        assertEquals(DayOfWeek.SATURDAY, AppPreferences(firstDayOfWeek = DayOfWeek.SATURDAY).weekStart(Locale.US))
    }
}
