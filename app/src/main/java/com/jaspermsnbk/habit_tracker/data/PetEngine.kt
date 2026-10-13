package com.jaspermsnbk.habit_tracker.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal const val MAX_HEALTH = 100
internal const val FED_DAY_HEALTH = 10
internal const val MISSED_DAY_DAMAGE = 25
internal const val FED_DAYS_PER_FREEZE = 5
internal const val MAX_FREEZES = 3

/** Every this many fed days is one level, up to [MAX_LEVEL]. */
internal const val FED_DAYS_PER_LEVEL = 3
internal const val MAX_LEVEL = 20

/** The level a pet has reached after [fedDays] fed days; a new pet is level 1. */
internal fun petLevel(fedDays: Int): Int = minOf(MAX_LEVEL, 1 + fedDays / FED_DAYS_PER_LEVEL)

/** A day is "fed" when at least this share of the habits that existed that day were done. */
internal const val FED_RATIO = 0.5

/** Forgiveness covers a gap of at most this many days, and can be used once per cooldown. */
internal const val MAX_FORGIVABLE_GAP_DAYS = 7
internal const val FORGIVE_COOLDOWN_DAYS = 14L

enum class PetMood { Happy, Content, Hungry, Sad, Dead }

/**
 * The pet as the UI sees it, derived entirely from check-in history.
 *
 * @param freezes unused freeze tokens; one is spent automatically on a missed day
 * @param fedStreak consecutive fed days, not counting days a freeze or forgiveness covered
 * @param streakRun how many times this pet's fed streak has broken, so each streak has its own number
 * @param fedDays every fed day in the pet's life, which is what evolution counts; a freeze or forgiveness never adds to it
 * @param forgivableDays the missed days forgiveness would cover; empty when it isn't on offer
 */
data class PetState(
    val health: Int,
    val mood: PetMood,
    val freezes: Int,
    val fedStreak: Int,
    val fedDays: Int,
    val streakRun: Int,
    val fedToday: Boolean,
    val diedOn: LocalDate?,
    val forgivableDays: List<LocalDate>,
) {
    val alive: Boolean get() = diedOn == null
    val level: Int get() = petLevel(fedDays)
    val canForgive: Boolean get() = forgivableDays.isNotEmpty()
}

/**
 * Replays every day since the pet was born to work out its state. Nothing but the birth date and
 * the forgiven days is stored, so the pet can't drift from the check-ins, and editing history
 * simply changes the result.
 *
 * Each day, in order:
 * - **fed** (see [FED_RATIO]): health goes up, and every [FED_DAYS_PER_FREEZE] fed days in a row
 *   earns a freeze token (at most [MAX_FREEZES]);
 * - **missed**: a forgiven day does nothing, otherwise a freeze is spent, and with none left the
 *   pet takes [MISSED_DAY_DAMAGE] and loses its streak. At zero health it dies.
 *
 * Today never counts as missed, since it isn't over. Days with no habits are skipped.
 *
 * @param forgiven days the user forgave
 * @param lastForgivenOn when forgiveness was last used, for the cooldown
 */
internal fun petState(
    bornOn: LocalDate,
    today: LocalDate,
    habits: List<HabitUi>,
    forgiven: Set<LocalDate>,
    lastForgivenOn: LocalDate?,
): PetState {
    var health = MAX_HEALTH
    var freezes = 0
    var streak = 0
    var fedDays = 0
    var streakRun = 0
    var fedToday = false
    var diedOn: LocalDate? = null
    var gapStart: LocalDate = bornOn
    var gapHasDamage = false

    var day = bornOn
    while (!day.isAfter(today)) {
        val active = habits.filter { !it.createdOn.isAfter(day) }
        if (active.isNotEmpty()) {
            val fed = active.count { day in it.completedDates }.toDouble() / active.size >= FED_RATIO
            when {
                fed -> {
                    health = minOf(MAX_HEALTH, health + FED_DAY_HEALTH)
                    streak++
                    fedDays++
                    if (streak % FED_DAYS_PER_FREEZE == 0) freezes = minOf(MAX_FREEZES, freezes + 1)
                    gapStart = day.plusDays(1)
                    gapHasDamage = false
                    if (day == today) fedToday = true
                }
                day == today -> Unit
                day in forgiven -> Unit
                freezes > 0 -> freezes--
                else -> {
                    if (streak > 0) streakRun++
                    streak = 0
                    gapHasDamage = true
                    health -= MISSED_DAY_DAMAGE
                    if (health <= 0) {
                        health = 0
                        diedOn = day
                        break
                    }
                }
            }
        }
        day = day.plusDays(1)
    }

    // "A little while away" is measured to yesterday. A dead pet is only forgiven back through the
    // day it died, since the days after that never counted.
    val gapEnd = diedOn ?: today.minusDays(1)
    val daysAway = ChronoUnit.DAYS.between(gapStart, today)
    val gapLength = ChronoUnit.DAYS.between(gapStart, gapEnd) + 1
    val offCooldown = lastForgivenOn == null || ChronoUnit.DAYS.between(lastForgivenOn, today) >= FORGIVE_COOLDOWN_DAYS
    val forgivableDays =
        if (gapHasDamage && gapLength >= 1 && daysAway <= MAX_FORGIVABLE_GAP_DAYS && offCooldown) {
            (0 until gapLength).map { gapStart.plusDays(it) }
        } else {
            emptyList()
        }

    val mood = when {
        diedOn != null -> PetMood.Dead
        health < 40 -> PetMood.Sad
        fedToday -> if (health >= 70) PetMood.Happy else PetMood.Content
        else -> PetMood.Hungry
    }
    return PetState(health, mood, freezes, streak, fedDays, streakRun, fedToday, diedOn, forgivableDays)
}
