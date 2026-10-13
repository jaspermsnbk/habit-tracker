package com.jaspermsnbk.habit_tracker.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jaspermsnbk.habit_tracker.data.HabitDatabase
import com.jaspermsnbk.habit_tracker.data.HabitRepository
import com.jaspermsnbk.habit_tracker.data.inMemoryDatabase
import com.jaspermsnbk.habit_tracker.data.testPreferences
import com.jaspermsnbk.habit_tracker.data.waitUntilSettled
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// A tall screen, so the header and the first widgets are on screen together.
@Config(qualifiers = "w400dp-h1400dp")
@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: HabitDatabase
    private lateinit var repository: HabitRepository

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        repository = HabitRepository(db.habitDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun launch(onOpenPet: () -> Unit = {}) {
        val viewModel = HabitViewModel(repository, testPreferences())
        composeRule.setContent { ProfileScreen(viewModel, onOpenPet = onOpenPet) }
    }

    @Test
    fun withoutAPetOrHabits_invitesYouToAdoptOne_andShowsAnEmptyTrends() {
        launch()

        composeRule.onNodeWithText("No pet yet").assertIsDisplayed()
        composeRule.onNodeWithText("Trends").assertIsDisplayed()
        composeRule.onNodeWithText("Add a habit to see your trends here.").assertIsDisplayed()
    }

    @Test
    fun showsThePetAndItsLevel_andOpensThePetTabWhenTapped() {
        runBlocking { repository.adoptPet("Momo", "Pip") }
        var opened = 0
        launch(onOpenPet = { opened++ })

        composeRule.waitUntilSettled { composeRule.onAllNodesWithText("Momo").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Level 1 · Egg").assertIsDisplayed()

        composeRule.onNodeWithText("Momo").performClick()

        assertEquals(1, opened)
    }

    @Test
    fun trendsAreCardsUnderTheHeader() {
        runBlocking { repository.addHabit("Read", "#2E7D32") }
        launch()

        composeRule.waitUntilSettled { composeRule.onAllNodesWithText("Read").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Check-ins per day").assertIsDisplayed()
    }
}
