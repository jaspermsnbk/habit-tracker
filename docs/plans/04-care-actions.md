# 4. Care actions: feed, play, cuddle, sleep

**Goal:** a small daily ritual that feels warm without being a way to farm rewards.

## Current state

- Nothing in the app. The mockup page (`habigachi-pets.html`) has the animations to port.
- `PetCanvas` already sways and floats "z"s; it has no one-shot animations.

## Design

- **Feed:** the pet is fed by finishing habits. A "Feed" button is enabled once today is fed
  (`state.fedToday`) and plays the chomp animation. It changes no stats; it is the reward for finishing a habit.
- **Play:** unlocks when two or more habits are done today (or all of them, if you have fewer than two).
- **Cuddle:** always available, once per hour. It only raises a "bond" counter shown as hearts, never health.
- **Sleep:** the pet goes to bed automatically between 22:00 and 07:00 local time (`ArtMood.Sleep`, night background).
  The button toggles it manually during the day for fun.
- **Caps:** store one row per action per day in `pet_care_log (date, action, count)`. Feed 1, Play 3, Cuddle 8 a day.
  Nothing the pet earns depends on these counts except bond hearts, so there is no way to cheat health or level.
- **Animation:** extend `PetCanvas` with an `PetAction?` parameter (Chew, Hop, Hearts, Ball) driven by an
  `Animatable` progress value; draw berries, ball and hearts as shapes in the same 200x200 space.

## Data changes

New table `pet_care_log` (**DB v7**, `MIGRATION_6_7`). Add its delete to `deleteAllData`.

## Files

`Entities.kt`, `HabitDao.kt`, `HabitRepository.kt`, `HabitViewModel.kt`, `PetScreen.kt`,
`ui/pet/PetCanvas.kt`, new `ui/pet/PetActions.kt`.

## Tests

- Care log: caps are enforced, counts reset the next day (fixed `Clock` from plan 1).
- Unlock rules for Play with 0, 1, 2 habits.
- Auto-sleep boundaries (21:59, 22:00, 06:59, 07:00).
- `PetCanvasTest`: every `PetAction` frame draws.
- `PetScreenTest`: buttons enable and disable as habits are checked.

## Risks

- A permanently animating canvas costs battery. Animate only while the Pet tab is visible and stop for one-shot actions.
