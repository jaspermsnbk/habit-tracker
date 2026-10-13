package com.jaspermsnbk.habit_tracker.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Opens the "How it works" screen. Provided by [HabitApp]; null elsewhere, such as screens shown
 * on their own in tests, which then have no help button.
 */
val LocalOpenHelp = staticCompositionLocalOf<(() -> Unit)?> { null }

/** The app bar shared by every tab, so the settings button sits in the same place on each. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitTopBar(title: String, onOpenSettings: () -> Unit) {
    val openHelp = LocalOpenHelp.current
    TopAppBar(
        title = { Text(title) },
        actions = {
            if (openHelp != null) {
                IconButton(onClick = openHelp) {
                    Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = "How it works")
                }
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.AccountCircle, contentDescription = "Settings")
            }
        },
    )
}
