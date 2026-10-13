package com.jaspermsnbk.habit_tracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jaspermsnbk.habit_tracker.ui.pet.ArtMood
import com.jaspermsnbk.habit_tracker.ui.pet.EVOLUTION_FED_DAYS
import com.jaspermsnbk.habit_tracker.ui.pet.MAX_STAGE
import com.jaspermsnbk.habit_tracker.ui.pet.Outfit
import com.jaspermsnbk.habit_tracker.ui.pet.PetCanvas
import com.jaspermsnbk.habit_tracker.ui.pet.PetLook
import com.jaspermsnbk.habit_tracker.ui.pet.Species

/** Fed days at which each stage starts, for captions: stage 0 is the egg. */
internal fun stageStartLabel(stage: Int): String {
    if (stage == 0) return "Start"
    val days = EVOLUTION_FED_DAYS[stage - 1]
    return "$days fed ${if (days == 1) "day" else "days"}"
}

/**
 * Every pet the app can draw, for checking the art: a live preview you can set to any stage, mood
 * and outfit, then each species' full evolution line.
 */
@Composable
fun AdminScreen(modifier: Modifier = Modifier, onOpenSettings: () -> Unit = {}) {
    Scaffold(
        modifier = modifier,
        topBar = { HabitTopBar("Admin", onOpenSettings) },
    ) { innerPadding ->
        LazyColumn(
            Modifier.padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item { PreviewCard() }
            item { Text("Evolution lines", style = MaterialTheme.typography.titleLarge) }
            items(Species.entries) { species -> EvolutionLine(species) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreviewCard() {
    var species by rememberSaveable { mutableStateOf(Species.Mochi) }
    var stage by rememberSaveable { mutableIntStateOf(MAX_STAGE) }
    var mood by rememberSaveable { mutableStateOf(ArtMood.Happy) }
    var outfit by rememberSaveable { mutableStateOf(Outfit.None) }
    val look = PetLook(species, stage, mood, outfit)

    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Preview", style = MaterialTheme.typography.titleLarge)
            Text(
                "${look.stageName} the ${species.label.lowercase()}, ${mood.name.lowercase()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PetCanvas(look, Modifier.fillMaxWidth(), animate = true)
            ChipGroup("Species", Species.entries, species, { it.label }) { species = it }
            ChipGroup("Stage", (0..MAX_STAGE).toList(), stage, { species.stageNames[it] }) { stage = it }
            ChipGroup("Mood", ArtMood.entries, mood, { it.name }) { mood = it }
            ChipGroup("Outfit", Outfit.entries, outfit, { it.habit }) { outfit = it }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipGroup(title: String, options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(label(option)) },
                )
            }
        }
    }
}

@Composable
private fun EvolutionLine(species: Species) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(species.label, style = MaterialTheme.typography.titleMedium)
        Text(species.blurb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (stage in 0..MAX_STAGE) {
                Card(
                    Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(Modifier.padding(6.dp)) {
                        PetCanvas(PetLook(species, stage, if (stage == 0) ArtMood.Idle else ArtMood.Happy), Modifier.fillMaxWidth())
                        Text(species.stageNames[stage], style = MaterialTheme.typography.labelMedium)
                        Text(
                            stageStartLabel(stage),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
