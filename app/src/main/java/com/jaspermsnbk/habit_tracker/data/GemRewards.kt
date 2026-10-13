package com.jaspermsnbk.habit_tracker.data

import java.time.LocalDate

/**
 * Gems paid out when a fed-day streak reaches a milestone: (days, gems). Past the last one, every
 * further [REPEAT_EVERY_DAYS] days pays [REPEAT_GEMS]. Tuned here and nowhere else.
 */
internal val STREAK_MILESTONES: List<Pair<Int, Int>> = listOf(7 to 5, 14 to 10, 30 to 25, 60 to 50, 100 to 100)

internal const val REPEAT_EVERY_DAYS = 100
internal const val REPEAT_GEMS = 100

/** Every milestone a streak of [streak] days has reached, in order. */
internal fun milestonesReached(streak: Int): List<Int> {
    val fixed = STREAK_MILESTONES.map { it.first }.filter { it <= streak }
    val lastFixed = STREAK_MILESTONES.last().first
    val repeating = generateSequence(lastFixed + REPEAT_EVERY_DAYS) { it + REPEAT_EVERY_DAYS }
        .takeWhile { it <= streak }
        .toList()
    return fixed + repeating
}

/** What reaching [milestone] days pays. */
internal fun gemsForMilestone(milestone: Int): Int =
    STREAK_MILESTONES.firstOrNull { it.first == milestone }?.second ?: REPEAT_GEMS

/**
 * Identifies one milestone of one streak of one pet, so it pays once: [bornOn] separates a replacement
 * pet from the one before, and [run] counts how many times this pet's streak has broken.
 */
internal fun streakGrantKey(bornOn: LocalDate, run: Int, milestone: Int): String =
    "streak-$bornOn-$run-$milestone"
