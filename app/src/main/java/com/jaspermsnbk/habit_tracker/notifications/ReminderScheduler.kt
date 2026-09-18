package com.jaspermsnbk.habit_tracker.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Sets the once-a-day alarm that fires [ReminderReceiver]. Each firing schedules the next one
 * instead of using a repeating alarm, so a changed clock or time zone is picked up.
 *
 * @param now the current time in the phone's time zone; replaceable for tests
 */
class ReminderScheduler(
    private val context: Context,
    private val now: () -> ZonedDateTime = { ZonedDateTime.now() },
) {
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    /** Schedules the next reminder at [time], replacing any already scheduled. */
    fun schedule(time: LocalTime) {
        val triggerAt = nextReminderAt(now(), time)
        // Inexact, so no exact-alarm permission is needed; Android may deliver it a few minutes late.
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt.toInstant().toEpochMilli(),
            reminderIntent(),
        )
    }

    fun cancel() {
        alarmManager.cancel(reminderIntent())
    }

    private fun reminderIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_REMIND),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/**
 * The first moment at [time] strictly after [now], in [now]'s time zone. On a day when [time]
 * is skipped by a daylight saving change, it falls just after the gap.
 */
internal fun nextReminderAt(now: ZonedDateTime, time: LocalTime): ZonedDateTime {
    val today = now.toLocalDate().atTime(time).atZone(now.zone)
    return if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(time).atZone(now.zone)
}
