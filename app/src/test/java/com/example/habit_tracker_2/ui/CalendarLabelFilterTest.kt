package com.example.habit_tracker_2.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.habit_tracker_2.data.HabitDatabase
import com.example.habit_tracker_2.data.HabitRepository
import com.example.habit_tracker_2.data.inMemoryDatabase
import com.example.habit_tracker_2.data.testPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class CalendarLabelFilterTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: HabitDatabase
    private val todayLabel = LocalDate.now().format(DAY_FORMAT)

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        val repository = HabitRepository(db.habitDao())
        runBlocking {
            repository.addLabel("Fitness")
            repository.addLabel("Learning")
            val labels = repository.labels.first().associate { it.name to it.id }
            repository.addHabit("Run", "#2E7D32", labels.getValue("Fitness"))
            repository.addHabit("Read", "#1565C0", labels.getValue("Learning"))
            repository.habits.first().forEach { repository.toggleToday(it.id) }
        }

        val viewModel = HabitViewModel(repository, testPreferences())
        composeRule.setContent { CalendarScreen(viewModel) }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("2 check-ins this month").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun selectingLabel_tracksOnlyThatLabelsHabits() {
        composeRule.onNodeWithText("Fitness").performClick()

        composeRule.onNodeWithText("Fitness: 1 check-in this month").assertExists()
        composeRule.onNodeWithContentDescription("$todayLabel, completed: Run").assertExists()
        // The "Read" habit chip belongs to another label, so it's hidden.
        composeRule.onNodeWithText("Read").assertDoesNotExist()
    }

    @Test
    fun dayDetails_onlyListHabitsInSelectedLabel() {
        composeRule.onNodeWithText("Learning").performClick()

        composeRule.onNodeWithContentDescription("$todayLabel, completed: Read").performClick()

        composeRule.onNodeWithText("1 of 1 habit completed").assertIsDisplayed()
    }

    @Test
    fun switchingLabel_clearsHabitSelection() {
        composeRule.onNodeWithText("Run").performClick()
        composeRule.onNodeWithText("Run: 1 day this month").assertExists()

        composeRule.onNodeWithText("Learning").performClick()

        composeRule.onNodeWithText("Learning: 1 check-in this month").assertExists()
    }
}
