# 8. Widget and streak freezes

**Goal:** show the pet on the home screen, and decide how the older per-habit streak freezes relate to the pet's freezes.

## Current state

- `master` has a Glance widget for today's habits (`widget/HabitWidget.kt`, receiver, config activity, label filter).
  `feature/pet` does not have it until plan 1 merges it.
- `origin/claude/streak-freezes` (not merged) adds per-habit freezes: one earned per 7-day streak, up to 3 banked,
  spent from the calendar day sheet to protect a past day. It adds `habits.freezesAvailable`, `habits.freezeMilestone` and a
  `habit_freezes` table at DB v3, which collides with this branch's numbering.
- The pet has its own freeze tokens (one per 5 fed days, max 3, spent automatically).

## Decision needed

The two freeze systems overlap. Recommendation: **keep the pet's freezes as the only ones** and drop the per-habit branch.
The pet is the main point, one system is easier to explain on the How it works screen, and it protects the pet rather than a
number. If the per-habit streak matters on its own, port only its calendar day-sheet UI ("Protected by a freeze") on top of the pet's freezes.

## Widget design

- Add a second widget, **Pet widget**, 2x2: the pet drawn at its stage and mood, its name, and a health bar.
  Glance cannot run Compose `Canvas`, so render `PetCanvas` to a `Bitmap` with `ImageBitmap`/`Picture` in a worker
  (draw with the same `drawPet` into an Android `Canvas` through `CanvasDrawScope`) and show it as an `Image`.
- Tapping the pet opens the Pet tab; a small button marks the next habit done (reuse `ToggleHabitAction`).
- Update on check-in and at midnight (reuse the existing receiver's update path and the day-change tick from plan 1).

## Files

new `widget/PetWidget.kt`, `widget/PetBitmap.kt`, receiver and `res/xml/pet_widget_info.xml`, manifest entry.

## Tests

- `PetBitmap` renders non-empty bitmaps for every species and mood (Robolectric native graphics).
- Widget state mapping (pet to display model) as a pure function.
- Manual check on a device for resizing and dark mode.

## Risks

- Bitmap size limits for widgets (about one screen of pixels in memory). Render at 2x the widget size, not more.
