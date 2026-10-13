package com.jaspermsnbk.habit_tracker.ui.pet

/** Where on the pet an item goes. One item can be worn per slot. */
enum class ShopSlot(val label: String) {
    Outfit("Outfits"),
    Hat("Hats"),
    Held("Held"),
    Background("Backgrounds"),
}

/**
 * Something the shop sells. [outfit] is what is drawn for items in the Outfit slot; other slots get
 * their drawing with the item art.
 */
data class ShopItem(
    val id: String,
    val name: String,
    val slot: ShopSlot,
    val price: Int,
    val blurb: String,
    val outfit: Outfit? = null,
)

/** Everything for sale, with prices in gems. Change prices here and nowhere else. */
object ShopCatalog {
    val items: List<ShopItem> = listOf(
        ShopItem("outfit-runner", "Runner gear", ShopSlot.Outfit, 30, "A headband and red sneakers for a pet that never stops.", Outfit.Runner),
        ShopItem("outfit-reader", "Reader's glasses", ShopSlot.Outfit, 30, "Round glasses and a book, for the curious.", Outfit.Reader),
        ShopItem("outfit-mindful", "Calm aura", ShopSlot.Outfit, 30, "A leaf, a glow and a few sparkles.", Outfit.Mindful),
    )

    fun find(id: String?): ShopItem? = items.firstOrNull { it.id == id }

    /** The slots that have something for sale, in display order. */
    val slots: List<ShopSlot> = ShopSlot.entries.filter { slot -> items.any { it.slot == slot } }

    fun forSlot(slot: ShopSlot): List<ShopItem> = items.filter { it.slot == slot }
}

/** The outfit drawn for what is worn in the Outfit slot; none when nothing is. */
fun outfitFor(equipped: Map<String, String>): Outfit =
    ShopCatalog.find(equipped[ShopSlot.Outfit.name])?.outfit ?: Outfit.None
