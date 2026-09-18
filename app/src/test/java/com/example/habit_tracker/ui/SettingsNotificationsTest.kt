package com.jaspermsnbk.habit_tracker.ui

import android.Manifest
import android.app.Application
import android.text.format.DateFormat
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jaspermsnbk.habit_tracker.data.DEFAULT_REMINDER_TIME
import com.jaspermsnbk.habit_tracker.data.HabitDatabase
import com.jaspermsnbk.habit_tracker.data.HabitRepository
import com.jaspermsnbk.habit_tracker.data.PreferencesStore
import com.jaspermsnbk.habit_tracker.data.inMemoryDatabase
import com.jaspermsnbk.habit_tracker.data.testPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class SettingsNotificationsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private lateinit var db: HabitDatabase
    private val preferences: PreferencesStore = testPreferences()

    private val reminderRow get() = composeRule.onNode(hasText("Daily reminder"))
    private val timeRow get() = composeRule.onNode(hasText("Reminder time") and hasClickAction())

    @Before
    fun setUp() {
        // These tests never wait for habit data, so keep queries from outliving them.
        db = inMemoryDatabase(synchronous = true)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun launch() {
        val viewModel = HabitViewModel(HabitRepository(db.habitDao()), preferences)
        composeRule.setContent { SettingsScreen(viewModel, onBack = {}) }
    }

    private fun allowNotifications(allowed: Boolean) {
        if (allowed) {
            shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun timeText(time: LocalTime) = time.format(reminderTimeFormatter(DateFormat.is24HourFormat(app)))

    @Test
    fun reminder_isOffByDefault_withTheTimeGreyedOut() {
        launch()

        reminderRow.performScrollTo().assertIsOff()
        timeRow.performScrollTo().assertIsNotEnabled()
        composeRule.onNode(hasText("Reminder time") and hasText(timeText(DEFAULT_REMINDER_TIME))).assertExists()
    }

    @Test
    fun turningOn_withNotificationsAllowed_savesIt() {
        allowNotifications(true)
        launch()

        reminderRow.performScrollTo().performClick()

        assertTrue(preferences.preferences.value.reminderEnabled)
        reminderRow.assertIsOn()
        timeRow.assertIsEnabled()
        composeRule.onNodeWithText("Every day at ${timeText(DEFAULT_REMINDER_TIME)}, if any habits are left")
            .assertExists()
        composeRule.onNodeWithText("Notifications are off").assertDoesNotExist()
    }

    @Test
    fun turningOn_withoutPermission_asksInsteadOfTurningOn() {
        allowNotifications(false)
        launch()

        reminderRow.performScrollTo().performClick()

        assertFalse(preferences.preferences.value.reminderEnabled)
        reminderRow.assertIsOff()
    }

    @Test
    fun turningOff_savesIt() {
        allowNotifications(true)
        preferences.setReminderEnabled(true)
        launch()

        reminderRow.performScrollTo().performClick()

        assertFalse(preferences.preferences.value.reminderEnabled)
    }

    @Test
    fun reminderOn_butNotificationsBlocked_explainsHowToFixIt() {
        allowNotifications(false)
        preferences.setReminderEnabled(true)
        launch()

        composeRule.onNodeWithText("Notifications are off").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Open settings").assertIsDisplayed()
    }

    @Test
    fun timeRow_opensPicker_andSetKeepsTheChosenTime() {
        allowNotifications(true)
        preferences.setReminderEnabled(true)
        preferences.setReminderTime(LocalTime.of(7, 15))
        launch()

        timeRow.performScrollTo().performClick()
        composeRule.onNode(hasText("Reminder time") and hasAnyAncestor(isDialog())).assertIsDisplayed()
        composeRule.onNode(hasText("Set") and hasAnyAncestor(isDialog())).performClick()

        composeRule.onNode(hasText("Reminder time") and hasAnyAncestor(isDialog())).assertDoesNotExist()
        assertEquals(LocalTime.of(7, 15), preferences.preferences.value.reminderTime)
        composeRule.onNode(hasText("Reminder time") and hasText(timeText(LocalTime.of(7, 15)))).assertExists()
    }

    @Test
    fun timeDialog_cancelLeavesTimeUnchanged() {
        allowNotifications(true)
        preferences.setReminderEnabled(true)
        launch()

        timeRow.performScrollTo().performClick()
        composeRule.onNode(hasText("Cancel") and hasAnyAncestor(isDialog())).performClick()

        composeRule.onNode(hasText("Reminder time") and hasAnyAncestor(isDialog())).assertDoesNotExist()
        assertEquals(DEFAULT_REMINDER_TIME, preferences.preferences.value.reminderTime)
    }
}
