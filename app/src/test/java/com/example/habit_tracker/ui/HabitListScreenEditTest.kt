package com.jaspermsnbk.habit_tracker.ui

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jaspermsnbk.habit_tracker.data.HabitDatabase
import com.jaspermsnbk.habit_tracker.data.HabitRepository
import com.jaspermsnbk.habit_tracker.data.HabitUi
import com.jaspermsnbk.habit_tracker.data.inMemoryDatabase
import com.jaspermsnbk.habit_tracker.data.testPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitListScreenEditTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: HabitDatabase
    private lateinit var repository: HabitRepository

    private val dialogTextField = hasSetTextAction() and hasAnyAncestor(isDialog())
    private val saveButton = hasText("Save") and hasAnyAncestor(isDialog())
    private fun inDialog(text: String) = hasText(text) and hasAnyAncestor(isDialog())

    /** "Read", green, no label, done today; plus a "Fitness" label to file it under. */
    @Before
    fun setUp() {
        db = inMemoryDatabase()
        repository = HabitRepository(db.habitDao())
        runBlocking {
            repository.addLabel("Fitness")
            repository.addHabit("Read", "#2E7D32")
            repository.toggleToday(habit().id)
        }

        val viewModel = HabitViewModel(repository, testPreferences())
        composeRule.setContent { HabitListScreen(viewModel) }
        waitFor(hasContentDescription("Done today"))
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun habit(): HabitUi = runBlocking { repository.habits.first().single() }

    private fun waitFor(matcher: SemanticsMatcher) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openEditor() = composeRule.onNodeWithText("Read").performClick()

    @Test
    fun tappingCard_opensEditorWithCurrentValues() {
        openEditor()

        composeRule.onNodeWithText("Edit habit").assertIsDisplayed()
        composeRule.onNode(dialogTextField).assert(hasText("Read"))
        composeRule.onNode(hasContentDescription("Green") and hasAnyAncestor(isDialog())).assertIsSelected()
        composeRule.onNode(inDialog("None")).assertIsSelected()
    }

    @Test
    fun saving_updatesNameColorAndLabel_andKeepsTodaysCheckIn() {
        openEditor()
        composeRule.onNode(dialogTextField).performTextReplacement("  Read a book ")
        composeRule.onNode(hasContentDescription("Blue") and hasAnyAncestor(isDialog())).performClick()
        composeRule.onNode(inDialog("Fitness")).performClick()
        composeRule.onNode(saveButton).performClick()

        waitFor(hasText("Read a book"))
        composeRule.onNodeWithText("Edit habit").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Label: Fitness", useUnmergedTree = true).assertExists()
        val habit = habit()
        assertEquals("Read a book", habit.name)
        assertEquals("#1565C0", habit.color)
        assertEquals("Fitness", habit.labelName)
        assertTrue(habit.doneToday)
    }

    @Test
    fun selectingEmoji_savesAndRendersItOnTheCard() {
        openEditor()
        composeRule.onNode(inDialog("📚")).performClick()
        composeRule.onNode(saveButton).performClick()

        waitFor(hasText("📚"))
        composeRule.onNodeWithText("Edit habit").assertDoesNotExist()
        composeRule.onNodeWithText("📚").assertIsDisplayed()
        assertEquals("📚", habit().emoji)
    }

    @Test
    fun blankName_cannotBeSaved() {
        openEditor()

        composeRule.onNode(dialogTextField).performTextReplacement("   ")

        composeRule.onNode(saveButton).assertIsNotEnabled()
    }

    @Test
    fun cancel_leavesHabitUnchanged() {
        openEditor()
        composeRule.onNode(dialogTextField).performTextReplacement("Something else")

        composeRule.onNode(inDialog("Cancel")).performClick()

        composeRule.onNodeWithText("Edit habit").assertDoesNotExist()
        assertEquals("Read", habit().name)
    }

    @Test
    fun doneToggle_andDeleteButton_keepTheirOwnActions() {
        composeRule.onNodeWithContentDescription("Done today").performClick()

        composeRule.onNodeWithText("Edit habit").assertDoesNotExist()
        composeRule.waitUntil(timeoutMillis = 5_000) { !habit().doneToday }
        assertFalse(habit().doneToday)

        composeRule.onNodeWithContentDescription("Delete habit").performClick()

        composeRule.onNodeWithText("Delete habit?").assertIsDisplayed()
        composeRule.onNodeWithText("Edit habit").assertDoesNotExist()
    }
}
