package com.jaspermsnbk.habit_tracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

private enum class Tab(val label: String, val icon: ImageVector) {
    Habits("Habits", Icons.Filled.CheckCircle),
    Calendar("Calendar", Icons.Filled.CalendarMonth),
    Trends("Trends", Icons.Filled.Insights),
}

/**
 * Top-level shell once the user is in: a bottom navigation bar switching between screens,
 * with settings opening over all of them.
 */
@Composable
fun HabitApp(viewModel: HabitViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.Habits) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    // Keeps each tab's filters, month and range while it's off screen, including behind settings.
    val tabStates = rememberSaveableStateHolder()

    BackHandler(enabled = showSettings) { showSettings = false }

    if (showSettings) {
        SettingsScreen(viewModel, onBack = { showSettings = false })
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
        // Each screen has its own Scaffold and top bar, which handle the status bar inset.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        val screenModifier = Modifier
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
        val openSettings = { showSettings = true }
        tabStates.SaveableStateProvider(tab.name) {
            when (tab) {
                Tab.Habits -> HabitListScreen(viewModel, screenModifier, openSettings)
                Tab.Calendar -> CalendarScreen(viewModel, screenModifier, openSettings)
                Tab.Trends -> TrendsScreen(viewModel, screenModifier, openSettings)
            }
        }
    }
}
