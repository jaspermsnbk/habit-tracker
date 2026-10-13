# 5. Pet voice: notifications and check-in reactions

**Goal:** the pet speaks for the app, so reminders and check-ins feel like the pet's own.

## Current state

- `reminderContent()` in `ReminderNotifier.kt` builds "N habits left today" and mentions an at-risk streak.
- Checking a habit off (`HabitListScreen` circle) gives no feedback beyond the streak count.

## Design

- **Voice lines:** a pure `petLine(mood, name, event)` returning short strings, with a few variants per mood
  chosen by a seeded index so tests are stable. Events: HabitDone, AllDone, Hungry, Sad, AboutToDie, Forgiven, Hatched, Evolved.
- **Reminder:** when a pet exists, the notification title becomes the pet's line ("Momo is hungry") and the text keeps the
  habit list. Extra at-risk line when the pet would take damage tonight: it has no freeze, health is low, and nothing is done yet.
- **Death warning:** an extra notification at the reminder time on the day the pet is one miss from dying.
- **Check-in reaction:** after checking a habit, a small speech bubble above the pet on the Pet tab, and a snackbar-free
  inline "Momo loved that" on the Habits tab header. The Habits tab gets a slim pet strip at the top (small `PetCanvas` and
  the latest line) that opens the Pet tab.
- **Evolution moment:** a one-time dialog when `stageForFedDays` rises, showing the old and new forms. Store the last-seen
  stage in `PreferencesStore` (`pet_seen_stage`) so it shows once.

## Files

new `data/PetLines.kt`, `ReminderNotifier.kt`, `HabitListScreen.kt` (pet strip), `PetScreen.kt` (bubble), `PreferencesStore.kt`.

## Tests

- `petLine` returns a non-empty line for every mood and event, and the same line for the same seed.
- `reminderContent` with and without a pet; the death warning only when it applies.
- Evolution dialog appears once and not again after dismissing.

## Risks

- Notification copy is easy to make annoying. Keep it to one notification a day, as now, and respect the existing Settings toggle.
