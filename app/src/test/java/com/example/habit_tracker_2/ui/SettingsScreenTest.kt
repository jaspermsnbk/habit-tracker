package com.example.habit_tracker_2.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var backPresses = 0

    @Before
    fun setUp() {
        composeRule.setContent { SettingsScreen(onBack = { backPresses++ }) }
    }

    @Test
    fun accountSection_showsGuestAndWhereDataLives() {
        composeRule.onNode(hasText("Account") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithText("Guest").assertIsDisplayed()
        composeRule.onNodeWithText("Your habits are saved on this phone only. Signing in is coming soon.")
            .assertIsDisplayed()
    }

    @Test
    fun signIn_isNotAvailableYet() {
        composeRule.onNode(hasText("Sign in") and hasClickAction()).assertIsNotEnabled()
    }

    @Test
    fun backArrow_callsOnBack() {
        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backPresses)
    }
}
