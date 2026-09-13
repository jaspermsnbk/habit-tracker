package com.example.habit_tracker_2

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val app: HabitApplication = ApplicationProvider.getApplicationContext()

    @Test
    fun firstLaunch_showsLanding_thenGuestEntersHabitList() {
        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.onNodeWithText("Continue as guest").performClick()

            composeRule.onNodeWithText("Habits").assertIsDisplayed()
            composeRule.onNodeWithText("Continue as guest").assertDoesNotExist()
            assertTrue(app.sessionStore.isGuest)
        }
    }

    @Test
    fun returningGuest_skipsLanding() {
        app.sessionStore.continueAsGuest()

        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.onNodeWithText("Continue as guest").assertDoesNotExist()
            composeRule.onNodeWithContentDescription("Add habit").assertIsDisplayed()
        }
    }

    @Test
    fun guestChoice_survivesRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            composeRule.onNodeWithText("Continue as guest").performClick()

            scenario.recreate()

            composeRule.onNodeWithText("Continue as guest").assertDoesNotExist()
            composeRule.onNodeWithText("Habits").assertIsDisplayed()
        }
    }
}
