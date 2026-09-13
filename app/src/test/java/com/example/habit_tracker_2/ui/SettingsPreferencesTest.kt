package com.example.habit_tracker_2.ui

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.habit_tracker_2.data.HabitDatabase
import com.example.habit_tracker_2.data.HabitRepository
import com.example.habit_tracker_2.data.PreferencesStore
import com.example.habit_tracker_2.data.ThemeMode
import com.example.habit_tracker_2.data.inMemoryDatabase
import com.example.habit_tracker_2.data.testPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek

@RunWith(AndroidJUnit4::class)
class SettingsPreferencesTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: HabitDatabase
    private val preferences: PreferencesStore = testPreferences()

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        val viewModel = HabitViewModel(HabitRepository(db.habitDao()), preferences)
        composeRule.setContent { SettingsScreen(viewModel, onBack = {}) }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun row(title: String, value: String) = composeRule.onNode(hasText(title) and hasText(value))

    /** Opens the [title] row's dialog and picks [option]. */
    private fun choose(title: String, option: String) {
        composeRule.onNodeWithText(title).performScrollTo().performClick()
        composeRule.onNodeWithText(option).performClick()
    }

    @Test
    fun theme_savesChoice_andShowsIt() {
        row("Theme", "System default").assertExists()

        choose("Theme", "Dark")

        assertEquals(ThemeMode.Dark, preferences.preferences.value.theme)
        row("Theme", "Dark").assertExists()
        // Picking an option closes the dialog.
        composeRule.onNodeWithText("Light").assertDoesNotExist()
    }

    @Test
    fun dynamicColor_togglesFromTheWholeRow() {
        val dynamicColor = composeRule.onNode(hasText("Dynamic color"))
        dynamicColor.performScrollTo().assertIsOn()

        dynamicColor.performClick()

        dynamicColor.assertIsOff()
        assertFalse(preferences.preferences.value.dynamicColor)
    }

    @Test
    fun weekStart_canBeChosen_andResetToTheRegion() {
        row("Week starts on", "System default (Sunday)").assertExists()

        choose("Week starts on", "Monday")

        assertEquals(DayOfWeek.MONDAY, preferences.preferences.value.firstDayOfWeek)
        row("Week starts on", "Monday").assertExists()

        choose("Week starts on", "System default (Sunday)")

        assertNull(preferences.preferences.value.firstDayOfWeek)
    }

    @Test
    fun trendsRange_savesChoice() {
        row("Trends opens on", "30 days").assertExists()

        choose("Trends opens on", "1 year")

        assertEquals(TrendRange.Year.name, preferences.preferences.value.trendsRange)
        row("Trends opens on", "1 year").assertExists()
    }
}
