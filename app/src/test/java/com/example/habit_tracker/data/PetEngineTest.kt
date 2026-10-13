package com.jaspermsnbk.habit_tracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PetEngineTest {

    private val born = LocalDate.of(2026, 9, 1)

    private fun habit(vararg done: LocalDate, createdOn: LocalDate = born) = HabitUi(
        id = "h", name = "Read", color = "#000000", emoji = null, doneToday = false, currentStreak = 0,
        last7 = emptyList(), completedDates = done.toSet(), labelId = null, labelName = null, createdOn = createdOn,
    )

    private fun daysFrom(start: LocalDate, count: Int) = (0 until count).map { start.plusDays(it.toLong()) }.toTypedArray()

    private fun state(
        today: LocalDate,
        habits: List<HabitUi>,
        forgiven: Set<LocalDate> = emptySet(),
        lastForgivenOn: LocalDate? = null,
    ) = petState(born, today, habits, forgiven, lastForgivenOn)

    @Test
    fun noHabits_petStaysAtFullHealth() {
        val s = state(born.plusDays(10), emptyList())
        assertEquals(MAX_HEALTH, s.health)
        assertTrue(s.alive)
    }

    @Test
    fun missingToday_isNotPenalised() {
        val s = state(born.plusDays(3), listOf(habit(*daysFrom(born, 3))))
        assertEquals(MAX_HEALTH, s.health)
        assertEquals(PetMood.Hungry, s.mood)
        assertFalse(s.fedToday)
    }

    @Test
    fun fedToday_isHappy() {
        val today = born.plusDays(2)
        val s = state(today, listOf(habit(*daysFrom(born, 3))))
        assertEquals(PetMood.Happy, s.mood)
        assertEquals(3, s.fedStreak)
    }

    @Test
    fun missedDay_costsHealthAndResetsStreak() {
        val s = state(born.plusDays(3), listOf(habit(born, born.plusDays(2), born.plusDays(3))))
        // day 1 missed (-25), then two fed days (+10 each)
        assertEquals(95, s.health)
        assertEquals(2, s.fedStreak)

        val missedLast = state(born.plusDays(3), listOf(habit(born, born.plusDays(1))))
        assertEquals(75, missedLast.health) // day 2 missed, today pending
        assertEquals(0, missedLast.fedStreak)
    }

    @Test
    fun healthNeverExceedsMax() {
        val s = state(born.plusDays(20), listOf(habit(*daysFrom(born, 21))))
        assertEquals(MAX_HEALTH, s.health)
    }

    @Test
    fun fiveFedDaysInARow_earnAFreeze_cappedAtMax() {
        assertEquals(1, state(born.plusDays(4), listOf(habit(*daysFrom(born, 5)))).freezes)
        assertEquals(MAX_FREEZES, state(born.plusDays(29), listOf(habit(*daysFrom(born, 30)))).freezes)
    }

    @Test
    fun freeze_coversAMissedDay_andKeepsTheStreak() {
        // 5 fed days earn a freeze, day 6 is missed and spends it, day 7 is fed.
        val done = daysFrom(born, 5) + born.plusDays(6)
        val s = state(born.plusDays(6), listOf(habit(*done)))
        assertEquals(0, s.freezes)
        assertEquals(MAX_HEALTH, s.health)
        assertEquals(6, s.fedStreak)
    }

    @Test
    fun fourMissedDays_killThePet() {
        val s = state(born.plusDays(5), listOf(habit()))
        assertEquals(PetMood.Dead, s.mood)
        assertEquals(born.plusDays(3), s.diedOn)
        assertEquals(0, s.health)
    }

    @Test
    fun lowHealth_isSad() {
        val s = state(born.plusDays(3), listOf(habit()))
        assertEquals(25, s.health)
        assertEquals(PetMood.Sad, s.mood)
    }

    @Test
    fun habitsAddedLater_dontCountEarlierDaysAsMissed() {
        val today = born.plusDays(10)
        val s = state(today, listOf(habit(createdOn = today)))
        assertEquals(MAX_HEALTH, s.health)
    }

    @Test
    fun halfOfHabitsDone_countsAsFed() {
        val a = habit(born)
        val b = habit()
        assertTrue(state(born, listOf(a, b)).fedToday)
        assertFalse(state(born, listOf(b, b)).fedToday)
    }

    @Test
    fun forgiveness_isOfferedForTheTrailingGap() {
        val today = born.plusDays(4)
        val s = state(today, listOf(habit(born)))
        assertEquals((1L..3L).map { born.plusDays(it) }, s.forgivableDays)
    }

    @Test
    fun forgivenDays_doNoDamage_andRevive() {
        val today = born.plusDays(5)
        val habits = listOf(habit(born, today))
        assertEquals(PetMood.Dead, state(today, habits).mood)

        val s = state(today, habits, forgiven = (1L..4L).map { born.plusDays(it) }.toSet())
        assertTrue(s.alive)
        assertEquals(MAX_HEALTH, s.health)
        assertFalse(s.canForgive)
    }

    @Test
    fun forgiveness_notOfferedAfterALongAbsence() {
        val s = state(born.plusDays(MAX_FORGIVABLE_GAP_DAYS.toLong() + 5), listOf(habit(born)))
        assertFalse(s.canForgive)
    }

    @Test
    fun forgiveness_waitsOutItsCooldown() {
        val today = born.plusDays(10)
        val habits = listOf(habit(*daysFrom(born, 8)))
        assertTrue(state(today, habits).canForgive)
        assertFalse(state(today, habits, lastForgivenOn = today.minusDays(3)).canForgive)
        assertTrue(state(today, habits, lastForgivenOn = today.minusDays(FORGIVE_COOLDOWN_DAYS)).canForgive)
    }

    @Test
    fun forgiveness_notOfferedWhenNothingWasMissed() {
        val s = state(born.plusDays(2), listOf(habit(*daysFrom(born, 3))))
        assertTrue(s.forgivableDays.isEmpty())
        assertNull(s.diedOn)
    }
}
