package com.jaspermsnbk.habit_tracker.notifications

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jaspermsnbk.habit_tracker.data.HabitUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ReminderNotifierTest {

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val notifications = shadowOf(app.getSystemService(NotificationManager::class.java))

    private fun habit(name: String, streak: Int = 0) = HabitUi(
        id = name,
        name = name,
        color = "#2E7D32",
        emoji = null,
        doneToday = false,
        currentStreak = streak,
        last7 = List(7) { false },
        completedDates = emptySet(),
        labelId = null,
        labelName = null,
        createdOn = LocalDate.of(2026, 9, 1),
    )

    @Test
    fun content_isNull_whenEverythingIsDone() {
        assertNull(reminderContent(emptyList()))
    }

    @Test
    fun content_forOneHabit() {
        assertEquals(
            ReminderContent("1 habit left today", "Still to do: Read."),
            reminderContent(listOf(habit("Read"))),
        )
    }

    @Test
    fun content_listsHabits_andNamesTheLongestStreakAtRisk() {
        assertEquals(
            ReminderContent(
                "3 habits left today",
                "Still to do: Read, Run and Water. Finish Water to keep your 5-day streak.",
            ),
            reminderContent(listOf(habit("Read", streak = 2), habit("Run"), habit("Water", streak = 5))),
        )
    }

    @Test
    fun show_postsReminder_whenNotificationsAreAllowed() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        ReminderNotifier.show(app, listOf(habit("Read"), habit("Run")))

        val posted = notifications.allNotifications.single()
        assertEquals(ReminderNotifier.CHANNEL_ID, posted.channelId)
        assertEquals("2 habits left today", posted.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals("Still to do: Read and Run.", posted.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
    }

    @Test
    fun show_postsNothing_withoutPermission() {
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        ReminderNotifier.show(app, listOf(habit("Read")))

        assertTrue(notifications.allNotifications.isEmpty())
    }

    @Test
    fun show_postsNothing_whenEverythingIsDone() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        ReminderNotifier.show(app, emptyList())

        assertTrue(notifications.allNotifications.isEmpty())
    }
}
