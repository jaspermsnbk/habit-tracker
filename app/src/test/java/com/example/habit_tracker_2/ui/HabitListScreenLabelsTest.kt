package com.example.habit_tracker_2.ui

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.habit_tracker_2.data.HabitDatabase
import com.example.habit_tracker_2.data.HabitRepository
import com.example.habit_tracker_2.data.inMemoryDatabase
import com.example.habit_tracker_2.data.testPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitListScreenLabelsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: HabitDatabase
    private lateinit var repository: HabitRepository

    private val dialogTextField = hasSetTextAction() and hasAnyAncestor(isDialog())
    private val dialogAddButton = hasText("Add") and hasAnyAncestor(isDialog())

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        repository = HabitRepository(db.habitDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun speedDial_revealsAddHabitAndAddLabel() {
        launch()
        composeRule.onNodeWithText("Add label").assertDoesNotExist()

        composeRule.onNodeWithContentDescription("Add").performClick()

        composeRule.onNodeWithText("Add habit").assertIsDisplayed()
        composeRule.onNodeWithText("Add label").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Close add menu").performClick()

        composeRule.onNodeWithText("Add label").assertDoesNotExist()
    }

    @Test
    fun addLabel_fromSpeedDial_createsLabelAndFilterChip() {
        launch()

        openSpeedDialItem("Add label")
        composeRule.onNode(dialogTextField).performTextInput("  Fitness ")
        composeRule.onNode(dialogAddButton).performClick()

        waitFor(hasText("Fitness"))
        composeRule.onNode(isDialog()).assertDoesNotExist()
        composeRule.onNodeWithText("All labels").assertIsDisplayed()
        assertEquals(listOf("Fitness"), runBlocking { repository.labels.first() }.map { it.name })
    }

    @Test
    fun addLabel_withExistingNameIgnoringCase_isBlocked() {
        seedLabel("Fitness")
        launch()
        waitFor(hasText("Fitness"))

        openSpeedDialItem("Add label")
        composeRule.onNode(dialogTextField).performTextInput("fitness")

        composeRule.onNodeWithText("A label with this name already exists").assertIsDisplayed()
        composeRule.onNode(dialogAddButton).assertIsNotEnabled()
    }

    @Test
    fun addHabit_withLabel_showsLabelOnCard() {
        seedLabel("Fitness")
        launch()
        waitFor(hasText("Fitness"))

        openSpeedDialItem("Add habit")
        composeRule.onNode(dialogTextField).performTextInput("Run")
        composeRule.onNode(hasText("Fitness") and hasAnyAncestor(isDialog())).performClick()
        composeRule.onNode(dialogAddButton).performClick()

        waitFor(hasContentDescription("Label: Fitness"))
        assertEquals("Fitness", runBlocking { repository.habits.first() }.single().labelName)
    }

    @Test
    fun filteringByLabel_showsOnlyThatLabelsHabits() {
        val fitness = seedLabel("Fitness")
        val learning = seedLabel("Learning")
        seedLabel("Mindfulness")
        runBlocking {
            repository.addHabit("Run", "#2E7D32", fitness)
            repository.addHabit("Read", "#1565C0", learning)
            repository.addHabit("Water", "#00838F")
        }
        launch()
        waitFor(hasText("Water"))

        composeRule.onNodeWithText("Fitness").performClick()

        composeRule.onNodeWithText("Run").assertIsDisplayed()
        composeRule.onNodeWithText("Read").assertDoesNotExist()
        composeRule.onNodeWithText("Water").assertDoesNotExist()

        composeRule.onNodeWithText("Mindfulness").performScrollTo().performClick()

        composeRule.onNodeWithText("No habits with this label yet.").assertIsDisplayed()

        composeRule.onNodeWithText("All labels").performScrollTo().performClick()

        composeRule.onNodeWithText("Water").assertIsDisplayed()
    }

    private fun launch() {
        val viewModel = HabitViewModel(repository, testPreferences())
        composeRule.setContent { HabitListScreen(viewModel) }
    }

    private fun seedLabel(name: String): String = runBlocking {
        repository.addLabel(name)
        repository.labels.first().first { it.name == name }.id
    }

    private fun openSpeedDialItem(text: String) {
        composeRule.onNodeWithContentDescription("Add").performClick()
        composeRule.onNodeWithText(text).performClick()
    }

    /** Room emits on a background thread, so wait for the UI to catch up. */
    private fun waitFor(matcher: SemanticsMatcher) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
