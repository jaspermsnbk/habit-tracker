package com.jaspermsnbk.habit_tracker.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CurrentStreakTest {

    private val today = LocalDate.of(2026, 9, 12)

    private fun daysAgo(vararg offsets: Long): Set<LocalDate> =
        offsets.map { today.minusDays(it) }.toSet()

    @Test
    fun noEntries_isZero() {
        assertEquals(0, currentStreak(emptySet(), today))
    }

    @Test
    fun onlyToday_isOne() {
        assertEquals(1, currentStreak(daysAgo(0), today))
    }

    @Test
    fun consecutiveDaysEndingToday_areCounted() {
        assertEquals(3, currentStreak(daysAgo(0, 1, 2), today))
    }

    @Test
    fun missingToday_doesNotBreakStreakThroughYesterday() {
        assertEquals(2, currentStreak(daysAgo(1, 2), today))
    }

    @Test
    fun gap_endsStreak() {
        assertEquals(2, currentStreak(daysAgo(0, 1, 3, 4, 5), today))
    }

    @Test
    fun lastDoneTwoDaysAgo_isZero() {
        assertEquals(0, currentStreak(daysAgo(2, 3), today))
    }
}
