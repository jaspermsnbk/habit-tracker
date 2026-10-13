package com.jaspermsnbk.habit_tracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID

/** What happened when buying an item. */
sealed interface BuyResult {
    data object Bought : BuyResult
    data object AlreadyOwned : BuyResult
    data class NotEnoughGems(val missing: Int) : BuyResult
}

/** Data access for gems, owned items and what is equipped. An abstract class so [buy] can be one transaction. */
@Dao
abstract class GemDao {

    @Query("SELECT COALESCE(SUM(amount), 0) FROM gem_ledger")
    abstract fun observeBalance(): Flow<Int>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM gem_ledger")
    abstract suspend fun balance(): Int

    /** Returns the new row id, or -1 when a line with the same [GemLedgerEntity.eventKey] already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertLedger(entry: GemLedgerEntity): Long

    @Query("SELECT * FROM gem_ledger WHERE eventKey = :eventKey LIMIT 1")
    abstract suspend fun findByKey(eventKey: String): GemLedgerEntity?

    @Query("SELECT * FROM gem_ledger WHERE kind = 'STREAK_GRANT' ORDER BY createdAt ASC")
    abstract fun observeStreakGrants(): Flow<List<GemLedgerEntity>>

    @Query("SELECT * FROM owned_items")
    abstract fun observeOwned(): Flow<List<OwnedItemEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertOwned(item: OwnedItemEntity): Long

    @Query("SELECT COUNT(*) FROM owned_items WHERE itemId = :itemId")
    abstract suspend fun ownedCount(itemId: String): Int

    @Query("SELECT * FROM equipped")
    abstract fun observeEquipped(): Flow<List<EquippedEntity>>

    @Upsert
    abstract suspend fun setEquipped(equipped: EquippedEntity)

    @Query("DELETE FROM equipped WHERE slot = :slot")
    abstract suspend fun clearEquipped(slot: String)

    @Query("DELETE FROM gem_ledger")
    abstract suspend fun deleteLedger()

    @Query("DELETE FROM owned_items")
    abstract suspend fun deleteOwned()

    @Query("DELETE FROM equipped")
    abstract suspend fun deleteEquipped()

    /**
     * Buys an item: it must not be owned and the balance must cover [price]. The spend and the
     * ownership are written together or not at all.
     */
    @Transaction
    open suspend fun buy(itemId: String, price: Int, now: Instant): BuyResult {
        if (ownedCount(itemId) > 0) return BuyResult.AlreadyOwned
        val balance = balance()
        if (balance < price) return BuyResult.NotEnoughGems(missing = price - balance)
        insertLedger(
            GemLedgerEntity(
                id = UUID.randomUUID().toString(),
                amount = -price,
                kind = GemKind.SPEND.name,
                reason = "Bought $itemId",
                eventKey = "buy:$itemId",
                createdAt = now,
            )
        )
        insertOwned(OwnedItemEntity(itemId = itemId, acquiredAt = now))
        return BuyResult.Bought
    }
}
