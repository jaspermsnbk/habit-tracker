# 7. Items and cosmetics

**Goal:** things to collect and a reason to keep going between evolutions.

## Current state

- Outfits come from habit traits only. There is no inventory.

## Design

- **Earning:** items unlock on milestones, never by tapping: first fed day, 7 and 30 fed days, a 14-day streak,
  every freeze used, forgiveness used, each evolution. A milestone is a pure function of the history, so the set of
  *earned* items is derived; only what the user has **equipped** is stored.
- **Item kinds:** hat, held item, background, and a few accessories, each drawn as shapes by a function that takes the
  same `PetGeometry` the outfits use, so every item fits every species.
- **Slots:** one hat, one held item, one background. Outfits from traits remain a separate slot so a runner can wear a party hat.
- **Wardrobe screen:** reachable from the Pet tab. Shows earned and locked items, with the milestone each needs.
- **Notification:** when a milestone is reached, show it on the Pet tab with "New item".

## Data changes

`pet_equipped (slot TEXT PRIMARY KEY, item TEXT)`, **DB v9**. Earned items need no table.
Store `lastSeenItems` in preferences to show the "New item" badge once.

## Files

new `data/PetMilestones.kt`, `ui/pet/PetItems.kt`, `WardrobeScreen.kt`, `HabitRepository.kt`, `Entities.kt`.

## Tests

- Each milestone fires on its day and not before, and recomputes if history is edited.
- Equipping into an occupied slot replaces it; an item you have not earned cannot be equipped.
- Every item draws on every species and stage.

## Risks

- Items multiply art. Start with six, one per slot-type, and add by milestone as the app grows.
