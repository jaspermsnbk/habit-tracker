package com.example.habit_tracker_2

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val app: HabitApplication = ApplicationProvider.getApplicationContext()

    // The app's database is a process-wide singleton, so habits added here would leak into
    // other tests that expect an empty list.
    @After
    fun tearDown() = runBlocking {
        app.repository.habits.first().forEach { app.repository.deleteHabit(it.id) }
    }

    @Test
    fun firstLaunch_showsLanding_thenGuestEntersHabitList() {
        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.onNodeWithText("Continue as guest").performClick()

            composeRule.onNodeWithContentDescription("Add").assertIsDisplayed()
            composeRule.onNodeWithText("Continue as guest").assertDoesNotExist()
            assertTrue(app.sessionStore.isGuest)
        }
    }

    @Test
    fun returningGuest_skipsLanding() {
        app.sessionStore.continueAsGuest()

        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.onNodeWithText("Continue as guest").assertDoesNotExist()
            composeRule.onNodeWithContentDescription("Add").assertIsDisplayed()
        }
    }

    @Test
    fun guestChoice_survivesRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            composeRule.onNodeWithText("Continue as guest").performClick()

            scenario.recreate()

            composeRule.onNodeWithText("Continue as guest").assertDoesNotExist()
            composeRule.onNodeWithContentDescription("Add").assertIsDisplayed()
        }
    }

    @Test
    fun navigationBar_switchesBetweenHabitsAndCalendar() {
        app.sessionStore.continueAsGuest()

        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.onNode(hasText("Calendar") and hasClickAction()).performClick()

            composeRule.onNodeWithText("Add a habit to see your history here.").assertIsDisplayed()
            composeRule.onNodeWithContentDescription("Add").assertDoesNotExist()

            composeRule.onNode(hasText("Habits") and hasClickAction()).performClick()

            composeRule.onNodeWithContentDescription("Add").assertIsDisplayed()
        }
    }

    @Test
    fun navigationBar_opensTrends() {
        app.sessionStore.continueAsGuest()

        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.onNode(hasText("Trends") and hasClickAction()).performClick()

            composeRule.onNodeWithText("Add a habit to see your trends here.").assertIsDisplayed()
            composeRule.onNodeWithContentDescription("Add").assertDoesNotExist()
        }
    }

    @Test
    fun settingsButton_opensSettings_andBackArrowReturnsToSameTab() {
        app.sessionStore.continueAsGuest()

        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.onNode(hasText("Calendar") and hasClickAction()).performClick()
            composeRule.onNodeWithContentDescription("Settings").performClick()

            composeRule.onNodeWithText("Settings").assertIsDisplayed()
            // Settings covers the whole app, bottom bar included.
            composeRule.onNode(hasText("Calendar") and hasClickAction()).assertDoesNotExist()

            composeRule.onNodeWithContentDescription("Back").performClick()

            composeRule.onNodeWithText("Add a habit to see your history here.").assertIsDisplayed()
            composeRule.onNodeWithText("Settings").assertDoesNotExist()
        }
    }

    @Test
    fun settingsButton_isOnEveryTab() {
        app.sessionStore.continueAsGuest()

        ActivityScenario.launch(MainActivity::class.java).use {
            listOf("Habits", "Calendar", "Trends").forEach { tab ->
                composeRule.onNode(hasText(tab) and hasClickAction()).performClick()
                composeRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
            }
        }
    }

    @Test
    fun systemBack_closesSettings() {
        app.sessionStore.continueAsGuest()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            composeRule.onNodeWithContentDescription("Settings").performClick()
            composeRule.onNodeWithText("Settings").assertIsDisplayed()

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }

            composeRule.onNodeWithContentDescription("Add").assertIsDisplayed()
        }
    }

    @Test
    fun tabState_survivesSettingsAndTabSwitches() {
        app.sessionStore.continueAsGuest()
        runBlocking { app.repository.addHabit("Read", "#2E7D32") }

        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.onNode(hasText("Trends") and hasClickAction()).performClick()
            composeRule.onNodeWithText("1Y").performClick()
            composeRule.onNodeWithText("Check-ins per week").assertExists()

            composeRule.onNodeWithContentDescription("Settings").performClick()
            composeRule.onNodeWithContentDescription("Back").performClick()

            composeRule.onNodeWithText("Check-ins per week").assertExists()

            composeRule.onNode(hasText("Habits") and hasClickAction()).performClick()
            composeRule.onNode(hasText("Trends") and hasClickAction()).performClick()

            composeRule.onNodeWithText("Check-ins per week").assertExists()
        }
    }
}
