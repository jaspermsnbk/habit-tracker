# 1. Baseline and sync

**Goal:** start the pet work from a green, current base.

## Current state

- `feature/pet` branched from `claude/habit-emojis`. `master` has four widget commits
  (`4f8b744` to `d3663d9`) that `feature/pet` lacks.
- Four UI tests fail on every branch: `HabitListScreenEditTest` (2), `HabitListScreenLabelsTest`
  (`addHabit_withLabel_showsLabelOnCard`), `SettingsDataTest` (`confirmingDelete_wipesHabitsAndLabels`).
  They failed silently before because `SettingsDataTest` had a bad import and the suite did not compile.
- The pet flow (`HabitRepository.pet`) reads `LocalDate.now()` only when the data changes, so a pet left
  on screen across midnight keeps yesterday's state until something is checked off. The same is true of
  the habit list streaks.

## Steps

1. Merge `master` into `feature/pet` (`git merge master`). Expect conflicts only in `HabitApp.kt` or
   `HabitDatabase.kt` if the widget touched them; resolve by keeping both.
2. Find why the four tests fail (they time out waiting for a dialog or label). Likely cause: a Compose test
   API change, since `createComposeRule` is imported from `junit4.v2`. Fix the tests or the code, whichever is wrong.
3. Add a day-change tick: a `Flow<LocalDate>` that emits at midnight and when the app returns to the
   foreground, combined into `habits` and `pet` so both recompute.
   - Inject a `Clock` into `HabitRepository` (default `Clock.systemDefaultZone()`) so tests can set the date.
   - Replace `LocalDate.now()` in `HabitRepository`, `toggleToday` and `forgivePet` with `LocalDate.now(clock)`.

## Tests

- Repository test with a fixed `Clock`: advance a day, assert `habits` and `pet` re-emit with new streaks.
- The four repaired UI tests.

## Risks

- A midnight tick that never cancels leaks. Collect it in the repository's flow so it stops when nothing observes.
