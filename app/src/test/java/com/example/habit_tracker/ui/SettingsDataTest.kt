package com.jaspermsnbk.habit_tracker.ui

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jaspermsnbk.habit_tracker.data.HabitDatabase
import com.jaspermsnbk.habit_tracker.data.HabitRepository
import com.jaspermsnbk.habit_tracker.data.inMemoryDatabase
import com.jaspermsnbk.habit_tracker.data.testPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsDataTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: HabitDatabase
    private lateinit var repository: HabitRepository

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        repository = HabitRepository(db.habitDao())
        runBlocking {
            repository.addLabel("Fitness")
            val label = repository.labels.first().single()
            repository.addHabit("Run", "#2E7D32", label.id)
        }

        val viewModel = HabitViewModel(repository, testPreferences())
        composeRule.setContent { SettingsScreen(viewModel, onBack = {}) }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Fitness").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun deleteAllData_asksFirst() {
        composeRule.onNodeWithText("Delete all data").performScrollTo().performClick()

        composeRule.onNodeWithText("Delete all data?").assertIsDisplayed()
        composeRule.onNodeWithText("All habits, completions and labels will be permanently deleted. This can't be undone.")
            .assertIsDisplayed()
        assertTrue(runBlocking { repository.habits.first() }.isNotEmpty())
    }

    @Test
    fun confirmingDelete_wipesHabitsAndLabels() {
        composeRule.onNodeWithText("Delete all data").performScrollTo().performClick()
        composeRule.onNodeWithText("Delete").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { repository.habits.first() }.isEmpty()
        }
        assertTrue(runBlocking { repository.labels.first() }.isEmpty())
    }

    @Test
    fun cancellingDelete_keepsData() {
        composeRule.onNodeWithText("Delete all data").performScrollTo().performClick()

        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithText("Delete all data?").assertDoesNotExist()
        assertTrue(runBlocking { repository.habits.first() }.isNotEmpty())
    }
}
