package com.jaspermsnbk.habit_tracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaspermsnbk.habit_tracker.data.PetMood
import com.jaspermsnbk.habit_tracker.data.PetUi

internal fun PetMood.face(): String = when (this) {
    PetMood.Happy -> "😸"
    PetMood.Content -> "😺"
    PetMood.Hungry -> "🙀"
    PetMood.Sad -> "😿"
    PetMood.Dead -> "🪦"
}

internal fun PetUi.message(): String = when (state.mood) {
    PetMood.Happy -> "$name is full and happy."
    PetMood.Content -> "$name is fed for today."
    PetMood.Hungry -> "$name is hungry. Do your habits to feed ${name}."
    PetMood.Sad -> "$name is wasting away. Feed ${name} soon!"
    PetMood.Dead -> "$name died. You can still bring ${name} back if you were only away a little while."
}

/** The pet tab: adopt a pet, then watch its health follow your habits. */
@Composable
fun PetScreen(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
) {
    val pet by viewModel.pet.collectAsState()
    var showAdopt by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = { HabitTopBar("Pet", onOpenSettings) },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val current = pet
            if (current == null) {
                Spacer(Modifier.height(48.dp))
                Text("🥚", fontSize = 96.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Adopt a pet and feed it by keeping up your habits.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { showAdopt = true }) { Text("Adopt a pet") }
            } else {
                PetContent(current, onForgive = viewModel::forgivePet, onAdoptNew = { showAdopt = true })
            }
        }
    }

    if (showAdopt) {
        LabelNameDialog(
            title = "Name your pet",
            confirmText = "Adopt",
            existingNames = emptyList(),
            onDismiss = { showAdopt = false },
            onConfirm = {
                viewModel.adoptPet(it)
                showAdopt = false
            },
            fieldLabel = "Pet name",
        )
    }
}

@Composable
private fun PetContent(pet: PetUi, onForgive: () -> Unit, onAdoptNew: () -> Unit) {
    val state = pet.state
    Text(pet.name, style = MaterialTheme.typography.headlineMedium)
    Text(state.mood.face(), fontSize = 96.sp, modifier = Modifier.padding(vertical = 8.dp))
    Text(pet.message(), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    Spacer(Modifier.height(16.dp))

    if (state.alive) {
        Text("Health ${state.health}/100", style = MaterialTheme.typography.labelLarge)
        LinearProgressIndicator(
            progress = { state.health / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Stat("${state.fedStreak}", "days fed in a row")
            Stat("${state.freezes}", if (state.freezes == 1) "freeze" else "freezes")
        }
    }

    if (state.canForgive) {
        Spacer(Modifier.height(24.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Welcome back!", style = MaterialTheme.typography.titleMedium)
                Text(
                    "You missed ${state.forgivableDays.size} " +
                        "${if (state.forgivableDays.size == 1) "day" else "days"}. " +
                        "Forgive them and ${pet.name} won't lose any health. You can do this once every 2 weeks.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = onForgive) { Text("Forgive missed days") }
            }
        }
    }

    if (!state.alive) {
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onAdoptNew) { Text("Adopt a new pet") }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
