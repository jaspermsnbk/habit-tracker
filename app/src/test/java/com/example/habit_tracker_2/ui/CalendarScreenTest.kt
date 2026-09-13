package com.example.habit_tracker_2.ui

import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class CalendarScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: HabitDatabase
    private val today = LocalDate.now()
    private val todayLabel = today.format(DAY_FORMAT)

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        val repository = HabitRepository(db.habitDao())
        runBlocking {
            repository.addHabit("Read", "#2E7D32")
            repository.addHabit("Run", "#1565C0")
            val read = repository.habits.first().first { it.name == "Read" }
            repository.toggleToday(read.id)
        }

        val viewModel = HabitViewModel(repository, testPreferences())
        composeRule.setContent { CalendarScreen(viewModel) }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Run").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun showsCurrentMonth_withNextDisabled() {
        composeRule.onNodeWithText(YearMonth.now().format(MONTH_FORMAT)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Next month").assertIsNotEnabled()
    }

    @Test
    fun allHabits_marksWhichHabitsWereCompletedToday() {
        composeRule.onNodeWithContentDescription("$todayLabel, completed: Read").assertExists()
        composeRule.onNodeWithText("1 check-in this month").assertExists()
    }

    @Test
    fun selectingHabit_showsOnlyThatHabitsDays() {
        composeRule.onNodeWithText("Run").performClick()

        composeRule.onNodeWithContentDescription("$todayLabel, nothing completed").assertExists()
        composeRule.onNodeWithText("Run: 0 days this month").assertExists()

        composeRule.onNodeWithText("Read").performClick()

        composeRule.onNodeWithContentDescription("$todayLabel, completed: Read").assertExists()
        composeRule.onNodeWithText("Read: 1 day this month").assertExists()
    }

    @Test
    fun monthNavigation_movesBackAndForward() {
        val current = YearMonth.now()

        composeRule.onNodeWithContentDescription("Previous month").performClick()

        composeRule.onNodeWithText(current.minusMonths(1).format(MONTH_FORMAT)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Next month").assertIsEnabled().performClick()
        composeRule.onNodeWithText(current.format(MONTH_FORMAT)).assertIsDisplayed()
    }

    @Test
    fun tappingDay_withAllHabits_listsHabitsCompletedThatDay() {
        composeRule.onNodeWithContentDescription("$todayLabel, completed: Read").performClick()

        composeRule.onNodeWithText(today.format(SHEET_DATE_FORMAT)).assertIsDisplayed()
        composeRule.onNodeWithText("1 of 2 habits completed").assertIsDisplayed()
        composeRule.onNode(hasText("Read") and hasAnyAncestor(hasTestTag(DAY_HABITS_LIST_TAG)))
            .assertIsDisplayed()
        composeRule.onNode(hasText("Run") and hasAnyAncestor(hasTestTag(DAY_HABITS_LIST_TAG)))
            .assertDoesNotExist()
    }

    @Test
    fun tappingDay_withNothingCompleted_saysSo() {
        val day = YearMonth.now().minusMonths(1).atDay(15)
        composeRule.onNodeWithContentDescription("Previous month").performClick()

        composeRule.onNodeWithContentDescription("${day.format(DAY_FORMAT)}, nothing completed").performClick()

        composeRule.onNodeWithText(day.format(SHEET_DATE_FORMAT)).assertIsDisplayed()
        composeRule.onNodeWithText("Nothing completed").assertIsDisplayed()
        composeRule.onNodeWithTag(DAY_HABITS_LIST_TAG).assertDoesNotExist()
    }

    @Test
    fun tappingDay_withOneHabitSelected_doesNotOpenDetails() {
        composeRule.onNodeWithText("Read").performClick()

        composeRule.onNodeWithContentDescription("$todayLabel, completed: Read")
            .assertHasNoClickAction()
            .performClick()

        composeRule.onNodeWithText(today.format(SHEET_DATE_FORMAT)).assertDoesNotExist()
    }
}
