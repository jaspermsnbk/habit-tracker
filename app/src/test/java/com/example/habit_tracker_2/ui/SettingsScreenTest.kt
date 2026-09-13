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
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.habit_tracker_2.data.HabitDatabase
import com.example.habit_tracker_2.data.HabitRepository
import com.example.habit_tracker_2.data.inMemoryDatabase
import com.example.habit_tracker_2.data.testPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: HabitDatabase
    private var backPresses = 0

    @Before
    fun setUp() {
        // These tests never wait for habit data, so keep queries from outliving them.
        db = inMemoryDatabase(synchronous = true)
        val viewModel = HabitViewModel(HabitRepository(db.habitDao()), testPreferences())
        composeRule.setContent { SettingsScreen(viewModel, onBack = { backPresses++ }) }
    }

    @After
    fun tearDown() {
        db.close()
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
    fun labelsSection_withNoLabels_explainsThem() {
        composeRule.onNodeWithText("No labels yet. $NEW_LABEL_DESCRIPTION").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun aboutSection_showsAppVersion() {
        composeRule.onNode(hasText("About") and isHeading()).performScrollTo()
        composeRule.onNode(hasText("Version") and hasText("1.0")).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun backArrow_callsOnBack() {
        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backPresses)
    }
}
