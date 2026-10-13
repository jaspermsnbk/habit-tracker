# Pet feature plans

The pet is the main point of the app. Each plan below is one shippable step, with its own
tests and commits. Work on `feature/pet`; merge into it, not `master`.

## What exists

- **Pet rules** (`data/PetEngine.kt`): health, mood, freeze tokens, death and forgiveness, replayed
  from check-ins. Stored: pet name, birth date, forgiven days (DB v4).
- **Pet tab** (`ui/PetScreen.kt`): choose a species and name, then the drawn pet with level, evolution progress, health, streak, freezes, forgiveness and a gravestone when it dies.
- **Profile tab** (`ui/ProfileScreen.kt`, `ui/TrendsWidgets.kt`): your pet at a glance, the gem balance and the trends as scrolling cards. The Shop button at its top right opens the Shop pop-up. Trends is no longer a tab.
- **Gems and the Shop** (`data/Gem*.kt`, `ui/ShopSheet.kt`): a gem ledger, streak rewards, outfits bought with gems and worn on the pet, and a purchase-provider seam with nothing behind it yet.
- **Pet art** (`ui/pet/`): four species, four stages, five moods, three outfits, drawn with Compose
  Canvas. Shown on the Pet tab and the Admin tab.
- **Admin tab** (`ui/AdminScreen.kt`): previews every look.
- **How it works** (`ui/HowItWorksScreen.kt`): first-run welcome and a help screen reachable from the top bar.

## Order and dependencies

| # | Plan | Needs | Size |
|---|------|-------|------|
| 1 | [Baseline and sync](01-baseline-and-sync.md) (done) | none | S |
| 2 | [Drawn pets in the Pet tab](02-drawn-pets-in-pet-tab.md) (done) | 1 | M |
| 3 | [Label traits and stats](03-label-traits-and-outfits.md) (outfits moved to 10) | 2 | M |
| 4 | [Care actions](04-care-actions.md) | 2 | M |
| 5 | [Pet voice: notifications and reactions](05-pet-voice.md) | 2 | S |
| 6 | [Branching evolutions](06-branching-evolutions.md) | 3 | M |
| 7 | [Items and cosmetics](07-items-and-cosmetics.md) (builds on 10) | 2, 10 | L |
| 8 | [Widget and streak freezes](08-widget-and-streak-freezes.md) | 2, 3 | M |
| 9 | [Egg hatching](09-egg-hatching.md) (note for later) | 2, 3 | M |
| 10 | [Shop and gems](10-shop-and-gems.md) (done, except milestone items and Play Billing) | 2, 11 | L |
| 11 | [Profile tab](11-profile-tab.md) (trends widgets, shop pop-up) (done) | 2 | M |

Plans 3, 4, 5 and 11 can run in parallel now that 2 is done. Plan 10's data work (ledger, rewards, buying) can start at once, but its Shop pop-up needs plan 11's Profile tab. Plan 10 does not need plan 3. Sizes: S is under a day, M a day or two, L longer.

## Rules every plan follows

- The pet's state is **derived** from check-ins, never stored as a mutable number. Store only choices
  (name, species, forgiven days, items owned) and one-off events.
- Evolution follows **total fed days**, never the current streak, so a bad week cannot shrink a pet.
- Anything the pet earns must come from finishing habits. A button that gives rewards for free gets tapped for free.
- Schema changes get a new migration and a migration test. Never edit a released migration.
- Each commit leaves `./gradlew :app:testDebugUnitTest` no worse than before.
- When a plan changes a rule the pet follows, update `PetRulesCopy` and `HowItWorksScreen` in the same commit.
  The copy is built from the same constants the pet uses, and `HowItWorksScreenTest` checks they match.
