package com.jaspermsnbk.habit_tracker.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jaspermsnbk.habit_tracker.MainActivity
import com.jaspermsnbk.habit_tracker.R
import com.jaspermsnbk.habit_tracker.data.HabitUi

/** Builds and posts the daily reminder notification. */
object ReminderNotifier {

    const val CHANNEL_ID = "daily_reminder"
    private const val NOTIFICATION_ID = 1

    /** Registers the reminder channel, which people can also adjust in system settings. */
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Daily reminder",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "A reminder about habits you haven't done yet today" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Whether a reminder would be shown: permission granted, and neither the app nor its channel blocked. */
    fun canNotify(context: Context): Boolean {
        val manager = NotificationManagerCompat.from(context)
        val permitted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        val channelBlocked = manager.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE
        return permitted && manager.areNotificationsEnabled() && !channelBlocked
    }

    /** Shows a reminder listing [habitsLeft]. Shows nothing if they're all done or reminders are blocked. */
    @SuppressLint("MissingPermission") // canNotify checks it.
    fun show(context: Context, habitsLeft: List<HabitUi>) {
        val content = reminderContent(habitsLeft) ?: return
        if (!canNotify(context)) return

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(content.title)
            .setContentText(content.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}

internal data class ReminderContent(val title: String, val text: String)

/** What the reminder says about [habitsLeft], or null when nothing is left to do today. */
internal fun reminderContent(habitsLeft: List<HabitUi>): ReminderContent? {
    if (habitsLeft.isEmpty()) return null

    val title = if (habitsLeft.size == 1) "1 habit left today" else "${habitsLeft.size} habits left today"
    val names = habitsLeft.map { it.name }
    val list = if (names.size == 1) names.single() else names.dropLast(1).joinToString(", ") + " and " + names.last()
    // A habit not done today still has its streak through yesterday, which ends tonight.
    val atRisk = habitsLeft.filter { it.currentStreak > 0 }.maxByOrNull { it.currentStreak }
    val text = "Still to do: $list." +
        if (atRisk != null) " Finish ${atRisk.name} to keep your ${atRisk.currentStreak}-day streak." else ""
    return ReminderContent(title, text)
}
