package com.example.habit_tracker_2.notifications

import android.app.AlarmManager
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.habit_tracker_2.HabitApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
class ReminderSchedulerTest {

    private val app: HabitApplication = ApplicationProvider.getApplicationContext()
    private val alarms = shadowOf(app.getSystemService(AlarmManager::class.java))
    private val zone = ZoneId.of("America/New_York")
    private val eightPm = LocalTime.of(20, 0)

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int) =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone)

    @Test
    fun nextReminder_isLaterToday_ifTheTimeHasNotPassed() {
        assertEquals(at(2026, 9, 13, 20, 0), nextReminderAt(at(2026, 9, 13, 9, 30), eightPm))
    }

    @Test
    fun nextReminder_isTomorrow_onceTheTimeHasPassedOrIsNow() {
        assertEquals(at(2026, 9, 14, 20, 0), nextReminderAt(at(2026, 9, 13, 21, 0), eightPm))
        assertEquals(at(2026, 9, 14, 20, 0), nextReminderAt(at(2026, 9, 13, 20, 0), eightPm))
    }

    @Test
    fun nextReminder_skippedByDaylightSaving_comesJustAfterTheGap() {
        // Clocks jump from 2:00 to 3:00 on March 8, 2026 in New York.
        val next = nextReminderAt(at(2026, 3, 7, 23, 0), LocalTime.of(2, 30))

        assertEquals(LocalDateTime.of(2026, 3, 8, 3, 30), next.toLocalDateTime())
    }

    @Test
    fun schedule_setsWakeUpAlarm_andCancelRemovesIt() {
        val now = at(2026, 9, 13, 9, 30)
        val scheduler = ReminderScheduler(app) { now }

        scheduler.schedule(eightPm)

        val alarm = requireAlarm()
        assertEquals(AlarmManager.RTC_WAKEUP, alarm.getType())
        assertEquals(at(2026, 9, 13, 20, 0).toInstant().toEpochMilli(), alarm.triggerAtMs)
        assertTrue(alarm.isAllowWhileIdle)

        scheduler.cancel()

        assertNull(alarms.peekNextScheduledAlarm())
    }

    @Test
    fun app_schedulesAndCancelsTheAlarm_asTheReminderIsTurnedOnAndOff() {
        assertNull(alarms.peekNextScheduledAlarm())

        app.preferencesStore.setReminderEnabled(true)
        shadowOf(Looper.getMainLooper()).idle()

        assertNotNull(alarms.peekNextScheduledAlarm())

        app.preferencesStore.setReminderEnabled(false)
        shadowOf(Looper.getMainLooper()).idle()

        assertNull(alarms.peekNextScheduledAlarm())
    }

    private fun requireAlarm() = alarms.peekNextScheduledAlarm() ?: throw AssertionError("Expected a scheduled alarm")
}
