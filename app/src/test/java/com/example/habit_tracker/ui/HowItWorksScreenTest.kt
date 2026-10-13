package com.jaspermsnbk.habit_tracker.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jaspermsnbk.habit_tracker.data.FED_RATIO
import com.jaspermsnbk.habit_tracker.data.FED_DAYS_PER_FREEZE
import com.jaspermsnbk.habit_tracker.data.MAX_FORGIVABLE_GAP_DAYS
import com.jaspermsnbk.habit_tracker.data.MISSED_DAY_DAMAGE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HowItWorksScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun firstRun_welcomesYou_andHasNoBackButton() {
        var done = 0
        composeRule.setContent { HowItWorksScreen(firstRun = true, onDone = { done++ }) }

        composeRule.onNodeWithText("Meet your pet").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()

        composeRule.onNodeWithText("Got it, let's go").performClick()

        assertEquals(1, done)
    }

    @Test
    fun asHelp_hasABackButton_andNoStartButton() {
        var done = 0
        composeRule.setContent { HowItWorksScreen(firstRun = false, onDone = { done++ }) }

        composeRule.onNodeWithText("Your pet, explained").assertIsDisplayed()
        composeRule.onNodeWithText("Got it, let's go").assertDoesNotExist()

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, done)
    }

    @Test
    fun explainsFeedingMissesFreezesComingBackAndLevelling() {
        composeRule.setContent { HowItWorksScreen(firstRun = false, onDone = {}) }

        listOf("Feeding it", "Missing a day", "Freeze tokens", "Coming back", "Levelling up").forEach {
            composeRule.onNodeWithText(it).assertExists()
        }
    }

    // The copy says "at least half"; if the rule changes, the words must change with it.
    @Test
    fun copy_matchesTheRules() {
        assertEquals(0.5, FED_RATIO, 0.0)
        val copy = PetRulesCopy()
        assertTrue(copy.missed.contains("$MISSED_DAY_DAMAGE health"))
        assertTrue(copy.freezes.contains("$FED_DAYS_PER_FREEZE fed days"))
        assertTrue(copy.forgiveness.contains("$MAX_FORGIVABLE_GAP_DAYS days"))
    }
}
