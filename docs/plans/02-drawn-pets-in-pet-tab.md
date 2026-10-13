# 2. Drawn pets in the Pet tab

**Goal:** the Pet tab shows the pet you adopted, drawn with `PetCanvas`, at the stage its fed days earned.

## Current state

- `PetScreen` shows an emoji face (`PetMood.face()`).
- `PetEntity` has `id`, `name`, `bornOn`. `PetState` has no count of fed days.
- `PetCanvas(look, animate)` and `stageForFedDays` exist and are tested.

## Design

- **Fed days:** add `fedDays: Int` to `PetState`, counted in `petState()` each time a day is fed.
  Freeze- and forgiven-covered days do not count. This is the number evolution reads.
- **Species:** add `species: String` to `PetEntity` (store the enum name; unknown names fall back to `Mochi`,
  as `PreferencesStore` does for themes). The pet is chosen at adoption and cannot change, so it feels like one animal.
- **Level:** `level = min(20, 1 + fedDays / 3)`. Show it with the progress to the next stage using `fedDaysToNextStage`.
- **Look:** `PetLook(species, stageForFedDays(fedDays), state.mood.toArtMood(), outfit)`. Outfit stays `None`
  until plan 3. A dead pet shows a grave drawn on the canvas, not an emoji.
- **Adoption dialog:** replace the name-only dialog with a screen that shows the four species side by side
  (their stage 2 looks), asks for a name, and then adopts. Reuse the Admin evolution rows for the cards.
- **Egg:** a newly adopted pet is an egg until the first fed day, which hatches it.

## Data changes

- `PetEntity.species` (TEXT NOT NULL DEFAULT 'Mochi'), **DB v5**, `MIGRATION_4_5` with
  `ALTER TABLE pet ADD COLUMN species TEXT NOT NULL DEFAULT 'Mochi'`.
- `HabitRepository.adoptPet(name, species)`; `PetUi` gains `species`.

## Files

`PetEngine.kt`, `Entities.kt`, `HabitDatabase.kt`, `HabitRepository.kt`, `HabitViewModel.kt`, `PetScreen.kt`,
new `AdoptPetScreen.kt`, `ui/pet/PetModel.kt` (level helpers).

## Tests

- `petState`: `fedDays` counts only fed days; freeze and forgiven days do not add.
- `MIGRATION_4_5` keeps an existing pet and defaults it to Mochi (extend `MigrationTest`).
- `PetScreenTest`: no pet shows the adopt flow; a pet shows its name, level and next evolution; a dead pet shows
  "Adopt a new pet".
- Level and progress helpers, including level 20 cap.

## Risks

- Users who already adopted a pet on a dev build get Mochi. Acceptable before release.
