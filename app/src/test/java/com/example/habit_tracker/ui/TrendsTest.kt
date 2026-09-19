package com.jaspermsnbk.habit_tracker.ui

import com.jaspermsnbk.habit_tracker.data.HabitUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TrendsTest {

    private val today = LocalDate.of(2026, 9, 12)

    private fun habit(name: String, createdDaysAgo: Long, vararg doneDaysAgo: Long) = HabitUi(
        id = name,
        name = name,
        color = "#000000",
        emoji = null,
        doneToday = 0L in doneDaysAgo,
        currentStreak = 0,
        last7 = List(7) { false },
        completedDates = doneDaysAgo.map { today.minusDays(it) }.toSet(),
        labelId = null,
        labelName = null,
        createdOn = today.minusDays(createdDaysAgo),
    )

    @Test
    fun buckets_tileTheWindowEndingToday() {
        TrendRange.entries.forEach { range ->
            val buckets = computeTrends(emptyList(), range, today).buckets

            assertEquals(range.bucketCount, buckets.size)
            assertEquals(today.minusDays(range.days - 1L), buckets.first().start)
            assertEquals(today, buckets.last().end)
            buckets.zipWithNext().forEach { (a, b) -> assertEquals(a.end.plusDays(1), b.start) }
        }
    }

    @Test
    fun possible_startsWhenHabitIsCreated() {
        val trends = computeTrends(listOf(habit("Read", createdDaysAgo = 2)), TrendRange.Week, today)

        assertEquals(listOf(0, 0, 0, 0, 1, 1, 1), trends.buckets.map { it.possible })
        assertEquals(3, trends.possible)
        assertEquals(0f, trends.rate, 0f)
    }

    @Test
    fun checkInBeforeCreation_extendsTrackedPeriod() {
        val trends = computeTrends(listOf(habit("Read", 0, 4)), TrendRange.Week, today)

        assertEquals(1, trends.completed)
        assertEquals(5, trends.possible)
    }

    @Test
    fun weeklyBuckets_sumTheirDays() {
        val trends = computeTrends(listOf(habit("Read", 100, 0, 1, 7)), TrendRange.Quarter, today)

        assertEquals(2, trends.buckets.last().completed)
        assertEquals(7, trends.buckets.last().possible)
        assertEquals(1, trends.buckets[trends.buckets.size - 2].completed)
        assertEquals(3, trends.completed)
        assertEquals(91, trends.possible)
    }

    @Test
    fun previousRate_comesFromTheWindowBefore() {
        val trends = computeTrends(listOf(habit("Read", 20, 7, 8, 9, 10)), TrendRange.Week, today)

        assertEquals(4f / 7, trends.previousRate!!, 0.001f)
        assertEquals(0f, trends.rate, 0f)
    }

    @Test
    fun previousRate_isNullWhenNothingWasTrackedBefore() {
        val trends = computeTrends(listOf(habit("Read", 3, 0)), TrendRange.Week, today)

        assertNull(trends.previousRate)
    }

    @Test
    fun perHabit_isSortedBestFirst_withStreaksClippedToWindow() {
        val trends = computeTrends(
            listOf(habit("A", 6, 0), habit("B", 6, 0, 1, 2), habit("C", 20, 5, 6, 7, 8)),
            TrendRange.Week,
            today,
        )

        assertEquals(listOf("B", "C", "A"), trends.perHabit.map { it.habit.name })
        assertEquals(listOf(3, 2, 1), trends.perHabit.map { it.bestStreak })
        assertEquals(3, trends.bestStreak)
    }

    @Test
    fun weekdayRates_onlyCountTrackedDays() {
        val trends = computeTrends(listOf(habit("Read", 13, 0, 7)), TrendRange.Month, today)

        assertEquals(7, trends.weekdayRates.size)
        assertEquals(1f, trends.weekdayRates.getValue(today.dayOfWeek), 0f)
        assertEquals(0f, trends.weekdayRates.getValue(today.dayOfWeek.plus(1)), 0f)
    }

    @Test
    fun longestStreak_findsLongestRun() {
        assertEquals(0, longestStreak(emptySet()))
        assertEquals(1, longestStreak(setOf(today)))
        assertEquals(3, longestStreak(setOf(1L, 2, 3, 5, 6).map { today.minusDays(it) }.toSet()))
    }

    @Test
    fun bucketLabel_namesDaysAndWeeks() {
        val trends = computeTrends(emptyList(), TrendRange.Quarter, today)

        assertEquals("Today", bucketLabel(TrendBucket(today, today, 0, 0), today))
        assertEquals("Sep 11", bucketLabel(TrendBucket(today.minusDays(1), today.minusDays(1), 0, 0), today))
        assertEquals("Last 7 days", bucketLabel(trends.buckets.last(), today))
        assertEquals("Aug 30 – Sep 5", bucketLabel(trends.buckets[trends.buckets.size - 2], today))
    }

    @Test
    fun barIndexAt_clampsToChart() {
        assertEquals(0, barIndexAt(-5f, 100f, 7))
        assertEquals(3, barIndexAt(50f, 100f, 7))
        assertEquals(6, barIndexAt(100f, 100f, 7))
    }
}
