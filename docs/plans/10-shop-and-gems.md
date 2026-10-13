# 10. Shop and gems

**Goal:** a Shop tab where gems, earned by keeping long streaks, buy outfits and other cosmetics for the pet.
Built so a future "buy gems" in-app purchase drops in without reworking anything.

This replaces the automatic outfit choice in plan 3 and takes over the owned and equipped tables planned in plan 7.
No gems or items can be bought with money directly; money only ever buys gems.

## Design

### Earning gems
- Gems come from the pet's **fed-day streak** (`PetState.fedStreak`), at milestones. Starting values, all tunable in one
  file (`data/GemRewards.kt`):

  | Streak | Gems |
  |--------|------|
  | 7 days | 5 |
  | 14 days | 10 |
  | 30 days | 25 |
  | 60 days | 50 |
  | 100 days, then every 100 | 100 |

- A first outfit (about 30 gems) is then a 14 to 30 day streak away, and a hat (about 20) a couple of weeks.
- **Granted once, kept forever.** A grant is a row in the ledger with a unique key, so it never repeats and is not
  taken back if history is edited later. The key is `streak-<bornOn>-<run>-<milestone>`, where `run` counts how many
  times this pet's streak has broken (add `streakRun` to `PetState`), and `bornOn` stops a replacement pet colliding
  with an earlier one's grants.
- A freeze or forgiveness keeps a streak alive, so it keeps its run and its milestones.

### The ledger (money that can never drift)
- Table `gem_ledger (id, amount, kind, reason, key UNIQUE NULL, createdAt)`. `amount` is signed.
- **Balance is `SUM(amount)`**, exposed as a `Flow<Int>`. There is no stored balance to get out of step.
- Kinds: `STREAK_GRANT`, `SPEND`, `PURCHASE`, `REFUND`, `ADJUST`. The unique `key` makes every grant, spend and purchase
  idempotent (`buy:<itemId>`, `iap:<orderId>`), so a retried write cannot double-credit.

### Items
- A static `ShopCatalog` in code: `id`, `name`, `slot`, `priceGems`, and what to draw. Slots: `Outfit`, `Hat`, `Held`, `Background`.
- The three existing outfits become shop items (Runner, Reader, Mindful) at 30 gems each. Hats, held items and
  backgrounds follow from plan 7's art.
- Tables `owned_items (itemId PK, acquiredAt, source)` and `equipped (slot PK, itemId)`.
- **Buying** is one Room transaction: not already owned, balance at least the price, then insert the `SPEND` row and the
  owned row together. It either happens fully or not at all.
- **Equipping** needs ownership; one item per slot; equipping replaces what was there.
- **Traits still matter, differently:** plan 3 keeps traits for stats. An outfit may later require a trait score to *unlock
  for purchase* (for example 10 days of running before the Runner outfit can be bought); leave that out of v1.

### The Shop (a pop-up on the Profile tab)
- It is a **bottom sheet opened from a button in the Profile tab's top bar** (see plan 11), not a tab and not in Settings.
- The gem balance shows in the Profile header and at the top of the sheet.
- Slot chips, then a grid of items drawn on **your own pet** at its current species and stage, with price.
- Tap an item for its detail inside the sheet: larger preview, price, **Buy**, **Equip** or **Unequip**, and "N more gems" when short.

### Buying gems later (the seam, not built yet)
- `interface GemPurchaseProvider { val available: Boolean; suspend fun packs(): List<GemPack>; suspend fun buy(activity, pack): PurchaseResult }`
  with a default `NoGemPurchases` (not available, no packs). The Shop shows a "Get gems" row only when `available`.
- `GemPack(id, gems, priceText)`: priceText comes from the store, never hard coded.
- A completed purchase becomes a `PURCHASE` ledger row keyed `iap:<orderId>`; a refund is a negative `REFUND` row.
- Gems are a **consumable** in Google Play Billing: consume after crediting, so it can be bought again.
- The Play Billing library is **not** added now.

## Data changes

One migration, **DB v6**: `gem_ledger`, `owned_items`, `equipped`. Add all three to `deleteAllData` (see the policy ticket).

## Files

new `data/GemLedger.kt`, `data/GemRewards.kt`, `data/ShopCatalog.kt`, `data/GemPurchaseProvider.kt`, `ui/ShopSheet.kt`,
`ui/pet/PetModel.kt` (`PetLook` gains equipped items), `ui/pet/PetOutfits.kt`,
`data/PetEngine.kt` (`streakRun`), `HabitRepository.kt`, `HabitDatabase.kt`.

## Tests

- Ledger: balance is the sum; a repeated key is ignored; spend cannot take the balance below zero.
- Rewards: each milestone grants once, a broken streak starts a new run that can earn again, a replacement pet earns again,
  and editing history later does not remove a grant.
- Buy: not enough gems, already owned, success (balance falls and item is owned), and a failure leaves no half-written rows.
- Equip: only owned items; replaces the slot; unequip.
- Every catalogue item draws on every species and stage.
- Shop sheet: balance shown, locked item shows how many more gems, buying updates the grid.
- A fake `GemPurchaseProvider`: "Get gems" appears only when available; a purchase credits once even if delivered twice.

## Risks and decisions

- **Backdated check-ins** could farm gems. Decide whether gems only count check-ins made on the day itself, using
  `HabitEntryEntity.createdAt`. Default recommendation: yes, for gems only.
- **"Delete all data" and paid gems.** Wiping the ledger would destroy gems someone paid for, and consumed Play purchases
  cannot be restored. Before in-app purchases ship, decide: keep purchased gems on a wipe, or tie them to an account.
  Until then, free gems are wiped with everything else, and the confirm text should say so.
- **Trusting the phone.** Without a backend, a purchase cannot be verified server side. The backend phase in `SPEC.md` is where that belongs.
- **Balance.** Prices and rewards are guesses; keep them in two constant files so they can be tuned after real use.
