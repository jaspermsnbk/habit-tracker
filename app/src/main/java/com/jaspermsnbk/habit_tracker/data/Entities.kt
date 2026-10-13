package com.jaspermsnbk.habit_tracker.data

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
    val emoji: String? = null,
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

/** The user's pet. There is at most one row; its state is derived from check-ins since [bornOn]. */
@Entity(tableName = "pet")
data class PetEntity(
    @PrimaryKey val id: String = PET_ID,
    val name: String,
    val bornOn: LocalDate,
    /** The name of a `Species`; the UI falls back to the first species for names it does not know. */
    val species: String = DEFAULT_PET_SPECIES,
) {
    companion object {
        const val PET_ID = "pet"
    }
}

const val DEFAULT_PET_SPECIES = "Mochi"

/** A missed day the user forgave, and the day they did it (for the forgiveness cooldown). */
@Entity(tableName = "pet_forgiven_days")
data class PetForgivenDayEntity(
    @PrimaryKey val date: LocalDate,
    val forgivenOn: LocalDate,
)

/**
 * One line of the gem ledger. [amount] is signed, and the balance is the sum of every line, so there
 * is no stored balance to fall out of step. [eventKey], when set, is unique: a second line with the
 * same key is ignored, which makes every grant, purchase and refund safe to retry.
 */
@Entity(
    tableName = "gem_ledger",
    indices = [Index(value = ["eventKey"], unique = true)],
)
data class GemLedgerEntity(
    @PrimaryKey val id: String,
    val amount: Int,
    val kind: String,
    val reason: String,
    val eventKey: String? = null,
    val createdAt: Instant,
)

/** What a ledger line is for. */
enum class GemKind { STREAK_GRANT, SPEND, PURCHASE, REFUND, ADJUST }

/** A shop item the user owns. [source] is "shop" for bought items or "milestone" for free ones. */
@Entity(tableName = "owned_items")
data class OwnedItemEntity(
    @PrimaryKey val itemId: String,
    val acquiredAt: Instant,
    val source: String = SOURCE_SHOP,
) {
    companion object {
        const val SOURCE_SHOP = "shop"
        const val SOURCE_MILESTONE = "milestone"
    }
}

/** The item worn in a slot; at most one per slot. */
@Entity(tableName = "equipped")
data class EquippedEntity(
    @PrimaryKey val slot: String,
    val itemId: String,
)
