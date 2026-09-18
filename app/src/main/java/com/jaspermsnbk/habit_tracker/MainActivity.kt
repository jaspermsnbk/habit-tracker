package com.jaspermsnbk.habit_tracker

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaspermsnbk.habit_tracker.data.ThemeMode
import com.jaspermsnbk.habit_tracker.ui.HabitApp
import com.jaspermsnbk.habit_tracker.ui.HabitViewModel
import com.jaspermsnbk.habit_tracker.ui.LandingScreen
import com.jaspermsnbk.habit_tracker.ui.theme.HabitTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as HabitApplication
        val sessionStore = app.sessionStore
        setContent {
            val preferences by app.preferencesStore.preferences.collectAsState()
            val darkTheme = when (preferences.theme) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            // System bar icons follow the app's theme, which can differ from the phone's.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    // The same scrims enableEdgeToEdge uses by default for 3-button navigation.
                    navigationBarStyle = SystemBarStyle.auto(
                        Color.argb(0xe6, 0xFF, 0xFF, 0xFF),
                        Color.argb(0x80, 0x1b, 0x1b, 0x1b),
                    ) { darkTheme },
                )
                onDispose {}
            }

            HabitTrackerTheme(darkTheme = darkTheme, dynamicColor = preferences.dynamicColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    var hasSession by rememberSaveable { mutableStateOf(sessionStore.isGuest) }
                    if (hasSession) {
                        val viewModel: HabitViewModel = viewModel(factory = HabitViewModel.Factory)
                        HabitApp(viewModel)
                    } else {
                        LandingScreen(
                            onContinueAsGuest = {
                                sessionStore.continueAsGuest()
                                hasSession = true
                            },
                        )
                    }
                }
            }
        }
    }
}
