package com.example.habit_tracker_2.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LandingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsTitleAndGuestButton() {
        composeRule.setContent { LandingScreen(onContinueAsGuest = {}) }

        composeRule.onNodeWithText("Habit Tracker").assertIsDisplayed()
        composeRule.onNodeWithText("Continue as guest").assertIsDisplayed()
    }

    @Test
    fun tappingContinueAsGuest_invokesCallbackOnce() {
        var calls = 0
        composeRule.setContent { LandingScreen(onContinueAsGuest = { calls++ }) }

        composeRule.onNodeWithText("Continue as guest").performClick()

        assertEquals(1, calls)
    }
}
