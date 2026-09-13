package com.example.habit_tracker_2.data

import android.content.Context
import androidx.core.content.edit

/**
 * Remembers how the user entered the app. For now the only option is guest; in Phase 4
 * this is where the device-generated guestId and auth token will be stored.
 */
class SessionStore(context: Context) {

    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    val isGuest: Boolean
        get() = prefs.getBoolean(KEY_GUEST, false)

    fun continueAsGuest() {
        prefs.edit { putBoolean(KEY_GUEST, true) }
    }

    private companion object {
        const val KEY_GUEST = "guest"
    }
}
