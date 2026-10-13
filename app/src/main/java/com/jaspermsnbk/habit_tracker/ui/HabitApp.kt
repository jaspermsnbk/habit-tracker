package com.jaspermsnbk.habit_tracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel

private enum class Tab(val label: String, val icon: ImageVector) {
    Habits("Habits", Icons.Filled.CheckCircle),
    Pet("Pet", Icons.Filled.Pets),
    Calendar("Calendar", Icons.Filled.CalendarMonth),
    Profile("Profile", Icons.Filled.Person),
    Admin("Admin", Icons.Filled.AdminPanelSettings),
}

/**
 * Top-level shell once the user is in: a bottom navigation bar switching between screens,
 * with settings opening over all of them.
 */
@Composable
fun HabitApp(viewModel: HabitViewModel, shop: ShopViewModel = viewModel(factory = ShopViewModel.Factory)) {
    var tab by rememberSaveable { mutableStateOf(Tab.Habits) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    // Keeps each tab's filters, month and range while it's off screen, including behind settings.
    val tabStates = rememberSaveableStateHolder()

    var showHelp by rememberSaveable { mutableStateOf(false) }
    val preferences by viewModel.preferences.collectAsState()

    BackHandler(enabled = showSettings || showHelp) {
        if (showHelp) showHelp = false else showSettings = false
    }

    // The first time in, the pet is explained before anything else.
    if (!preferences.howItWorksSeen) {
        HowItWorksScreen(firstRun = true, onDone = viewModel::markHowItWorksSeen)
        return
    }

    if (showHelp) {
        HowItWorksScreen(firstRun = false, onDone = { showHelp = false })
        return
    }

    CompositionLocalProvider(LocalOpenHelp provides { showHelp = true }) {
        HabitShell(
            viewModel = viewModel,
            shop = shop,
            tab = tab,
            onTabChange = { tab = it },
            showSettings = showSettings,
            onShowSettings = { showSettings = it },
            tabStates = tabStates,
        )
    }
}

@Composable
private fun HabitShell(
    viewModel: HabitViewModel,
    shop: ShopViewModel,
    tab: Tab,
    onTabChange: (Tab) -> Unit,
    showSettings: Boolean,
    onShowSettings: (Boolean) -> Unit,
    tabStates: SaveableStateHolder,
) {
    if (showSettings) {
        SettingsScreen(viewModel, onBack = { onShowSettings(false) })
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { onTabChange(item) },
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
        val openSettings = { onShowSettings(true) }
        tabStates.SaveableStateProvider(tab.name) {
            when (tab) {
                Tab.Habits -> HabitListScreen(viewModel, screenModifier, openSettings)
                Tab.Pet -> PetScreen(viewModel, screenModifier, openSettings, shop)
                Tab.Calendar -> CalendarScreen(viewModel, screenModifier, openSettings)
                Tab.Profile -> ProfileScreen(viewModel, screenModifier, openSettings, onOpenPet = { onTabChange(Tab.Pet) }, shop = shop)
                Tab.Admin -> AdminScreen(screenModifier, openSettings)
            }
        }
    }
}
