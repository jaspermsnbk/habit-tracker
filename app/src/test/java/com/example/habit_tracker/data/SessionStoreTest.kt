package com.jaspermsnbk.habit_tracker.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun freshInstall_isNotGuest() {
        assertFalse(SessionStore(context).isGuest)
    }

    @Test
    fun continueAsGuest_setsGuest() {
        val store = SessionStore(context)

        store.continueAsGuest()

        assertTrue(store.isGuest)
    }

    @Test
    fun guestChoice_persistsAcrossInstances() {
        SessionStore(context).continueAsGuest()

        assertTrue(SessionStore(context).isGuest)
    }
}
