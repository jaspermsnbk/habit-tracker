package com.jaspermsnbk.habit_tracker.ui.pet

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * A pet, drawn with Compose Canvas. It is square and fills the width it is given.
 *
 * @param animate sway gently and, while asleep, float "z"s. Leave off in lists and tests, where
 *   an endless animation would keep every frame busy.
 */
@Composable
fun PetCanvas(look: PetLook, modifier: Modifier = Modifier, animate: Boolean = false) {
    var wave = 0f
    var zPhase = 0f
    if (animate) {
        val transition = rememberInfiniteTransition(label = "pet")
        val w by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse),
            label = "wave",
        )
        val z by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
            label = "z",
        )
        wave = w
        zPhase = z
    }
    Canvas(
        modifier
            .aspectRatio(1f)
            .semantics { contentDescription = "${look.species.label}: ${look.description}" },
    ) {
        val factor = size.minDimension / ART_SIZE
        scale(factor, factor, pivot = Offset.Zero) { drawPet(look, wave, zPhase) }
    }
}
