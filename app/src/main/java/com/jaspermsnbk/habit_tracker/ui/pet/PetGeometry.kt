package com.jaspermsnbk.habit_tracker.ui.pet

import kotlin.math.max

/** Pets are drawn in a 200 x 200 space and scaled to fit; this is the y of the ground line. */
internal const val GROUND_Y = 168f

internal const val ART_SIZE = 200f

/**
 * Where a hatched pet's parts go, in the 200 x 200 drawing space. The body is an ellipse with
 * half-width [w0] and half-height [h0] resting on [GROUND_Y]; everything else hangs off it, so
 * outfits and expressions line up on every species and stage.
 */
internal data class PetGeometry(
    val cx: Float,
    val cy: Float,
    val w0: Float,
    val h0: Float,
    val eyeY: Float,
    val eyeDx: Float,
    val eyeR: Float,
    val footDx: Float,
    /** Where the mouth sits, or null for species whose beak or snout is drawn with the body. */
    val mouthY: Float?,
) {
    val top: Float get() = cy - h0

    /** Scale relative to a middle-sized pet, for details that grow with the body. */
    val k: Float get() = w0 / 44f
}

/** Half-widths and half-heights for stages 1, 2 and 3. */
private fun sizes(species: Species): Pair<List<Float>, List<Float>> = when (species) {
    Species.Mochi -> listOf(36f, 44f, 54f) to listOf(32f, 42f, 52f)
    Species.Pip -> listOf(34f, 44f, 52f) to listOf(34f, 44f, 52f)
    Species.Bun -> listOf(34f, 42f, 50f) to listOf(34f, 43f, 52f)
    Species.Shell -> listOf(38f, 46f, 56f) to listOf(30f, 38f, 46f)
}

internal fun petGeometry(species: Species, stage: Int): PetGeometry {
    require(stage in 1..MAX_STAGE) { "hatched stages are 1..$MAX_STAGE, was $stage" }
    val (widths, heights) = sizes(species)
    val w = widths[stage - 1]
    val h = heights[stage - 1]
    val cy = GROUND_Y - h
    val eyeR = max(4f, .11f * w)
    return when (species) {
        Species.Mochi -> PetGeometry(100f, cy, w, h, cy - .1f * h, .38f * w, eyeR, .5f * w, cy + .2f * h)
        Species.Pip -> PetGeometry(100f, cy, w, h, cy - .2f * h, .42f * w, eyeR, .35f * w, null)
        Species.Bun -> PetGeometry(100f, cy, w, h, cy - .05f * h, .36f * w, eyeR, .5f * w, cy + .25f * h)
        Species.Shell -> PetGeometry(100f, cy, w, h, cy - .1f * h, .38f * w, eyeR, .55f * w, cy + .22f * h)
    }
}
