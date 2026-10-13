# 3. Label traits and outfits

**Goal:** the kind of habit you keep decides what the pet wears and which stat grows.

## Current state

- Labels are free text (`LabelEntity` has `id`, `name`, `createdAt`), so "Fitness" and "Gym" are different.
- `Outfit` has `Runner`, `Reader` and `Mindful`, each with a `trait` of Speed, Wisdom or Calm.
- Habits carry an optional `labelId`.

## Design

- **Trait per label:** add `trait: String?` to `LabelEntity` (null means no trait). The label dialog gets a row
  of trait chips: None, Speed (running, gym), Wisdom (reading, study), Calm (meditation, sleep). A keyword guess
  pre-selects a chip when the name matches ("run", "gym", "read", "study", "meditat", "sleep"); the user can change it.
- **Stats:** `traitScores(habits, today, windowDays = 30)`: for each trait, the number of completed days over the
  last 30 days across habits whose label has that trait. Pure function, derived like everything else.
- **Outfit:** the trait with the highest score wins; ties and a top score under 5 days give `Outfit.None`.
  To stop the outfit flickering, require a lead of at least 3 days to change away from the current winner (store nothing;
  compute against the previous day's winner by replaying, or keep it simple and accept small flicker in v1).
- **Stats on the Pet tab:** three bars (Speed, Wisdom, Calm) scaled 0 to 100 as `min(100, completedDays * 100 / 30)`.
- **Unlabelled habits** count toward health and fed days but toward no trait.

## Data changes

`LabelEntity.trait` (TEXT, nullable), **DB v6**, `MIGRATION_5_6`. `LabelUi` gains `trait`.

## Files

`Entities.kt`, `HabitDatabase.kt`, `HabitRepository.kt`, `LabelNameDialog.kt` (trait chips), new
`data/PetTraits.kt`, `PetScreen.kt`, `ui/pet/PetModel.kt` (map trait to outfit).

## Tests

- `traitScores`: window edges, labels without a trait, deleted labels, habits completed on the same day counting once each.
- Outfit choice: clear winner, tie, below threshold.
- Keyword guess table.
- `SettingsLabelsTest`: choosing a trait saves it; renaming keeps it.
- Migration test.

## Risks

- Existing label tests assert dialog text; adding chips must not change the strings they look for.
