package com.jaspermsnbk.habit_tracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Gems, the items bought with them, and what the pet wears. Money only ever buys gems; items are
 * only ever bought with gems. The balance is the sum of the ledger, so it cannot drift.
 */
class GemRepository(
    private val dao: GemDao,
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    val balance: Flow<Int> = dao.observeBalance()

    /** Ids of every item the user owns. */
    val owned: Flow<Set<String>> = dao.observeOwned().map { items -> items.mapTo(HashSet()) { it.itemId } }

    /** The item id worn in each slot, by slot name. */
    val equipped: Flow<Map<String, String>> =
        dao.observeEquipped().map { rows -> rows.associate { it.slot to it.itemId } }

    /** Every gem grant for a streak, oldest first, so the UI can tell the user what is new. */
    val streakGrants: Flow<List<GemLedgerEntity>> = dao.observeStreakGrants()

    /**
     * Pays every milestone a streak of [streak] days has reached that has not been paid yet. A milestone
     * pays once per streak of one pet, and is never taken back if the history is edited later.
     * @return the gems newly added
     */
    suspend fun awardStreakGems(bornOn: LocalDate, run: Int, streak: Int): Int {
        var added = 0
        for (milestone in milestonesReached(streak)) {
            val gems = gemsForMilestone(milestone)
            val inserted = dao.insertLedger(
                GemLedgerEntity(
                    id = UUID.randomUUID().toString(),
                    amount = gems,
                    kind = GemKind.STREAK_GRANT.name,
                    reason = "$milestone day streak",
                    eventKey = streakGrantKey(bornOn, run, milestone),
                    createdAt = Instant.now(clock),
                )
            )
            if (inserted != -1L) added += gems
        }
        return added
    }

    suspend fun buy(itemId: String, price: Int): BuyResult = dao.buy(itemId, price, Instant.now(clock))

    /** Puts an owned item on in its [slot], replacing what was there. @return false if the item is not owned */
    suspend fun equip(itemId: String, slot: String): Boolean {
        if (dao.ownedCount(itemId) == 0) return false
        dao.setEquipped(EquippedEntity(slot = slot, itemId = itemId))
        return true
    }

    suspend fun unequip(slot: String) = dao.clearEquipped(slot)

    /** Gives an item without a price, for milestones. Does nothing if it is already owned. */
    suspend fun grantItem(itemId: String, source: String = OwnedItemEntity.SOURCE_MILESTONE) {
        dao.insertOwned(OwnedItemEntity(itemId = itemId, acquiredAt = Instant.now(clock), source = source))
    }

    /**
     * Credits a gem pack bought with money. [orderId] comes from the store, and a second delivery of the
     * same order does nothing, so a purchase processed twice credits once.
     * @return whether gems were added
     */
    suspend fun recordPurchase(orderId: String, gems: Int): Boolean {
        require(gems > 0) { "A purchase adds gems" }
        return dao.insertLedger(
            GemLedgerEntity(
                id = UUID.randomUUID().toString(),
                amount = gems,
                kind = GemKind.PURCHASE.name,
                reason = "Bought $gems gems",
                eventKey = "iap:$orderId",
                createdAt = Instant.now(clock),
            )
        ) != -1L
    }

    /** Takes back a purchase that was refunded. The balance can go below zero if the gems were already spent. */
    suspend fun recordRefund(orderId: String): Boolean {
        val purchase = dao.findByKey("iap:$orderId") ?: return false
        return dao.insertLedger(
            GemLedgerEntity(
                id = UUID.randomUUID().toString(),
                amount = -purchase.amount,
                kind = GemKind.REFUND.name,
                reason = "Refunded order $orderId",
                eventKey = "refund:$orderId",
                createdAt = Instant.now(clock),
            )
        ) != -1L
    }

    /** Wipes gems, owned items and what is worn. Part of "Delete all data". */
    suspend fun deleteAll() {
        dao.deleteLedger()
        dao.deleteOwned()
        dao.deleteEquipped()
    }
}

/** A gem pack that costs money, as the store describes it. */
data class GemPack(val id: String, val gems: Int, val priceText: String)

sealed interface PurchaseResult {
    data class Success(val orderId: String, val gems: Int) : PurchaseResult
    data object Cancelled : PurchaseResult
    data class Failed(val message: String) : PurchaseResult
}

/**
 * Where gems can be bought with money. The app has none yet: [NoGemPurchases] says so, and the Shop hides
 * its "Get gems" row. A Google Play Billing version slots in here, crediting the ledger with
 * [GemRepository.recordPurchase]. Prices come from the store, never from this app.
 */
interface GemPurchaseProvider {
    val available: Boolean
    suspend fun packs(): List<GemPack>
    suspend fun buy(activity: android.app.Activity, pack: GemPack): PurchaseResult
}

object NoGemPurchases : GemPurchaseProvider {
    override val available: Boolean = false
    override suspend fun packs(): List<GemPack> = emptyList()
    override suspend fun buy(activity: android.app.Activity, pack: GemPack): PurchaseResult =
        PurchaseResult.Failed("Buying gems is not available yet")
}
