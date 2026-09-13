package com.example.habit_tracker_2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habit_tracker_2.ui.HabitListScreen
import com.example.habit_tracker_2.ui.HabitViewModel
import com.example.habit_tracker_2.ui.LandingScreen
import com.example.habit_tracker_2.ui.theme.HabitTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val sessionStore = (application as HabitApplication).sessionStore
        setContent {
            HabitTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    var hasSession by rememberSaveable { mutableStateOf(sessionStore.isGuest) }
                    if (hasSession) {
                        val viewModel: HabitViewModel = viewModel(factory = HabitViewModel.Factory)
                        HabitListScreen(viewModel)
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
