package com.jaspermsnbk.habit_tracker.ui

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp

/**
 * Scrolls the habit dialog to its bottom, where the labels are. The dialog is taller than a small
 * screen, and `performScrollTo` would scroll the label row sideways instead of the dialog.
 */
fun ComposeTestRule.scrollHabitDialogToBottom() {
    val dialogColumn: SemanticsMatcher = hasScrollAction() and hasAnyDescendant(hasText("Color")) and hasAnyAncestor(isDialog())
    onNode(dialogColumn).performTouchInput { swipeUp() }
}
