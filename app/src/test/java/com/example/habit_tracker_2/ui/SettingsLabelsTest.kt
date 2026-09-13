package com.example.habit_tracker_2.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.habit_tracker_2.data.HabitDatabase
import com.example.habit_tracker_2.data.HabitRepository
import com.example.habit_tracker_2.data.inMemoryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsLabelsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: HabitDatabase
    private lateinit var repository: HabitRepository

    /** Fitness: Run and Walk. Learning: Read. */
    @Before
    fun setUp() {
        db = inMemoryDatabase()
        repository = HabitRepository(db.habitDao())
        runBlocking {
            repository.addLabel("Fitness")
            repository.addLabel("Learning")
            val labels = repository.labels.first().associate { it.name to it.id }
            repository.addHabit("Run", "#2E7D32", labels.getValue("Fitness"))
            repository.addHabit("Walk", "#1565C0", labels.getValue("Fitness"))
            repository.addHabit("Read", "#6A1B9A", labels.getValue("Learning"))
        }

        val viewModel = HabitViewModel(repository)
        composeRule.setContent { SettingsScreen(viewModel, onBack = {}) }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("2 habits").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun labelNames() = runBlocking { repository.labels.first().map { it.name } }

    /** Waits for the list to show [name], then checks the saved labels are exactly [names]. */
    private fun waitForLabels(name: String, vararg names: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(names.toList(), labelNames())
    }

    @Test
    fun listsLabelsWithHabitCounts() {
        composeRule.onNode(hasText("Fitness") and hasText("2 habits")).assertExists()
        composeRule.onNode(hasText("Learning") and hasText("1 habit")).assertExists()
    }

    @Test
    fun tappingLabel_renamesIt_forItsHabitsToo() {
        composeRule.onNodeWithText("Fitness").performScrollTo().performClick()
        composeRule.onNode(hasSetTextAction()).performTextReplacement("Exercise")
        composeRule.onNodeWithText("Rename").performClick()

        waitForLabels("Exercise", "Exercise", "Learning")
        val run = runBlocking { repository.habits.first() }.first { it.name == "Run" }
        assertEquals("Exercise", run.labelName)
    }

    @Test
    fun rename_blocksAnotherLabelsName_butAllowsChangingCase() {
        composeRule.onNodeWithText("Fitness").performScrollTo().performClick()

        composeRule.onNode(hasSetTextAction()).performTextReplacement("learning")
        composeRule.onNodeWithText("A label with this name already exists").assertIsDisplayed()
        composeRule.onNodeWithText("Rename").assertIsNotEnabled()

        composeRule.onNode(hasSetTextAction()).performTextReplacement("FITNESS")
        composeRule.onNodeWithText("Rename").assertIsEnabled().performClick()

        waitForLabels("FITNESS", "FITNESS", "Learning")
    }

    @Test
    fun deletingLabel_asksFirst_andKeepsItsHabits() {
        composeRule.onNodeWithContentDescription("Delete Fitness").performScrollTo().performClick()
        composeRule.onNodeWithText("\"Fitness\" will be removed. Its 2 habits stay, just without a label.")
            .assertIsDisplayed()

        composeRule.onNodeWithText("Delete").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Fitness").fetchSemanticsNodes().isEmpty()
        }
        assertEquals(listOf("Learning"), labelNames())
        val habits = runBlocking { repository.habits.first() }
        assertEquals(3, habits.size)
        assertNull(habits.first { it.name == "Run" }.labelId)
    }

    @Test
    fun cancellingDelete_keepsLabel() {
        composeRule.onNodeWithContentDescription("Delete Fitness").performScrollTo().performClick()

        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithText("Delete label?").assertDoesNotExist()
        assertEquals(listOf("Fitness", "Learning"), labelNames())
    }

    @Test
    fun newLabelRow_addsLabel() {
        composeRule.onNodeWithText("New label").performScrollTo().performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("Health")
        composeRule.onNodeWithText("Add").performClick()

        waitForLabels("No habits", "Fitness", "Health", "Learning")
    }
}
