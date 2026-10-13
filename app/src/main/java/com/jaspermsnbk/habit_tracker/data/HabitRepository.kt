package com.jaspermsnbk.habit_tracker.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * A habit plus its derived, display-ready state. The UI reads this and nothing else.
 *
 * @param last7 completion flags for the 7 days ending today (index 0 = 6 days ago, index 6 = today)
 * @param completedDates every day this habit was completed, for the calendar
 * @param labelName the name of the habit's label, or null if it has none
 * @param createdOn the local day the habit was added, so trends don't count earlier days as missed
 */
data class HabitUi(
    val id: String,
    val name: String,
    val color: String,
    val emoji: String?,
    val doneToday: Boolean,
    val currentStreak: Int,
    val last7: List<Boolean>,
    val completedDates: Set<LocalDate>,
    val labelId: String?,
    val labelName: String?,
    val createdOn: LocalDate,
)

/** A label as the UI sees it. */
data class LabelUi(
    val id: String,
    val name: String,
)

/** The pet with its derived state. */
data class PetUi(
    val name: String,
    val species: String,
    val bornOn: LocalDate,
    val state: PetState,
)

/**
 * Single point of access to habit data. In Phase 1 it's Room-only; in Phase 3 this is
 * where the backend sync is added, without the UI or ViewModel needing to change.
 */
class HabitRepository(
    private val dao: HabitDao,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val dayPollMillis: Long = DAY_POLL_MILLIS,
) {

    /** Today's date, now and again whenever it changes, so streaks and the pet move on at midnight. */
    private val today: Flow<LocalDate> = todayFlow(clock, dayPollMillis)

    /** Live stream of labels, sorted by name ignoring case. */
    val labels: Flow<List<LabelUi>> =
        dao.observeLabels().map { labels -> labels.map { LabelUi(id = it.id, name = it.name) } }

    /** Live stream of habits with streaks computed. Recombines whenever data changes. */
    val habits: Flow<List<HabitUi>> =
        combine(dao.observeHabits(), dao.observeAllEntries(), dao.observeLabels(), today) { habits, entries, labels, today ->
            val datesByHabit: Map<String, Set<LocalDate>> =
                entries.groupBy { it.habitId }
                    .mapValues { (_, list) -> list.map { it.date }.toSet() }
            val labelNames = labels.associate { it.id to it.name }

            habits.map { habit ->
                val dates = datesByHabit[habit.id].orEmpty()
                HabitUi(
                    id = habit.id,
                    name = habit.name,
                    color = habit.color,
                    emoji = habit.emoji,
                    doneToday = today in dates,
                    currentStreak = currentStreak(dates, today),
                    last7 = (6 downTo 0).map { offset -> today.minusDays(offset.toLong()) in dates },
                    completedDates = dates,
                    labelId = habit.labelId,
                    labelName = habit.labelId?.let(labelNames::get),
                    createdOn = habit.createdAt.atZone(clock.zone).toLocalDate(),
                )
            }
        }

    /** Live pet, or null before one is adopted. Its state is replayed from check-ins on every change. */
    val pet: Flow<PetUi?> =
        combine(habits, dao.observePet(), dao.observeForgivenDays()) { habits, pet, forgiven ->
            pet?.let {
                PetUi(
                    name = it.name,
                    species = it.species,
                    bornOn = it.bornOn,
                    state = petState(
                        bornOn = it.bornOn,
                        today = LocalDate.now(clock),
                        habits = habits,
                        forgiven = forgiven.mapTo(HashSet()) { day -> day.date },
                        lastForgivenOn = forgiven.maxOfOrNull { day -> day.forgivenOn },
                    ),
                )
            }
        }

    /** Starts a new pet as of today, replacing any existing one (which is how a dead pet is replaced). */
    suspend fun adoptPet(name: String, species: String = DEFAULT_PET_SPECIES): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        dao.deleteForgivenDays()
        dao.upsertPet(PetEntity(name = trimmed, bornOn = LocalDate.now(clock), species = species))
        return true
    }

    suspend fun renamePet(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        dao.renamePet(trimmed)
        return true
    }

    /**
     * Forgives the missed days the pet's owner is being offered forgiveness for, which revives a
     * pet that died in that gap.
     * @return whether anything was forgiven
     */
    suspend fun forgivePet(): Boolean {
        val days = pet.first()?.state?.forgivableDays.orEmpty()
        if (days.isEmpty()) return false
        val today = LocalDate.now(clock)
        dao.insertForgivenDays(days.map { PetForgivenDayEntity(date = it, forgivenOn = today) })
        return true
    }

    suspend fun addHabit(name: String, color: String, labelId: String? = null, emoji: String? = null) {
        val now = Instant.now()
        dao.upsertHabit(
            HabitEntity(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                color = color,
                createdAt = now,
                updatedAt = now,
                labelId = labelId,
                emoji = emoji,
            )
        )
    }

    /**
     * Adds a label unless the name is blank or matches an existing label ignoring case.
     * @return whether the label was added
     */
    suspend fun addLabel(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || dao.findLabelByName(trimmed) != null) return false
        dao.insertLabel(
            LabelEntity(
                id = UUID.randomUUID().toString(),
                name = trimmed,
                createdAt = Instant.now(),
            )
        )
        return true
    }

    /**
     * Renames a label unless the new name is blank or taken by another label ignoring case,
     * so changing only a label's capitalization is allowed.
     * @return whether the label was renamed
     */
    suspend fun renameLabel(labelId: String, name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        val sameName = dao.findLabelByName(trimmed)
        if (sameName != null && sameName.id != labelId) return false
        dao.renameLabel(labelId, trimmed)
        return true
    }

    /** Deletes a label. Its habits stay, just without a label. */
    suspend fun deleteLabel(labelId: String) {
        dao.deleteLabel(labelId)
    }

    /** Toggle today's completion: unmark if already done, otherwise mark done. */
    suspend fun toggleToday(habitId: String) {
        val today = LocalDate.now(clock)
        if (dao.findEntry(habitId, today) != null) {
            dao.deleteEntry(habitId, today)
        } else {
            dao.insertEntry(
                HabitEntryEntity(
                    id = UUID.randomUUID().toString(),
                    habitId = habitId,
                    date = today,
                    createdAt = Instant.now(),
                )
            )
        }
    }

    /**
     * Changes a habit's name, color and label. Its check-ins, streaks and start date stay as they are.
     * @return whether the habit was updated; a blank name is rejected
     */
    suspend fun updateHabit(
        habitId: String,
        name: String,
        color: String,
        labelId: String?,
        emoji: String? = null,
    ): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        dao.updateHabit(habitId, trimmed, color, labelId, emoji, Instant.now())
        return true
    }

    suspend fun deleteHabit(habitId: String) {
        dao.archiveHabit(habitId, Instant.now())
    }

    /** Permanently wipes every habit, completion and label. This can't be undone. */
    suspend fun deleteAllData() {
        dao.deleteAllEntries()
        dao.deleteAllHabits()
        dao.deleteAllLabels()
        dao.deleteForgivenDays()
        dao.deletePet()
    }
}

/**
 * Count consecutive completed days ending today (or yesterday). Missing today does not
 * break a streak until the day is over — so a streak through yesterday still counts.
 */
internal fun currentStreak(dates: Set<LocalDate>, today: LocalDate): Int {
    var cursor = if (today in dates) today else today.minusDays(1)
    var streak = 0
    while (cursor in dates) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}

/** How often to check whether the date has changed. A minute keeps midnight within a minute even after the phone slept. */
internal const val DAY_POLL_MILLIS = 60_000L

/**
 * Emits the date from [clock] now, then again each time it changes. It checks every [pollMillis]
 * instead of sleeping until midnight, because a sleeping phone pauses long delays and would
 * wake late.
 */
internal fun todayFlow(clock: Clock, pollMillis: Long = DAY_POLL_MILLIS): Flow<LocalDate> =
    flow {
        while (true) {
            emit(LocalDate.now(clock))
            delay(pollMillis)
        }
    }.distinctUntilChanged().flowOn(Dispatchers.Default)
