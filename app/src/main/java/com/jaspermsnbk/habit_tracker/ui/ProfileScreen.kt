package com.jaspermsnbk.habit_tracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jaspermsnbk.habit_tracker.data.PetUi
import com.jaspermsnbk.habit_tracker.ui.pet.PetCanvas

/**
 * You: your pet at a glance, then the trends as cards to scroll through. Tapping the pet opens the Pet tab.
 */
@Composable
fun ProfileScreen(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
    onOpenPet: () -> Unit = {},
) {
    val pet by viewModel.pet.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = { HabitTopBar("Profile", onOpenSettings) },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProfileHeader(pet, onOpenPet)
            TrendsWidgets(viewModel)
        }
    }
}

@Composable
private fun ProfileHeader(pet: PetUi?, onOpenPet: () -> Unit) {
    Card(
        onClick = onOpenPet,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (pet == null) {
                Column {
                    Text("No pet yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Choose one on the Pet tab and your habits will feed it.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                PetCanvas(pet.look(), Modifier.size(72.dp))
                Column {
                    Text(pet.name, style = MaterialTheme.typography.titleLarge)
                    if (pet.state.alive) {
                        Text("Level ${pet.state.level} · ${pet.stageName()}", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("Needs a new owner", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
