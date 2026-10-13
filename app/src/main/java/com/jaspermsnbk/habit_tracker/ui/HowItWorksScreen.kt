package com.jaspermsnbk.habit_tracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaspermsnbk.habit_tracker.data.FED_DAYS_PER_FREEZE
import com.jaspermsnbk.habit_tracker.data.FED_DAY_HEALTH
import com.jaspermsnbk.habit_tracker.data.FORGIVE_COOLDOWN_DAYS
import com.jaspermsnbk.habit_tracker.data.MAX_FORGIVABLE_GAP_DAYS
import com.jaspermsnbk.habit_tracker.data.MAX_FREEZES
import com.jaspermsnbk.habit_tracker.data.MISSED_DAY_DAMAGE
import com.jaspermsnbk.habit_tracker.ui.pet.ArtMood
import com.jaspermsnbk.habit_tracker.ui.pet.MAX_STAGE
import com.jaspermsnbk.habit_tracker.ui.pet.PetCanvas
import com.jaspermsnbk.habit_tracker.ui.pet.PetLook
import com.jaspermsnbk.habit_tracker.ui.pet.Species

private fun days(count: Int) = "$count ${if (count == 1) "day" else "days"}"

/** The rules shown on the screen, written from the same numbers the pet uses so they cannot drift. */
internal class PetRulesCopy(
    val fed: String = "A day is fed when you finish at least half of that day's habits. Each fed day gives your pet " +
        "$FED_DAY_HEALTH health, up to 100.",
    val missed: String = "A missed day costs $MISSED_DAY_DAMAGE health, and your pet dies if its health reaches 0. " +
        "Today never counts as missed until it is over, so you can still catch up tonight.",
    val freezes: String = "Every $FED_DAYS_PER_FREEZE fed days in a row earns a freeze token, and you can hold up to " +
        "$MAX_FREEZES. A token is used automatically on a missed day, so your pet loses nothing.",
    val forgiveness: String = "Away for up to ${days(MAX_FORGIVABLE_GAP_DAYS)}? When you come back, a Welcome back card offers to " +
        "forgive the days you missed, which also brings back a pet that died while you were gone. " +
        "You can do this once every ${days(FORGIVE_COOLDOWN_DAYS.toInt())}.",
    val levelling: String = "Your pet grows as it collects fed days. Fed days are counted over its whole life, not just " +
        "the current streak, so a bad week can make your pet sad but never makes it smaller.",
)

/**
 * Explains the pet. Shown full screen the first time the app opens ([firstRun]) and, after that,
 * from the help button on every tab and from Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowItWorksScreen(firstRun: Boolean, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val copy = PetRulesCopy()
    Scaffold(
        modifier = modifier,
        topBar = {
            if (!firstRun) {
                TopAppBar(
                    title = { Text("How it works") },
                    navigationIcon = {
                        IconButton(onClick = onDone) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (firstRun) {
                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .safeDrawingPadding()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                ) { Text("Got it, let's go") }
            }
        },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .then(if (firstRun) Modifier.safeDrawingPadding() else Modifier)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PetCanvas(
                PetLook(Species.Mochi, MAX_STAGE, ArtMood.Happy),
                Modifier.widthIn(max = 220.dp).fillMaxWidth(),
                animate = true,
            )
            Text(
                if (firstRun) "Meet your pet" else "Your pet, explained",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                "Your habits feed a pet. Keep them up and it grows. Skip them and it gets sad.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Section("Feeding it", copy.fed)
            Section("Missing a day", copy.missed)
            Section("Freeze tokens", copy.freezes)
            Section("Coming back", copy.forgiveness)
            LevelSection(copy.levelling)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Section(title: String, body: String) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** The four forms of a pet with the total fed days that unlock each. */
@Composable
private fun LevelSection(body: String) {
    val species = Species.Mochi
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Levelling up", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Text(body, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (stage in 0..MAX_STAGE) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        PetCanvas(PetLook(species, stage, if (stage == 0) ArtMood.Idle else ArtMood.Happy), Modifier.fillMaxWidth())
                        Text(species.stageNames[stage], style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                        Text(
                            stageStartLabel(stage),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
