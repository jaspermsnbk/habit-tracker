package com.jaspermsnbk.habit_tracker.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jaspermsnbk.habit_tracker.data.HabitDatabase
import com.jaspermsnbk.habit_tracker.data.HabitEntryEntity
import com.jaspermsnbk.habit_tracker.data.HabitRepository
import com.jaspermsnbk.habit_tracker.data.inMemoryDatabase
import com.jaspermsnbk.habit_tracker.data.testPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class TrendsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: HabitDatabase

    /**
     * Read: done today, 1, 2 and 5 days ago (4 of 6 tracked days, best streak 3).
     * Run: done today (1 of 1). Together: 5 of 7, or 71%.
     */
    @Before
    fun setUp() {
        db = inMemoryDatabase()
        val repository = HabitRepository(db.habitDao())
        val today = LocalDate.now()
        runBlocking {
            repository.addHabit("Read", "#2E7D32")
            repository.addHabit("Run", "#1565C0")
            val habits = repository.habits.first()
            listOf(0L, 1, 2, 5).forEach { daysAgo ->
                db.habitDao().insertEntry(
                    HabitEntryEntity(
                        id = UUID.randomUUID().toString(),
                        habitId = habits.first { it.name == "Read" }.id,
                        date = today.minusDays(daysAgo),
                        createdAt = Instant.now(),
                    )
                )
            }
            repository.toggleToday(habits.first { it.name == "Run" }.id)
        }

        val viewModel = HabitViewModel(repository, testPreferences())
        composeRule.setContent { TrendsScreen(viewModel) }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Run").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun tile(value: String, label: String) = composeRule.onNode(hasText(value) and hasText(label))

    @Test
    fun allHabits_summarizesCheckInsCompletionAndStreak() {
        tile("5", "Check-ins").assertExists()
        tile("71%", "Completion").assertExists()
        tile("3 days", "Best streak").assertExists()
    }

    @Test
    fun breakdown_listsEachHabit_andTappingOneFocusesIt() {
        composeRule.onNode(hasText("4 of 6 days · best streak 3 days")).assertExists()
        composeRule.onNode(hasText("1 of 1 day · best streak 1 day"))
            .performScrollTo()
            .performClick()

        tile("1", "Check-ins").assertExists()
        tile("100%", "Completion").assertExists()
        composeRule.onNodeWithText("By habit").assertDoesNotExist()
    }

    @Test
    fun selectingHabitChip_narrowsStats() {
        composeRule.onAllNodes(hasText("Read") and hasClickAction()).onFirst().performClick()

        tile("4", "Check-ins").assertExists()
        tile("67%", "Completion").assertExists()
        composeRule.onNodeWithText("By habit").assertDoesNotExist()
    }

    @Test
    fun tappingChart_inspectsBar_andTappingAgainClears() {
        val chart = composeRule.onNodeWithTag(TREND_CHART_TAG).performScrollTo()
        chart.performTouchInput { click(Offset(width - 1f, centerY)) }

        composeRule.onNodeWithText("Today: 2 of 2 (100%)").assertExists()

        chart.performTouchInput { click(Offset(width - 1f, centerY)) }

        composeRule.onNodeWithText("Today: 2 of 2 (100%)").assertDoesNotExist()
    }

    @Test
    fun yearRange_plotsWeeks() {
        composeRule.onNodeWithText("1Y").performClick()

        composeRule.onNodeWithText("Check-ins per week").assertExists()
        composeRule.onNodeWithTag(TREND_CHART_TAG)
            .performScrollTo()
            .performTouchInput { click(Offset(width - 1f, centerY)) }
        composeRule.onNodeWithText("Last 7 days: 5 of 7 (71%)").assertExists()
    }

    @Test
    fun weekRange_hidesWeekdayPattern() {
        composeRule.onNodeWithText("By weekday").assertExists()

        composeRule.onNodeWithText("7D").performClick()

        composeRule.onNodeWithText("By weekday").assertDoesNotExist()
    }
}
