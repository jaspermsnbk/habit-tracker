package com.jaspermsnbk.habit_tracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaspermsnbk.habit_tracker.R
import com.jaspermsnbk.habit_tracker.ui.pet.ArtMood
import com.jaspermsnbk.habit_tracker.ui.pet.MAX_STAGE
import com.jaspermsnbk.habit_tracker.ui.pet.PetCanvas
import com.jaspermsnbk.habit_tracker.ui.pet.PetLook
import com.jaspermsnbk.habit_tracker.ui.pet.Species

/** First-launch welcome screen. Guest is the only entry path until Google sign-in lands (Phase 5). */
@Composable
fun LandingScreen(onContinueAsGuest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PetCanvas(PetLook(Species.Mochi, MAX_STAGE, ArtMood.Happy), Modifier.size(140.dp), animate = true)
        Spacer(Modifier.size(16.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.size(8.dp))
        Text(
            "Raise a pet with your habits.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(48.dp))
        Button(onClick = onContinueAsGuest, modifier = Modifier.fillMaxWidth()) {
            Text("Continue as guest")
        }
        Spacer(Modifier.size(12.dp))
        Text(
            "No account needed — your habits are saved on this device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
