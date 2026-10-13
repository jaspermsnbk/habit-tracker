package com.jaspermsnbk.habit_tracker.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaspermsnbk.habit_tracker.data.PetMood
import com.jaspermsnbk.habit_tracker.data.PetUi
import com.jaspermsnbk.habit_tracker.ui.pet.ArtMood
import com.jaspermsnbk.habit_tracker.ui.pet.PetCanvas
import com.jaspermsnbk.habit_tracker.ui.pet.PetLook
import com.jaspermsnbk.habit_tracker.ui.pet.Species
import com.jaspermsnbk.habit_tracker.ui.pet.evolutionProgress
import com.jaspermsnbk.habit_tracker.ui.pet.fedDaysToNextStage
import com.jaspermsnbk.habit_tracker.ui.pet.stageForFedDays
import com.jaspermsnbk.habit_tracker.ui.pet.toArtMood

internal fun PetUi.message(): String = when (state.mood) {
    PetMood.Happy -> "$name is full and happy."
    PetMood.Content -> "$name is fed for today."
    PetMood.Hungry -> "$name is hungry. Do your habits to feed ${name}."
    PetMood.Sad -> "$name is wasting away. Feed ${name} soon!"
    PetMood.Dead -> "$name died. You can still bring ${name} back if you were only away a little while."
}

internal fun PetUi.look(): PetLook =
    PetLook(Species.fromName(species), stageForFedDays(state.fedDays), state.mood.toArtMood())

internal fun PetUi.stageName(): String = look().stageName

/** What is left before the pet evolves, or a note that it is fully grown. */
internal fun PetUi.evolutionText(): String {
    val look = look()
    val left = fedDaysToNextStage(state.fedDays) ?: return "Fully grown"
    val next = look.species.stageNames[look.stage + 1]
    return "$left more fed ${if (left == 1) "day" else "days"} to become $next"
}

/** The pet tab: adopt a pet, then watch it grow with your habits. */
@Composable
fun PetScreen(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
) {
    val pet by viewModel.pet.collectAsState()
    var adopting by rememberSaveable { mutableStateOf(false) }

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
            if (current == null || adopting) {
                AdoptPet(
                    onAdopt = { name, species ->
                        viewModel.adoptPet(name, species.name)
                        adopting = false
                    },
                    onCancel = if (current != null) ({ adopting = false }) else null,
                )
            } else {
                PetContent(current, onForgive = viewModel::forgivePet, onAdoptNew = { adopting = true })
            }
        }
    }
}

/** Choose one of the species and name it. A new pet starts as an egg. */
@Composable
internal fun AdoptPet(onAdopt: (name: String, species: Species) -> Unit, onCancel: (() -> Unit)?) {
    var species by rememberSaveable { mutableStateOf(Species.Mochi) }
    var name by rememberSaveable { mutableStateOf("") }
    val trimmed = name.trim()

    Text("Choose your pet", style = MaterialTheme.typography.headlineSmall)
    Text(
        "It starts as an egg and hatches when you finish a habit.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(16.dp))
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Species.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { option ->
                    SpeciesCard(option, selected = option == species, onClick = { species = option }, Modifier.weight(1f))
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Pet name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
    Button(onClick = { onAdopt(trimmed, species) }, enabled = trimmed.isNotEmpty()) { Text("Adopt") }
    if (onCancel != null) TextButton(onClick = onCancel) { Text("Cancel") }
}

@Composable
private fun SpeciesCard(species: Species, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            PetCanvas(PetLook(species, 2, ArtMood.Happy), Modifier.fillMaxWidth())
            Text(species.label, style = MaterialTheme.typography.titleSmall)
            Text(
                species.blurb,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PetContent(pet: PetUi, onForgive: () -> Unit, onAdoptNew: () -> Unit) {
    val state = pet.state
    Text(pet.name, style = MaterialTheme.typography.headlineMedium)
    if (state.alive) {
        Text(
            "Level ${state.level} · ${pet.stageName()}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    PetCanvas(
        pet.look(),
        Modifier
            .widthIn(max = 280.dp)
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        animate = true,
    )
    Text(pet.message(), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    Spacer(Modifier.height(16.dp))

    if (state.alive) {
        Text(pet.evolutionText(), style = MaterialTheme.typography.labelLarge)
        LinearProgressIndicator(
            progress = { evolutionProgress(state.fedDays) },
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text("Health ${state.health}/100", style = MaterialTheme.typography.labelLarge)
        LinearProgressIndicator(
            progress = { state.health / 100f },
            color = MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
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
            Stat("${state.fedDays}", "fed days in total")
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
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
