package com.jaspermsnbk.habit_tracker.ui.pet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PetCanvasTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** Every species, stage, mood and outfit must draw without throwing. */
    @Test
    fun everyCombination_draws() {
        val looks = Species.entries.flatMap { species ->
            (0..MAX_STAGE).flatMap { stage ->
                ArtMood.entries.flatMap { mood ->
                    Outfit.entries.map { outfit -> PetLook(species, stage, mood, outfit) }
                }
            }
        }
        composeRule.setContent {
            Column { looks.forEach { PetCanvas(it, Modifier.size(24.dp)) } }
        }

        composeRule.onAllNodes(hasContentDescription("", substring = true)).assertCountEquals(looks.size)
    }

    @Test
    fun describesThePetForAccessibility() {
        composeRule.setContent {
            Column {
                PetCanvas(PetLook(Species.Bun, 2, ArtMood.Hungry), Modifier.size(48.dp))
            }
        }

        composeRule.onAllNodes(hasContentDescription("Bun: Bun, hungry")).assertCountEquals(1)
    }
}
