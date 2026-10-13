package com.jaspermsnbk.habit_tracker.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jaspermsnbk.habit_tracker.HabitApplication
import com.jaspermsnbk.habit_tracker.data.BuyResult
import com.jaspermsnbk.habit_tracker.data.GemPack
import com.jaspermsnbk.habit_tracker.data.GemPurchaseProvider
import com.jaspermsnbk.habit_tracker.data.GemRepository
import com.jaspermsnbk.habit_tracker.data.NoGemPurchases
import com.jaspermsnbk.habit_tracker.data.PreferencesStore
import com.jaspermsnbk.habit_tracker.data.PurchaseResult
import com.jaspermsnbk.habit_tracker.ui.pet.Outfit
import com.jaspermsnbk.habit_tracker.ui.pet.ShopItem
import com.jaspermsnbk.habit_tracker.ui.pet.ShopSlot
import com.jaspermsnbk.habit_tracker.ui.pet.outfitFor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * What the Shop and the pet's outfit need: the gem balance, what is owned and worn, and the actions
 * to buy, wear and take off items. Buying a pack of gems goes through [purchases], which has no
 * store behind it yet.
 */
class ShopViewModel(
    private val gems: GemRepository,
    private val preferencesStore: PreferencesStore,
    val purchases: GemPurchaseProvider = NoGemPurchases,
) : ViewModel() {

    val balance: StateFlow<Int> = gems.balance.state(0)

    val owned: StateFlow<Set<String>> = gems.owned.state(emptySet())

    val equipped: StateFlow<Map<String, String>> = gems.equipped.state(emptyMap())

    /** What the pet is wearing in the outfit slot. */
    val outfit: StateFlow<Outfit> = gems.equipped.map(::outfitFor).state(Outfit.None)

    /** Gems paid for streaks since the user last dismissed the notice. */
    val unseenGems: StateFlow<Int> =
        combine(gems.streakGrants, preferencesStore.preferences) { grants, prefs ->
            grants.filter { it.createdAt.toEpochMilli() > prefs.gemGrantsSeenAt }.sumOf { it.amount }
        }.state(0)

    private val _message = MutableStateFlow<String?>(null)

    /** The result of the last action, to tell the user once. */
    val message: StateFlow<String?> = _message

    fun dismissMessage() {
        _message.value = null
    }

    fun markGemsSeen() {
        viewModelScope.launch {
            val newest = gems.streakGrants.first().maxOfOrNull { it.createdAt.toEpochMilli() } ?: return@launch
            preferencesStore.setGemGrantsSeenAt(newest)
        }
    }

    /** Buys [item] and, if that works, puts it on. */
    fun buy(item: ShopItem) {
        viewModelScope.launch {
            _message.value = when (val result = gems.buy(item.id, item.price)) {
                BuyResult.Bought -> {
                    gems.equip(item.id, item.slot.name)
                    "${item.name} is yours, and your pet is wearing it."
                }
                BuyResult.AlreadyOwned -> "You already have ${item.name}."
                is BuyResult.NotEnoughGems -> "You need ${result.missing} more gems for ${item.name}."
            }
        }
    }

    fun equip(item: ShopItem) {
        viewModelScope.launch { gems.equip(item.id, item.slot.name) }
    }

    fun unequip(slot: ShopSlot) {
        viewModelScope.launch { gems.unequip(slot.name) }
    }

    /** Buys a pack of gems with money, if the store is available, and credits it once. */
    fun buyPack(activity: Activity, pack: GemPack) {
        viewModelScope.launch {
            when (val result = purchases.buy(activity, pack)) {
                is PurchaseResult.Success -> {
                    gems.recordPurchase(result.orderId, result.gems)
                    _message.value = "${result.gems} gems added."
                }
                PurchaseResult.Cancelled -> Unit
                is PurchaseResult.Failed -> _message.value = result.message
            }
        }
    }

    private fun <T> kotlinx.coroutines.flow.Flow<T>.state(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HabitApplication
                ShopViewModel(app.gemRepository, app.preferencesStore, app.gemPurchases)
            }
        }
    }
}
