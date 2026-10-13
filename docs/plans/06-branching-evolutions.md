# 6. Branching evolutions

**Goal:** at the adult stage the pet becomes a different form depending on the habits you kept, which gives the
level-up a payoff beyond "bigger".

## Current state

- One adult form per species, with outfits as the only variation (plan 3).

## Design

- **When:** stage 3 (21 fed days) picks a **branch**; a later stage 4 (60 fed days) is a "master" version of that branch.
- **Branch rule:** the winning trait at the moment of the 21st fed day (Speed, Wisdom, Calm, or Balanced when no trait leads).
  This needs a one-off record, because replaying later would change the answer: store `pet.branch` and set it the first time
  the repository sees `fedDays >= 21` with no branch. The only stored evolution state is that one column.
- **Forms:** per species and branch, an adult look defined by small differences in the existing drawing:
  Speed adds longer legs and streamlined ears, Wisdom adds a forehead jewel and bigger eyes, Calm adds a halo and
  half-closed eyes, Balanced is the current adult. Outfits remain separate and cosmetic.
- **Admin tab:** add a Branch chip so every species and branch can be previewed.
- **Names:** `Species.stageNames` becomes `formName(stage, branch)`.

## Data changes

`PetEntity.branch` (TEXT, nullable), **DB v8**, `MIGRATION_7_8`.

## Files

`Entities.kt`, `HabitRepository.kt`, `PetTraits.kt`, `ui/pet/PetModel.kt` (`Branch`), `PetSpecies.kt` (branch details),
`AdminScreen.kt`.

## Tests

- Branch assignment: first time only, ties to Balanced, not reassigned when habits change later.
- Every species, branch and stage draws (extend `PetCanvasTest`).
- Migration test.

## Risks

- Four species times four branches is a lot of art. Ship Mochi first, add branches to the other species one at a time.
