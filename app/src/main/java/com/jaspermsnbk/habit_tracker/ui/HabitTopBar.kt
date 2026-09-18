package com.jaspermsnbk.habit_tracker.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable

/** The app bar shared by every tab, so the settings button sits in the same place on each. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitTopBar(title: String, onOpenSettings: () -> Unit) {
    TopAppBar(
        title = { Text(title) },
        actions = {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.AccountCircle, contentDescription = "Settings")
            }
        },
    )
}
