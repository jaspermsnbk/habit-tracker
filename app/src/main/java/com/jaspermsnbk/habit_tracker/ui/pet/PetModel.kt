package com.jaspermsnbk.habit_tracker.ui.pet

import com.jaspermsnbk.habit_tracker.data.PetMood

/** The kinds of pet. Each has its own drawing and its own names for the four stages. */
enum class Species(val label: String, val blurb: String, val stageNames: List<String>) {
    Mochi("Mochi", "A round cat-like blob", listOf("Egg", "Mochling", "Mochi", "Grand Mochi")),
    Pip("Pip", "A small bird", listOf("Egg", "Peep", "Pip", "Plume Pip")),
    Bun("Bun", "A rabbit with tall ears", listOf("Egg", "Bunling", "Bun", "Grand Bun")),
    Shell("Shell", "A turtle with a patterned shell", listOf("Egg", "Pebble", "Shell", "Elder Shell")),
}

/** How the pet looks right now. Separate from [PetMood], which also has Dead. */
enum class ArtMood { Happy, Idle, Hungry, Sad, Sleep }

/** What the pet wears, chosen by the kind of habit the user sticks with most. */
enum class Outfit(val label: String, val habit: String, val trait: String?) {
    None("Plain", "Mixed habits", null),
    Runner("Headband and sneakers", "Running", "Speed"),
    Reader("Glasses and a book", "Reading", "Wisdom"),
    Mindful("Leaf and calm aura", "Meditating", "Calm"),
}

/** Everything the drawing needs. [stage] 0 is the egg, 1 to [MAX_STAGE] are the hatched forms. */
data class PetLook(
    val species: Species,
    val stage: Int,
    val mood: ArtMood = ArtMood.Idle,
    val outfit: Outfit = Outfit.None,
) {
    init {
        require(stage in 0..MAX_STAGE) { "stage must be 0..$MAX_STAGE, was $stage" }
    }

    val stageName: String get() = species.stageNames[stage]

    val description: String get() = "$stageName, ${mood.name.lowercase()}"
}

const val MAX_STAGE = 3

/** Total fed days at which a pet reaches stage 1, 2 and 3. */
val EVOLUTION_FED_DAYS = listOf(1, 7, 21)

/** The stage a pet has reached after [fedDays] fed days: 0 is still an egg. */
fun stageForFedDays(fedDays: Int): Int = EVOLUTION_FED_DAYS.count { fedDays >= it }

/** Fed days still needed for the next evolution, or null once fully evolved. */
fun fedDaysToNextStage(fedDays: Int): Int? =
    EVOLUTION_FED_DAYS.firstOrNull { it > fedDays }?.minus(fedDays)

fun PetMood.toArtMood(): ArtMood = when (this) {
    PetMood.Happy -> ArtMood.Happy
    PetMood.Content -> ArtMood.Idle
    PetMood.Hungry -> ArtMood.Hungry
    PetMood.Sad, PetMood.Dead -> ArtMood.Sad
}
