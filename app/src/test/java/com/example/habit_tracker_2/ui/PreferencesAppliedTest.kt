package com.example.habit_tracker_2.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.habit_tracker_2.data.HabitDatabase
import com.example.habit_tracker_2.data.HabitRepository
import com.example.habit_tracker_2.data.PreferencesStore
import com.example.habit_tracker_2.data.inMemoryDatabase
import com.example.habit_tracker_2.data.testPreferences
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek

/** Settings choices reach the screens that use them. */
@RunWith(AndroidJUnit4::class)
class PreferencesAppliedTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: HabitDatabase
    private lateinit var repository: HabitRepository
    private val preferences: PreferencesStore = testPreferences()

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        repository = HabitRepository(db.habitDao())
        runBlocking { repository.addHabit("Read", "#2E7D32") }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun waitForText(text: String) = composeRule.waitUntil(timeoutMillis = 5_000) {
        composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    /** Left edge of the first weekday header labelled [letter] (M for Monday, S for Saturday/Sunday). */
    private fun leftOf(letter: String) =
        composeRule.onAllNodesWithText(letter).fetchSemanticsNodes().minOf { it.boundsInRoot.left }

    @Test
    fun calendar_startsWeeksOnChosenDay_andUpdatesWhenItChanges() {
        composeRule.setContent { CalendarScreen(HabitViewModel(repository, preferences)) }
        waitForText("Read")

        // The test locale (US) starts weeks on Sunday.
        assertTrue(leftOf("S") < leftOf("M"))

        preferences.setFirstDayOfWeek(DayOfWeek.MONDAY)
        composeRule.waitForIdle()

        assertTrue(leftOf("M") < leftOf("S"))
    }

    @Test
    fun trends_opensOnChosenRange() {
        preferences.setTrendsRange(TrendRange.Year.name)

        composeRule.setContent { TrendsScreen(HabitViewModel(repository, preferences)) }

        waitForText("Check-ins per week")
    }
}
