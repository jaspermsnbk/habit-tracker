package com.example.habit_tracker_2.ui

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.habit_tracker_2.data.HabitDatabase
import com.example.habit_tracker_2.data.HabitEntryEntity
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
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class TrendsLabelFilterTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: HabitDatabase

    /** Run (Fitness): done today. Read (Learning): done today and yesterday. "Empty" has no habits. */
    @Before
    fun setUp() {
        db = inMemoryDatabase()
        val repository = HabitRepository(db.habitDao())
        runBlocking {
            repository.addLabel("Fitness")
            repository.addLabel("Learning")
            repository.addLabel("Empty")
            val labels = repository.labels.first().associate { it.name to it.id }
            repository.addHabit("Run", "#2E7D32", labels.getValue("Fitness"))
            repository.addHabit("Read", "#1565C0", labels.getValue("Learning"))
            repository.habits.first().forEach { repository.toggleToday(it.id) }
            db.habitDao().insertEntry(
                HabitEntryEntity(
                    id = UUID.randomUUID().toString(),
                    habitId = repository.habits.first().first { it.name == "Read" }.id,
                    date = LocalDate.now().minusDays(1),
                    createdAt = Instant.now(),
                )
            )
        }

        val viewModel = HabitViewModel(repository, testPreferences())
        composeRule.setContent { TrendsScreen(viewModel) }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("3") and hasText("Check-ins")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun checkIns(value: String) = composeRule.onNode(hasText(value) and hasText("Check-ins"))

    @Test
    fun selectingLabel_tracksOnlyThatLabelsHabits() {
        composeRule.onNodeWithText("Fitness").performClick()

        checkIns("1").assertExists()
        // Read belongs to another label, so neither its chip nor its breakdown row is shown.
        composeRule.onNode(hasText("Read")).assertDoesNotExist()
        composeRule.onNodeWithText("By habit").assertDoesNotExist()
    }

    @Test
    fun switchingLabel_clearsHabitSelection() {
        composeRule.onAllNodes(hasText("Read") and hasClickAction()).onFirst().performClick()
        checkIns("2").assertExists()

        composeRule.onNodeWithText("Learning").performClick()
        composeRule.onNodeWithText("All labels").performClick()

        checkIns("3").assertExists()
    }

    @Test
    fun labelWithoutHabits_saysSo() {
        composeRule.onNodeWithText("Empty").performClick()

        composeRule.onNodeWithText("No habits with this label yet.").assertExists()
        composeRule.onNodeWithText("All habits").assertDoesNotExist()
    }
}
