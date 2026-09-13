package com.example.habit_tracker_2.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

/**
 * Data access for habits and their completion entries.
 * Queries returning [Flow] emit again whenever the underlying tables change,
 * which is what drives automatic UI updates.
 */
@Dao
interface HabitDao {

    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY createdAt ASC")
    fun observeHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit_entries")
    fun observeAllEntries(): Flow<List<HabitEntryEntity>>

    @Upsert
    suspend fun upsertHabit(habit: HabitEntity)

    @Query("UPDATE habits SET archived = 1, updatedAt = :now WHERE id = :id")
    suspend fun archiveHabit(id: String, now: Instant)

    @Query("SELECT * FROM habit_entries WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun findEntry(habitId: String, date: LocalDate): HabitEntryEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntry(entry: HabitEntryEntity)

    @Query("DELETE FROM habit_entries WHERE habitId = :habitId AND date = :date")
    suspend fun deleteEntry(habitId: String, date: LocalDate)
}
