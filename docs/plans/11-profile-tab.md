# 11. Profile tab

**Goal:** a Profile tab that replaces the Trends tab. It holds the trends as scrollable widgets, and a button in its top
right opens the Shop as a pop-up. This keeps the bottom bar at five tabs without hiding anything.

Replaces the "move Admin into Settings" idea in plan 10.

## Bottom bar

Habits, Pet, Calendar, **Profile**, Admin. Trends is no longer a tab. Every tab still has the help and settings buttons.

## Profile tab

- **Header:** the pet's small portrait with its name and level, and the gem balance. Tapping the portrait opens the Pet tab.
- **Top bar, right side:** a **Shop** button (a bag or gem icon) opens the Shop pop-up, then the existing help and settings
  buttons. The Shop button shows no number; the balance is in the header and inside the pop-up.
- **Filters, kept from Trends:** the range selector (1W, 1M, 3M, 1Y) and the label filter sit under the header and apply to
  every widget below. They stay at the top of the scrolling list.
- **Widgets:** a vertical, scrolling list of cards, one per part of today's Trends screen:
  1. **Summary tiles** (best streak, completion, check-ins).
  2. **Check-ins** per day or per week, with the tap-to-inspect bars.
  3. **By weekday.**
  4. **By habit**, which still selects a single habit and filters the other widgets.
  Each is its own composable that takes the computed `Trends` and the range, so a new widget is one function and one list entry.
  New widgets can be added later (gems earned over time, stat bars from plan 3, pet history) without touching the others.
- Alternative if a long list feels heavy: a horizontal pager of widgets. Start vertical; it is the easier layout to scan.
- Filter and scroll state survive tab changes and Settings, as the Trends tab does today (`rememberSaveable` and the saveable state holder in `HabitApp`).

## Shop pop-up

- A **modal bottom sheet** opened from the Profile top bar, nearly full height. Back or a swipe down closes it.
- Contents are the Shop from plan 10: balance, slot chips, a grid of items drawn on the user's own pet, and the
  buy, equip and unequip sheet. The item detail opens inside the pop-up, not as another pop-up on top.
- Because it is a pop-up and not a tab, nothing else in the app needs to know about it. It reads the ledger and the catalogue.
- The sheet takes a `GemPurchaseProvider` for the future "Get gems" row (plan 10).

## Files

`ui/HabitApp.kt` (tabs), new `ui/ProfileScreen.kt`, `ui/TrendsScreen.kt` split into `ui/trends/*Widget.kt` plus the existing
`Trends.kt` maths, new `ui/ShopSheet.kt`, `ui/HabitTopBar.kt` (an optional extra action), `MainActivityTest`, trends tests.

## Tests

- The bottom bar has Habits, Pet, Calendar, Profile and Admin; there is no Trends tab.
- Each widget renders from fixed data; changing the range or label updates every widget; selecting a habit in "By habit" filters the others.
- The existing Trends tests (`TrendsScreenTest`, `TrendsLabelFilterTest`, `TrendsTest`) are moved to the widgets, not deleted.
- The Shop button opens the pop-up; back closes it and returns to the same scroll position.
- State survives tab switches and Settings (as `tabState_survivesSettingsAndTabSwitches` does today).

## Risks

- `TrendsScreen` is one 500-line file with shared state (range, label, habit, inspected bar). Pull that into a small state holder
  before splitting, or the widgets will end up passing a dozen parameters each.
- Users who go looking for "Trends" need to find it: label the tab Profile and title the first widget section "Trends".
