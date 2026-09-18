package com.jaspermsnbk.habit_tracker.ui

import com.jaspermsnbk.habit_tracker.data.HabitUi
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Time windows offered on the trends screen. Each is a whole number of buckets ending today,
 * so the latest bucket is never partial: short windows plot days, long ones plot weeks.
 */
internal enum class TrendRange(val label: String, val bucketDays: Int, val bucketCount: Int) {
    Week("7D", bucketDays = 1, bucketCount = 7),
    Month("30D", bucketDays = 1, bucketCount = 30),
    Quarter("3M", bucketDays = 7, bucketCount = 13),
    Year("1Y", bucketDays = 7, bucketCount = 52);

    val days: Int get() = bucketDays * bucketCount
}

/** Check-ins between [start] and [end] inclusive, out of the [possible] habit-days tracked then. */
internal data class TrendBucket(
    val start: LocalDate,
    val end: LocalDate,
    val completed: Int,
    val possible: Int,
) {
    val rate: Float get() = rate(completed, possible)
}

/** One habit's showing within a window. */
internal data class HabitTrend(
    val habit: HabitUi,
    val completed: Int,
    val possible: Int,
    val bestStreak: Int,
) {
    val rate: Float get() = rate(completed, possible)
}

internal data class Trends(
    val buckets: List<TrendBucket>,
    val completed: Int,
    val possible: Int,
    /** Completion rate of the equally long window just before this one; null if nothing was tracked then. */
    val previousRate: Float?,
    /** Longest run of consecutive days any one habit was completed within the window. */
    val bestStreak: Int,
    /** Sorted best first. */
    val perHabit: List<HabitTrend>,
    /** Completion rate by weekday within the window; weekdays with nothing to track are absent. */
    val weekdayRates: Map<DayOfWeek, Float>,
) {
    val rate: Float get() = rate(completed, possible)
}

/**
 * Summarizes [habits] over the [range] ending [today]. A habit only counts as "possible" from the
 * day it started being tracked, so adding a habit doesn't drag down the rate of earlier days.
 */
internal fun computeTrends(habits: List<HabitUi>, range: TrendRange, today: LocalDate): Trends {
    val start = today.minusDays(range.days - 1L)

    val buckets = List(range.bucketCount) { i ->
        val bucketStart = start.plusDays(i.toLong() * range.bucketDays)
        val bucketEnd = bucketStart.plusDays(range.bucketDays - 1L)
        TrendBucket(
            start = bucketStart,
            end = bucketEnd,
            completed = habits.sumOf { completedIn(it, bucketStart, bucketEnd) },
            possible = habits.sumOf { possibleIn(it, bucketStart, bucketEnd) },
        )
    }

    val previousStart = start.minusDays(range.days.toLong())
    val previousEnd = start.minusDays(1)
    val previousPossible = habits.sumOf { possibleIn(it, previousStart, previousEnd) }
    val previousRate = if (previousPossible == 0) {
        null
    } else {
        rate(habits.sumOf { completedIn(it, previousStart, previousEnd) }, previousPossible)
    }

    val perHabit = habits.map { habit ->
        HabitTrend(
            habit = habit,
            completed = completedIn(habit, start, today),
            possible = possibleIn(habit, start, today),
            bestStreak = longestStreak(habit.completedDates.filterTo(HashSet()) { it in start..today }),
        )
    }

    val weekdayDone = mutableMapOf<DayOfWeek, Int>()
    val weekdayPossible = mutableMapOf<DayOfWeek, Int>()
    generateSequence(start) { it.plusDays(1) }.takeWhile { it <= today }.forEach { date ->
        habits.forEach { habit ->
            if (date >= trackedSince(habit)) {
                weekdayPossible.merge(date.dayOfWeek, 1, Int::plus)
                if (date in habit.completedDates) weekdayDone.merge(date.dayOfWeek, 1, Int::plus)
            }
        }
    }

    return Trends(
        buckets = buckets,
        completed = buckets.sumOf { it.completed },
        possible = buckets.sumOf { it.possible },
        previousRate = previousRate,
        bestStreak = perHabit.maxOfOrNull { it.bestStreak } ?: 0,
        perHabit = perHabit.sortedByDescending { it.rate },
        weekdayRates = weekdayPossible.mapValues { (day, possible) -> rate(weekdayDone[day] ?: 0, possible) },
    )
}

/** The longest run of consecutive days in [dates]. */
internal fun longestStreak(dates: Set<LocalDate>): Int {
    var best = 0
    var run = 0
    var previous: LocalDate? = null
    for (date in dates.sorted()) {
        run = if (previous != null && date == previous.plusDays(1)) run + 1 else 1
        best = maxOf(best, run)
        previous = date
    }
    return best
}

/** When a habit started counting: its creation day, or an earlier check-in if one exists. */
private fun trackedSince(habit: HabitUi): LocalDate =
    habit.completedDates.minOrNull()?.let { minOf(it, habit.createdOn) } ?: habit.createdOn

private fun completedIn(habit: HabitUi, start: LocalDate, end: LocalDate): Int =
    habit.completedDates.count { it in start..end }

private fun possibleIn(habit: HabitUi, start: LocalDate, end: LocalDate): Int {
    val from = maxOf(start, trackedSince(habit))
    return if (from > end) 0 else ChronoUnit.DAYS.between(from, end).toInt() + 1
}

private fun rate(completed: Int, possible: Int): Float =
    if (possible == 0) 0f else completed.toFloat() / possible
