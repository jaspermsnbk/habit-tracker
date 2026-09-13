package com.example.habit_tracker_2.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * A habit the user is tracking. `id` is a UUID string so it can match the
 * server's id later (Phase 3+). `archived` is a soft-delete flag. `labelId`
 * optionally groups the habit under a [LabelEntity]; deleting the label just clears it.
 */
@Entity(
    tableName = "habits",
    foreignKeys = [
        ForeignKey(
            entity = LabelEntity::class,
            parentColumns = ["id"],
            childColumns = ["labelId"],
            onDelete = ForeignKey.SET_NULL,
        )
    ],
    indices = [Index(value = ["labelId"])],
)
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: String,
    val archived: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
    val labelId: String? = null,
)

/**
 * One row per day a habit was completed. The unique (habitId, date) index
 * enforces "a habit is done at most once per day". Deleting a habit cascades
 * to its entries.
 */
@Entity(
    tableName = "habit_entries",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["habitId", "date"], unique = true),
        Index(value = ["habitId"]),
    ],
)
data class HabitEntryEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val date: LocalDate,
    val createdAt: Instant,
)

/**
 * A user-defined habit type, like "Fitness". Names are unique ignoring case,
 * which the repository enforces.
 */
@Entity(tableName = "labels")
data class LabelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Instant,
)
