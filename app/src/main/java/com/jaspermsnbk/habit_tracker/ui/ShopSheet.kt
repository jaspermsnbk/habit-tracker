package com.jaspermsnbk.habit_tracker.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaspermsnbk.habit_tracker.data.GemPack
import com.jaspermsnbk.habit_tracker.data.PetUi
import com.jaspermsnbk.habit_tracker.ui.pet.ArtMood
import com.jaspermsnbk.habit_tracker.ui.pet.PetCanvas
import com.jaspermsnbk.habit_tracker.ui.pet.PetLook
import com.jaspermsnbk.habit_tracker.ui.pet.ShopCatalog
import com.jaspermsnbk.habit_tracker.ui.pet.ShopItem
import com.jaspermsnbk.habit_tracker.ui.pet.ShopSlot
import com.jaspermsnbk.habit_tracker.ui.pet.Species
import com.jaspermsnbk.habit_tracker.ui.pet.stageForFedDays

/** The pet as the shop shows it: yours, wearing [item]. A pet still in its egg is shown hatched, so the item can be seen. */
internal fun previewLook(pet: PetUi?, item: ShopItem): PetLook {
    val stage = (pet?.let { stageForFedDays(it.state.fedDays) } ?: 2).coerceAtLeast(1)
    return PetLook(Species.fromName(pet?.species), stage, ArtMood.Happy, item.outfit ?: com.jaspermsnbk.habit_tracker.ui.pet.Outfit.None)
}

/**
 * The Shop, as a pop-up over the Profile tab: spend gems on things for your pet. Tapping an item shows it
 * on your pet with a button to buy or wear it, inside the same pop-up.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopSheet(shop: ShopViewModel, pet: PetUi?, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = {
            shop.dismissMessage()
            onDismiss()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        ShopContent(shop, pet)
    }
}

/** What is inside the Shop pop-up, apart from the pop-up itself. */
@Composable
internal fun ShopContent(shop: ShopViewModel, pet: PetUi?) {
    val balance by shop.balance.collectAsState()
    val owned by shop.owned.collectAsState()
    val equipped by shop.equipped.collectAsState()
    val message by shop.message.collectAsState()
    var slot by rememberSaveable { mutableStateOf(ShopCatalog.slots.first()) }
    var openItemId by rememberSaveable { mutableStateOf<String?>(null) }
    val openItem = ShopCatalog.find(openItemId)

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Shop", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            GemBalance(balance)
        }
        message?.let { text ->
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = shop::dismissMessage) { Text("OK") }
                }
            }
        }
        if (openItem != null) {
            ItemDetail(
                item = openItem,
                pet = pet,
                balance = balance,
                owned = openItem.id in owned,
                worn = equipped[openItem.slot.name] == openItem.id,
                onBack = { openItemId = null },
                onBuy = { shop.buy(openItem) },
                onWear = { shop.equip(openItem) },
                onTakeOff = { shop.unequip(openItem.slot) },
            )
        } else {
            ItemList(
                slot = slot,
                onSlot = { slot = it },
                pet = pet,
                owned = owned,
                equipped = equipped,
                onOpen = { openItemId = it.id },
            )
        }
        if (shop.purchases.available) GetGems(shop)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemList(
    slot: ShopSlot,
    onSlot: (ShopSlot) -> Unit,
    pet: PetUi?,
    owned: Set<String>,
    equipped: Map<String, String>,
    onOpen: (ShopItem) -> Unit,
) {
    if (ShopCatalog.slots.size > 1) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ShopCatalog.slots.forEach { option ->
                FilterChip(selected = option == slot, onClick = { onSlot(option) }, label = { Text(option.label) })
            }
        }
    }
    ShopCatalog.forSlot(slot).chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { item ->
                ItemCard(
                    item = item,
                    pet = pet,
                    status = when {
                        equipped[item.slot.name] == item.id -> "Wearing"
                        item.id in owned -> "Owned"
                        else -> null
                    },
                    onClick = { onOpen(item) },
                    modifier = Modifier.weight(1f),
                )
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ItemCard(item: ShopItem, pet: PetUi?, status: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = if (status == "Wearing") BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            PetCanvas(previewLook(pet, item), Modifier.fillMaxWidth())
            Text(item.name, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
            if (status != null) {
                Text(status, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    GemIcon(size = 16.dp)
                    Text("${item.price}", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun ItemDetail(
    item: ShopItem,
    pet: PetUi?,
    balance: Int,
    owned: Boolean,
    worn: Boolean,
    onBack: () -> Unit,
    onBuy: () -> Unit,
    onWear: () -> Unit,
    onTakeOff: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Text("Back to the shop") }
        PetCanvas(previewLook(pet, item), Modifier.widthIn(max = 240.dp).fillMaxWidth())
        Text(item.name, style = MaterialTheme.typography.titleLarge)
        Text(
            item.blurb,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        when {
            worn -> OutlinedButton(onClick = onTakeOff) { Text("Take off") }
            owned -> Button(onClick = onWear) { Text("Wear") }
            else -> {
                Button(onClick = onBuy, enabled = balance >= item.price) { Text("Buy for ${item.price}") }
                if (balance < item.price) {
                    val missing = item.price - balance
                    Text(
                        "$missing more ${if (missing == 1) "gem" else "gems"} needed. Streaks earn them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private tailrec fun android.content.Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Gem packs for sale, shown only when the app has a store to sell them through. */
@Composable
private fun GetGems(shop: ShopViewModel) {
    val activity = LocalContext.current.findActivity() ?: return
    var packs by androidx.compose.runtime.remember { mutableStateOf<List<GemPack>>(emptyList()) }
    androidx.compose.runtime.LaunchedEffect(shop) { packs = shop.purchases.packs() }
    if (packs.isEmpty()) return
    Text("Get gems", style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        packs.forEach { pack ->
            OutlinedButton(onClick = { shop.buyPack(activity, pack) }) { Text("${pack.gems} gems · ${pack.priceText}") }
        }
    }
}
