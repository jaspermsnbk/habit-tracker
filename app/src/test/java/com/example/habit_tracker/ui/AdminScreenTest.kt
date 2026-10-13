package com.jaspermsnbk.habit_tracker.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasScrollAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jaspermsnbk.habit_tracker.ui.pet.Species
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// A tall screen, so every chip in the preview card is on screen to be clicked.
@Config(qualifiers = "w400dp-h1600dp")
@RunWith(AndroidJUnit4::class)
class AdminScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsThePreview_andStartsOnTheAdultMochi() {
        composeRule.setContent { AdminScreen() }

        composeRule.onNodeWithText("Admin").assertIsDisplayed()
        composeRule.onNodeWithText("Preview").assertIsDisplayed()
        composeRule.onNodeWithText("Grand Mochi the mochi, happy").assertIsDisplayed()
    }

    @Test
    fun choosingChips_changesThePreview() {
        composeRule.setContent { AdminScreen() }

        composeRule.onNode(hasText("Pip") and hasClickAction()).performClick()
        composeRule.onNode(hasText("Hungry") and hasClickAction()).performClick()

        composeRule.onNodeWithText("Plume Pip the pip, hungry").assertIsDisplayed()
    }

    @Test
    fun listsEveryEvolutionLine() {
        composeRule.setContent { AdminScreen() }
        val list = composeRule.onNode(hasScrollAction())

        Species.entries.forEach { species ->
            list.performScrollToNode(hasContentDescription("${species.label}: Egg, idle"))
            composeRule.onNode(hasContentDescription("${species.label}: Egg, idle")).assertExists()
        }
    }

    @Test
    fun stageStartLabel_namesTheFedDays() {
        assertEquals("Start", stageStartLabel(0))
        assertEquals("1 fed day", stageStartLabel(1))
        assertEquals("7 fed days", stageStartLabel(2))
        assertEquals("21 fed days", stageStartLabel(3))
    }
}
