package com.jaspermsnbk.habit_tracker.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jaspermsnbk.habit_tracker.data.HabitDatabase
import com.jaspermsnbk.habit_tracker.data.HabitRepository
import com.jaspermsnbk.habit_tracker.data.inMemoryDatabase
import com.jaspermsnbk.habit_tracker.data.testPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitListScreenDeleteTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: HabitDatabase
    private lateinit var repository: HabitRepository

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        repository = HabitRepository(db.habitDao())
        runBlocking { repository.addHabit("Read", "#2E7D32") }

        val viewModel = HabitViewModel(repository, testPreferences())
        composeRule.setContent { HabitListScreen(viewModel) }
        waitForText("Read", present = true)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun tappingDelete_showsConfirmationWithHabitName() {
        composeRule.onNodeWithContentDescription("Delete habit").performClick()

        composeRule.onNodeWithText("Delete habit?").assertIsDisplayed()
        composeRule.onNodeWithText("\"Read\" will be removed from your list.").assertIsDisplayed()
    }

    @Test
    fun cancel_keepsHabit() {
        composeRule.onNodeWithContentDescription("Delete habit").performClick()
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithText("Delete habit?").assertDoesNotExist()
        composeRule.onNodeWithText("Read").assertIsDisplayed()
        assertEquals(1, runBlocking { repository.habits.first() }.size)
    }

    @Test
    fun confirm_deletesHabit() {
        composeRule.onNodeWithContentDescription("Delete habit").performClick()
        composeRule.onNodeWithText("Delete").performClick()

        waitForText("Read", present = false)
        composeRule.onNodeWithText("Delete habit?").assertDoesNotExist()
        assertEquals(0, runBlocking { repository.habits.first() }.size)
    }

    /** Room emits on a background thread, so wait for the UI to catch up. */
    private fun waitForText(text: String, present: Boolean) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() == present
        }
    }
}
