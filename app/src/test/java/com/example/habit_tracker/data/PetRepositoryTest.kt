package com.jaspermsnbk.habit_tracker.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class PetRepositoryTest {

    private lateinit var db: HabitDatabase
    private lateinit var repository: HabitRepository

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        repository = HabitRepository(db.habitDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun noPetUntilAdopted() = runBlocking {
        assertNull(repository.pet.first())
        assertFalse(repository.adoptPet("  "))
        assertTrue(repository.adoptPet(" Momo "))
        val pet = repository.pet.first()!!
        assertEquals("Momo", pet.name)
        assertEquals(LocalDate.now(), pet.bornOn)
    }

    @Test
    fun rename_keepsBirthDate() = runBlocking {
        repository.adoptPet("Momo")
        assertTrue(repository.renamePet("Kiki"))
        assertEquals("Kiki", repository.pet.first()!!.name)
        assertFalse(repository.renamePet(""))
    }

    @Test
    fun forgive_revivesAPetThatDiedInTheGap() = runBlocking {
        val today = LocalDate.now()
        val born = today.minusDays(5)
        db.habitDao().upsertPet(PetEntity(name = "Momo", bornOn = born))
        db.habitDao().upsertHabit(
            HabitEntity(
                id = "h", name = "Read", color = "#000000", createdAt = Instant.now().minusSeconds(86_400L * 6),
                updatedAt = Instant.now(),
            )
        )
        db.habitDao().insertEntry(HabitEntryEntity("e", "h", born, Instant.now()))
        assertEquals(PetMood.Dead, repository.pet.first()!!.state.mood)

        assertTrue(repository.forgivePet())
        val revived = repository.pet.first()!!.state
        assertTrue(revived.alive)
        assertFalse(revived.canForgive)
        assertFalse(repository.forgivePet())
    }

    @Test
    fun adoptingAgain_startsOver() = runBlocking {
        db.habitDao().upsertPet(PetEntity(name = "Old", bornOn = LocalDate.now().minusDays(30)))
        repository.adoptPet("New")
        val pet = repository.pet.first()!!
        assertEquals("New", pet.name)
        assertEquals(LocalDate.now(), pet.bornOn)
    }

    @Test
    fun deleteAllData_removesThePet() = runBlocking {
        repository.adoptPet("Momo")
        repository.deleteAllData()
        assertNull(repository.pet.first())
    }

    @Test
    fun adopting_remembersTheSpecies_andDefaultsToMochi() = runBlocking {
        repository.adoptPet("Momo", "Pip")
        assertEquals("Pip", repository.pet.first()!!.species)

        repository.adoptPet("Kiki")
        assertEquals("Mochi", repository.pet.first()!!.species)
    }
}
