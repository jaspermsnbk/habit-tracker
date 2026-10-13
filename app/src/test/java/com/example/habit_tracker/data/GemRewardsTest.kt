package com.jaspermsnbk.habit_tracker.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class GemRewardsTest {

    @Test
    fun noMilestoneBeforeSevenDays() {
        assertEquals(emptyList<Int>(), milestonesReached(0))
        assertEquals(emptyList<Int>(), milestonesReached(6))
    }

    @Test
    fun milestonesAccumulateAsTheStreakGrows() {
        assertEquals(listOf(7), milestonesReached(7))
        assertEquals(listOf(7, 14), milestonesReached(20))
        assertEquals(listOf(7, 14, 30, 60, 100), milestonesReached(100))
    }

    @Test
    fun pastOneHundred_everyHundredPaysAgain() {
        assertEquals(listOf(7, 14, 30, 60, 100), milestonesReached(199))
        assertEquals(listOf(7, 14, 30, 60, 100, 200), milestonesReached(200))
        assertEquals(listOf(7, 14, 30, 60, 100, 200, 300), milestonesReached(340))
    }

    @Test
    fun rewardsGrowWithTheMilestone() {
        assertEquals(5, gemsForMilestone(7))
        assertEquals(10, gemsForMilestone(14))
        assertEquals(25, gemsForMilestone(30))
        assertEquals(50, gemsForMilestone(60))
        assertEquals(100, gemsForMilestone(100))
        assertEquals(REPEAT_GEMS, gemsForMilestone(400))
    }

    @Test
    fun grantKeys_differByPetRunAndMilestone() {
        val born = LocalDate.of(2026, 10, 1)
        val keys = setOf(
            streakGrantKey(born, 0, 7),
            streakGrantKey(born, 0, 14),
            streakGrantKey(born, 1, 7),
            streakGrantKey(born.plusDays(1), 0, 7),
        )
        assertEquals(4, keys.size)
        assertEquals("streak-2026-10-01-0-7", streakGrantKey(born, 0, 7))
    }
}
