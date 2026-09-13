package com.example.habit_tracker_2.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.habit_tracker_2.data.HabitDatabase
import com.example.habit_tracker_2.data.HabitRepository
import com.example.habit_tracker_2.data.inMemoryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DayHabitsSheetScrollTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: HabitDatabase

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        val repository = HabitRepository(db.habitDao())
        runBlocking {
            (1..HABIT_COUNT).forEach { repository.addHabit("Habit %02d".format(it), "#2E7D32") }
            repository.habits.first().forEach { repository.toggleToday(it.id) }
        }

        val viewModel = HabitViewModel(repository)
        composeRule.setContent { CalendarScreen(viewModel) }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("$HABIT_COUNT check-ins this month").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun longDayList_scrollsToLastHabit() {
        val todayLabel = LocalDate.now().format(DAY_FORMAT)
        // The trailing comma keeps e.g. "September 1," from matching "September 12,".
        composeRule.onNode(hasContentDescription("$todayLabel,", substring = true) and hasClickAction())
            .performClick()
        composeRule.onNodeWithText("$HABIT_COUNT of $HABIT_COUNT habits completed").assertIsDisplayed()

        val lastRow = hasText("Habit $HABIT_COUNT") and hasAnyAncestor(hasTestTag(DAY_HABITS_LIST_TAG))
        composeRule.onNodeWithTag(DAY_HABITS_LIST_TAG).performScrollToNode(lastRow)

        composeRule.onNode(lastRow).assertIsDisplayed()
    }

    private companion object {
        const val HABIT_COUNT = 30
    }
}
