package com.jaspermsnbk.habit_tracker.ui.pet

import com.jaspermsnbk.habit_tracker.data.PetMood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PetModelTest {

    @Test
    fun stageForFedDays_followsTheEvolutionThresholds() {
        assertEquals(0, stageForFedDays(0))
        assertEquals(1, stageForFedDays(1))
        assertEquals(1, stageForFedDays(6))
        assertEquals(2, stageForFedDays(7))
        assertEquals(2, stageForFedDays(20))
        assertEquals(3, stageForFedDays(21))
        assertEquals(3, stageForFedDays(500))
    }

    @Test
    fun fedDaysToNextStage_countsDown_andEndsAtNull() {
        assertEquals(1, fedDaysToNextStage(0))
        assertEquals(6, fedDaysToNextStage(1))
        assertEquals(1, fedDaysToNextStage(20))
        assertNull(fedDaysToNextStage(21))
    }

    @Test
    fun everySpeciesNamesEveryStage() {
        Species.entries.forEach { assertEquals(MAX_STAGE + 1, it.stageNames.size) }
    }

    @Test
    fun everyPetMoodHasAnArtMood() {
        PetMood.entries.forEach { it.toArtMood() }
        assertEquals(ArtMood.Happy, PetMood.Happy.toArtMood())
        assertEquals(ArtMood.Dead, PetMood.Dead.toArtMood())
    }

    @Test(expected = IllegalArgumentException::class)
    fun lookRejectsAnUnknownStage() {
        PetLook(Species.Mochi, MAX_STAGE + 1)
    }

    @Test
    fun geometry_restsOnTheGround_andGrowsWithEachStage() {
        Species.entries.forEach { species ->
            var previous = 0f
            for (stage in 1..MAX_STAGE) {
                val g = petGeometry(species, stage)
                assertEquals("$species $stage", GROUND_Y, g.cy + g.h0, 0.001f)
                assertTrue("$species $stage grows", g.w0 > previous)
                previous = g.w0
                // Leave room above the body for ears and crests inside the 200 x 200 space.
                assertTrue("$species $stage top", g.top > 40f)
                assertTrue("$species $stage eyes sit on the body", g.eyeY in g.top..g.cy + g.h0)
            }
        }
    }

    @Test
    fun fromName_findsASpecies_andFallsBackToTheFirst() {
        assertEquals(Species.Pip, Species.fromName("Pip"))
        assertEquals(Species.Mochi, Species.fromName("Dragon"))
        assertEquals(Species.Mochi, Species.fromName(null))
    }

    @Test
    fun evolutionProgress_fillsEachStageThenStaysFull() {
        assertEquals(0f, evolutionProgress(0), 0.001f)
        assertEquals(0f, evolutionProgress(1), 0.001f)
        assertEquals(0.5f, evolutionProgress(4), 0.001f) // halfway from day 1 to day 7
        assertEquals(0f, evolutionProgress(7), 0.001f)
        assertEquals(1f, evolutionProgress(21), 0.001f)
        assertEquals(1f, evolutionProgress(400), 0.001f)
    }
}
