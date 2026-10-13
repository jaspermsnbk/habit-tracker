package com.jaspermsnbk.habit_tracker.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class GemStreakTest {

    private val today = LocalDate.of(2026, 10, 12)
    private lateinit var db: HabitDatabase
    private lateinit var repository: HabitRepository

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        repository = HabitRepository(db.habitDao(), MutableClock(today))
        runBlocking {
            db.habitDao().upsertPet(PetEntity(name = "Momo", bornOn = today.minusDays(9)))
            val created = today.minusDays(10).atStartOfDay().toInstant(ZoneOffset.UTC)
            db.habitDao().upsertHabit(HabitEntity("h", "Read", "#000000", createdAt = created, updatedAt = created))
        }
    }

    @After
    fun tearDown() = db.close()

    /** Checks off the last [days] days, each recorded at [recordedAt] given the date it is for. */
    private fun checkOff(days: Int, recordedAt: (LocalDate) -> Instant) = runBlocking {
        for (offset in 0 until days) {
            val date = today.minusDays(offset.toLong())
            db.habitDao().insertEntry(HabitEntryEntity("e$offset", "h", date, recordedAt(date)))
        }
    }

    @Test
    fun checkInsMadeOnTheDay_countTowardGems() = runBlocking {
        checkOff(8) { date -> date.atTime(9, 0).toInstant(ZoneOffset.UTC) }

        val pet = repository.pet.first()!!

        assertEquals(8, pet.state.fedStreak)
        assertEquals(8, pet.gemStreak)
        assertEquals(0, pet.gemRun)
    }

    @Test
    fun aBackdatedWeek_keepsThePetFed_butEarnsNoGemStreak() = runBlocking {
        // Everything recorded today for earlier dates, as if added from the calendar afterwards.
        checkOff(8) { Instant.parse("2026-10-12T10:00:00Z") }

        val pet = repository.pet.first()!!

        assertEquals(8, pet.state.fedStreak)
        // Seen as it really happened, the seven earlier days were missed, so no streak is building.
        assertEquals(0, pet.gemStreak)
    }

    @Test
    fun awardingFromAPet_paysTheMilestonesItsStreakReached() = runBlocking {
        runBlocking {
            db.habitDao().upsertPet(PetEntity(name = "Momo", bornOn = today.minusDays(14)))
            val created = today.minusDays(15).atStartOfDay().toInstant(ZoneOffset.UTC)
            db.habitDao().upsertHabit(HabitEntity("h", "Read", "#000000", createdAt = created, updatedAt = created))
        }
        checkOff(15) { date -> date.atTime(9, 0).toInstant(ZoneOffset.UTC) }
        val gems = GemRepository(db.gemDao(), MutableClock(today))

        val pet = repository.pet.first()!!
        val added = gems.awardStreakGems(pet.bornOn, pet.gemRun, pet.gemStreak)

        assertEquals(15, pet.gemStreak)
        assertEquals(15, added) // 5 at day 7 and 10 at day 14
    }
}
