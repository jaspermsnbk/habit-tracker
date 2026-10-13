# Habigachi full review, 12 October 2026

Branch `feature/pet`, commit `2575513`. Covers everything on the branch: the habit tracker underneath, and the pet, art,
Admin, Profile, Shop and gem work built on top of it.

## 1. How this review was done

**Done**

- Read all 47 main Kotlin files (6,891 lines) and the build, manifest and resource files. Read the 43 test files (4,411 lines, 227 tests) and sampled them closely.
- Ran Android lint (`lintDebug`) and the Kotlin compiler with `--rerun-tasks`.
- Built the debug and release APKs, and the release APK again with R8 switched on (temporary change, reverted).
- Ran the whole unit test suite three times in a row.
- Generated Compose compiler stability reports (temporary change, reverted).
- Wrote throwaway benchmark and experiment tests to measure or reproduce each claim marked **measured** or **reproduced** below, then deleted them.

**Not done**

- Nothing was run on a phone or emulator. Every timing is from a warm JVM on a desktop CPU under Robolectric. Treat the numbers as
  relative sizes, not as what a phone will show. A mid-range phone is typically several times slower, and a cold start slower again.
- No profiling with Perfetto, Android Studio's profilers or JankStats; no dependency vulnerability scan; no Play policy review for in-app purchases.
- The toolchain versions in the version catalog (AGP 9.4.0, Kotlin 2.4.20, compileSdk 37, Compose BOM 2026.09.00, Room 2.8.5) are newer than
  I can independently judge. I took them as given.

**Severity scale:** *High* means wrong behaviour on the core feature, data loss, or a large cost to every user. *Medium* means a real
risk that will bite under common conditions. *Low* means worth fixing, unlikely to hurt soon.

## 2. Summary

The app is in good shape for its age. It has a clear design (state is derived from check-ins instead of stored), pure logic that is
well tested, zero compiler warnings, no network permission, and a consistent, documented code style. The problems are concentrated in
four places:

1. **One correctness bug at the heart of the pet.** Deleting a habit rewrites the pet's past. (C1)
2. **The data pipeline.** Every change recomputes everything, on the main thread, several times over. (P1, P2)
3. **Release builds are not optimised.** The release APK is 50 MB where it should be under 4 MB. (P3)
4. **Polish that a public release needs:** accessibility, localisation, lint, backup rules, error handling. (B1 to B7)

| ID | Severity | Finding |
|----|----------|---------|
| C1 | High | Deleting a habit rewrites the pet's whole history, which can kill a healthy pet or revive a dead one |
| P1 | High | The derived-state pipeline runs on the main thread, 3 to 5 copies at once, allocating about 3.7 MB per emission at 17,500 check-ins |
| P2 | High | `computeTrends` is quadratic in history, 7 to 25 ms on a desktop JVM for the 1-year range, run on the main thread |
| P3 | High | R8 is off for release: 50.3 MB release APK, 3.84 MB with R8 on |
| C4 | Medium | The UI cannot tell "loading" from "empty": the adopt screen flashes for people who have a pet, and `adoptPet` replaces a living pet |
| C3 | Medium | The repository's clock freezes the time zone when the app starts |
| C5 | Medium | Several multi-step writes are not atomic |
| C2 | Medium | Gem grant keys are built from derived state that can change |
| C6 | Medium | Free gems can be farmed by changing the phone's date |
| P4 | Medium | The animated pet recomposes on every frame (63 times in one second, measured) |
| P5 | Medium | State is collected without regard to the lifecycle, and three app-wide collectors never stop |
| B1 | Medium | Lint fails with 6 errors, so `./gradlew check` cannot pass |
| B3 | Medium | Accessibility gaps, including the main "done" button |
| B2 | Medium | All UI text is hard coded; no localisation path |
| B5 | Medium | Room schemas are not exported; migrations are tested against hand-copied schemas |
| B7 | Medium | Auto Backup is on with no rules, and now covers gems |
| other | Low | See sections 3 to 5 |

## 3. Correctness and data integrity

### C1 (High). Deleting a habit rewrites the pet's history

**Where:** `HabitDao.observeHabits()` (`WHERE archived = 0`), `HabitRepository.habits` and `.pet`, `data/PetEngine.kt`.

**What happens.** "Delete habit" only sets `archived = 1`. The pet is replayed from its birth date using only the habits that are
not archived *now*. A day counts as fed when at least half of that day's habits were done, so removing a habit changes the verdict
for every past day.

**Reproduced.** A pet born 10 days ago, with a habit it always did ("Read") and a habit it never did ("Run"):

- Before: alive, health 100, 11 fed days, streak 11.
- After deleting "Read": **dead, health 0, fed days 0, having died after four missed days (2026-10-05)**.
- The reverse also holds: one habit that was never done kills the pet; deleting that habit makes it alive at full health.

So the pet's life depends on today's habit list, not on what happened. Anyone who tidies their list can kill their pet, and
anyone about to lose their pet can delete the habit they are failing. It also moves the gem "run" numbers (C2).

**Fix, two options.**

- Record when a habit was archived (`archivedAt`, new migration) and treat it as active only on days before that. Include archived
  habits when replaying the pet; keep hiding them from lists. This keeps the "derive, don't store" design.
- Or stop replaying the past: write one immutable row per finished day (`pet_day_log`: date, fed, damage applied) and replay only
  from the last row. Edits to old data then cannot change the verdict. This is also the cheaper option at runtime (see P1).

Either way add a regression test: archive the habit that was always done and assert the pet's state does not change.

### C2 (Medium). Gem grant keys depend on mutable derived state

`streakGrantKey(bornOn, run, milestone)` uses `run`, the number of times the streak has broken, which is recomputed from history on
every emission. The ledger is append-only, but its idempotency key is only as stable as that number. Anything that rewrites history
(C1 again, forgiveness, a changed habit list) can move a streak to a different `run` and pay the same milestone a second time, or make
a grant "disappear" from the pet's point of view.

**Fix.** Key a grant on something fixed when the grant is made, for example the date the milestone was reached
(`streak-<bornOn>-<milestoneDate>-<milestone>`), or store the streak's start date once. Fixing C1 removes most of the risk.

### C3 (Medium). The repository's time zone is frozen at start-up

`HabitRepository(dao, clock = Clock.systemDefaultZone())` captures the zone when the object is made.

**Reproduced.** With the default zone set to UTC-11, make the clock, then switch the default to UTC+14: `LocalDate.now(clock)` still says
2026-10-12 while `LocalDate.now()` says 2026-10-13. A traveller's "today", their streaks and the pet's day-by-day replay stay on the
old zone until the process is killed. `createdOn`, and the "checked in on the day itself" rule for gems, are also derived with
`clock.zone`, so changing zone can reclassify old check-ins. The reminder scheduler is correct here (it asks for the zone each time and
re-arms on `TIMEZONE_CHANGED`); the repository is not.

**Fix.** A small `Clock` that reads `ZoneId.systemDefault()` on every call, plus a refresh when `ACTION_TIMEZONE_CHANGED` arrives. Longer
term, store the date a check-in was made for as a string/epoch-day at write time and store the zone offset with the instant.

### C4 (Medium). "Loading" looks like "empty", and `adoptPet` does not protect a living pet

**Reproduced.** Before its first emission the view model holds `pet = null` and `habits = []` even though a pet and a habit exist.
`PetScreen` shows the "Choose your pet" screen whenever the pet is null, so everyone who already has a pet sees the species chooser,
and "No habits yet" appears on the Habits tab, for the first moments of every cold start. On the Profile tab it reads "No pet yet".

Worse, `HabitRepository.adoptPet` has no guard. **Reproduced:** with a living pet, `adoptPet("Impostor", "Bun")` returns `true` and the
pet is replaced. Today only the UI prevents that, and the UI flash above makes a stray tap possible.

**Fix.** Model the state: `sealed interface Loadable<out T> { Loading; Loaded(value) }`, or make the `StateFlow` nullable *and* add a
separate loaded flag. Show a blank or skeleton while loading. Make `adoptPet` refuse unless there is no pet or the pet is dead.

### C5 (Medium). Multi-step writes are not atomic

None of these run in a transaction, so a crash or kill part-way leaves half a change:

- `HabitRepository.deleteAllData()` (five deletes) followed by `GemRepository.deleteAll()` (three more) from the view model.
- `adoptPet()` (clears forgiven days, then replaces the pet).
- `toggleToday()` (read, then insert or delete), so two quick taps can both read "not done" and the second tap's intent is lost.
- `addLabel()` and `renameLabel()` (check the name, then write), with no unique index behind them.

`GemDao.buy()` is the one place it is done properly (`@Transaction`). **Fix.** Use `RoomDatabase.withTransaction` (or abstract-class DAO
`@Transaction` methods) for each of these, and give `labels.name` a unique `COLLATE NOCASE` index.

### C6 (Medium, before in-app purchases). Everything trusts the phone's clock

Gems only count check-ins whose recorded time falls on the day they are for. That stops backdating from the calendar, but both values
come from the device clock, so setting the phone's date forward a day at a time and checking in each time builds a 100-day streak in
minutes. For free gems this only cheats the player. Once gems can be bought or sold it matters.

**Fix.** Remember the highest date seen and refuse to award gems when the clock goes backwards or jumps by more than a day; longer
term, take time from a server when the backend phase arrives. Keep this in mind for the "buy gems" work.

### Lower-severity correctness notes

- **C7 (Low). "Delete habit" never deletes data.** Soft-deleted habits and all their check-ins stay in the database and in backups
  forever, while the dialog says the habit "will be removed from your list". Fine as a feature; surprising as a privacy expectation.
- **C8 (Low). A refund can leave a negative gem balance** (the ledger allows it on purpose), and the UI would show "-30".
- **C9 (Low). `ProfileScreen` and `PetScreen` build a new `MutableStateFlow` on every recomposition when `shop` is null** (a test-only
  path I added). That restarts the collector each time. Make `shop` required and give tests a fake.
- **C10 (Low). `ShopViewModel` builds user-facing sentences** ("You need 25 more gems for Runner gear"). Expose a sealed result and
  format it in the UI so it can be localised.
- **C11 (Low). `HabitViewModel(repository, preferences, gemRepository: GemRepository? = null)`.** A null gem repository means "Delete
  all data" silently skips gems. Production passes it, but the nullable parameter exists only for tests. Make it required.
- **C12 (Low). Fixed during this review:** the Settings description for "Delete all data" still said "habit, completion and label" while
  the confirmation said pet, gems and items. It now matches (commit `2575513`).

## 4. Performance

All figures: warm JVM, desktop CPU, Robolectric. Compare them to each other, not to a 16 ms frame budget directly.

### P1 (High). The derived-state pipeline: main thread, duplicated, allocation heavy

`HabitRepository.habits` is `combine(observeHabits, observeAllEntries, observeLabels, today)`. The work (grouping every entry, building
two date sets per habit, `atZone` for each entry, streaks) is the combine's transform.

- **It runs on the main thread. Reproduced:** with the collector on `Dispatchers.Main`, the transform reported running on the main looper.
  There is no `flowOn`, and a flow's operators run in the collector's context.
- **It runs again for every collector.** Cold flows are not shared. On the Habits tab there are three live copies (the view model,
  the widget updater and the gem awarder, which goes through `pet`, which contains `habits`). On Profile there are four (the view model's
  `habits` for the trends, its `pet`, and the same two app-wide ones). A Glance widget session adds another while it is alive.
- **It does the same work more than once per change.** In an instrumented load of one unchanged data set, the transform ran **3 times**.
  The likely cause is that the sources deliver their first values at different moments and Room can emit again after it registers for
  invalidation; `combine` recomputes each time and Room lists are not de-duplicated.
- **Cost, measured.** With 20 habits and about three years of history (17,520 check-ins): reading the table alone takes 7.4 ms on Room's
  thread; the full pipeline takes 10.3 ms, so roughly 3 ms of mapping per pass; and one pass allocates **about 3.7 MB** on the
  collecting thread. Times three to five copies, one tap on a habit means roughly 11 to 18 MB of short-lived garbage (3.7 MB × 3 to 5) and 10 to 15 ms of
  main-thread work on a desktop CPU, with a phone likely several times that. With 2,920 check-ins it is 4 ms in total, so the
  problem grows with the years a person keeps using the app.
- **`SELECT * FROM habit_entries` with no `WHERE`.** Every change re-reads every row ever written.
- **`distinctUntilChanged()` on `List<HabitUi>`** (the widget updater) compares whole lists of date sets with `equals`, which is O(all dates).
- **The pet is replayed twice per emission** (once for health, once for gems), and `forgivePet()` runs the entire pipeline just to read one value.
  The replay itself is cheap (0.2 to 0.6 ms for 20 habits over five years), so it is not the issue; the plumbing around it is.

**Fix, in order of value.**

1. One shared pipeline: a single `StateFlow<List<HabitUi>>` (and one for the pet) created once with `stateIn(appScope, WhileSubscribed(5_000))`,
   `flowOn(Dispatchers.Default)`, and `distinctUntilChanged()` on each Room input. The view models, widget, receivers and awarder all read it.
2. Stop shipping every date to the UI. Cards need `doneToday`, `currentStreak` and `last7`; the calendar needs one month; trends need a window.
   Compute those in SQL (`GROUP BY habitId` over a date range) or keep a per-habit cache updated incrementally on write.
3. With the day log from C1, the pet needs only the last unfinished days instead of its whole life.

### P2 (High). `computeTrends` is quadratic in history

`ui/Trends.kt` calls `completedIn` (a full scan of a habit's date set) once per bucket, and `trackedSince` (a full `minOrNull()` scan)
inside the weekday loop, once per habit per day. For the 1-year range that is days × habits × dates.

**Measured:**

| Habits × days of history | 1 year range | 30 day range |
|---|---|---|
| 5 × 365 | 1.6 ms | 0.3 ms |
| 10 × 1,095 | 6.9 ms | 1.4 ms |
| 20 × 1,095 | 13.7 ms | 2.8 ms |
| 20 × 1,825 | 24.9 ms | 5.1 ms |

It runs inside composition (`remember(labelHabits, selected, range, today)`), on the main thread, and again on every habits emission.
Because the `remember` key is a list of data classes, even the key comparison is a deep `equals` over every date.

**Fix.** Compute `trackedSince` once per habit; store each habit's dates as a sorted `IntArray` of epoch days and answer range counts with
binary search or prefix sums; run in a `produceState`/view model on `Dispatchers.Default`. That makes it O(days + dates).
`MonthSummary.countIn` also allocates a `YearMonth` per check-in on every recomposition.

### P3 (High). R8 is off for release builds

`app/build.gradle.kts` sets `release { optimization { enable = false } }`.

**Measured:**

| Build | Size |
|---|---|
| Debug | 68.0 MB |
| Release, R8 off (current) | 50.3 MB (dex alone: 31.9 + 9.5 + 7.7 = 49.1 MB) |
| Release, R8 on (temporary change, reverted) | **3.84 MB**, one 3.0 MB `classes.dex` |

The cause is `material-icons-extended`, which contains thousands of icon classes and is only trimmed by R8. The cost is paid by every user:
download and install size, larger dex to load and verify, and slower cold starts. R8 built cleanly with no extra rules, but a clean build is not a working app: it still needs a real run on a device (Room, Glance and
anything reflective are the usual suspects) before shipping. R8 also obfuscates, which will matter once gems have a price.

**Fix.** Turn optimisation and resource shrinking on for release, add a signing config and a `proguard-rules.pro`, and test the release build on a
device. Also consider a baseline profile (see P9). The single use of `material-icons-extended` could also be replaced with a few vector assets.

### P4 (Medium). The animated pet recomposes on every frame

`PetCanvas` reads its two infinite animations in the composable body (`val w by ...; wave = w`), so each frame recomposes the function
instead of only redrawing the canvas.

**Measured:** 63 recompositions in one second of animation. Each draw also allocates roughly 3 to 7 KB (`Path`s, `listOf(Offset)`, a
palette object, geometry) and costs about 0.12 to 0.17 ms in software rendering. The `z` animation runs even when the pet is not asleep.
Both animations ignore the system "remove animations" setting.

**Fix.** Read the animated values inside the draw lambda (`drawBehind` or `Canvas { wave.value }`) so only the draw phase runs. Run the
`z` transition only while `mood == Sleep`. Cache `PetGeometry`, palette and reusable `Path`s with `remember(look)`. Skip the animations
when `ValueAnimator.areAnimatorsEnabled()` is false. The screens that animate a pet are Pet, How it works, Landing and the Admin preview.

### P5 (Medium). Lifecycle-unaware collection and permanent background collectors

- **28 `collectAsState()` and no `collectAsStateWithLifecycle()`.** A stopped activity keeps its subscriptions, so the pipelines in P1 keep
  running for UI nobody can see. `WhileSubscribed(5_000)` never gets to stop them. Add `lifecycle-runtime-compose`.
- **`HabitApplication` runs three never-ending collectors:** the gem awarder, the reminder re-armer, and a widget updater that collects all of
  `habits`. Each one holds Room observers (roughly 12 Room observers on the Habits tab and 21 on Profile) and, through `todayFlow`, a coroutine that wakes
  every 60 seconds for as long as the process lives.
- **No error handling in those collectors.** They run on `Dispatchers.Main.immediate` with a `SupervisorJob` and no
  `CoroutineExceptionHandler`, so one exception (a database error while awarding gems, say) crashes the whole app in the background.
- **The gem awarder does unnecessary writes.** On every pet emission it attempts one insert per milestone reached (up to 8 for a 340-day
  streak), each of which takes the write lock and is then ignored. Gems can only change when a check-in is made, so the awarding can run
  after `toggleToday`, or at least behind a `distinctUntilChanged()` on `(bornOn, run, streak)`.

### P6 to P9 (Low to Medium)

- **P6. Unstable UI models.** The Compose stability report shows `HabitUi`, `PetUi` and `PetState` as *unstable* (they hold `Set` and `List`).
  Strong skipping is on, so all 77 composables are skippable, but unstable parameters are compared by identity. Every emission builds
  new `HabitUi` objects, so every habit card recomposes after any change. Mark them `@Immutable` or use `kotlinx.collections.immutable`.
- **P7. Eager composition.** Profile, Calendar and Settings are a `Column(verticalScroll)`, so everything composes up front. Fine today;
  "By habit" will not be with many habits. The Profile plan called for lazy items.
- **P8. Draw-time allocations in the chart.** `PathEffect.dashPathEffect(floatArrayOf(...))` is allocated on every draw of the trend chart.
- **P9. Start-up.** `Application.onCreate` builds the database and reads `SharedPreferences` on the main thread, and starts the pipelines in
  P1. There is no app baseline profile (the APK only contains the libraries' 11 KB profile), no Macrobenchmark and no StrictMode in debug builds.

### What to measure on a device

Cold start with `reportFullyDrawn` and a Macrobenchmark; a Perfetto trace of one check-in with 5 years of data (look for main-thread slices
from `HabitRepository`); JankStats or `dumpsys gfxinfo` while scrolling Profile with the 1Y range; the Layout Inspector recomposition counter
on the Pet tab; heap allocations during one tap (Android Studio allocation tracker); and release vs debug APK install size.

## 5. Best-practice gaps

### B1. Lint fails, so `check` cannot pass

`lintDebug` reports **6 errors and 1 warning** and there is no baseline or `lint {}` configuration:

- `NonObservableLocale` at `CalendarScreen.kt:218`, `SettingsScreen.kt:364`, `TrendsWidgets.kt:257` and `TrendsWidgets.kt:388`. Use `LocalLocale.current`.
- `ViewModelConstructorInComposable` at `PreferencesAppliedTest.kt:53` and `:69` (tests building a view model inside `setContent`).
- `UseKtx` at `SettingsScreen.kt:611` (`Uri.parse`).

The Kotlin compiler reports no warnings at all, which is a good sign. There is no CI workflow in the repository to run lint or the tests.

### B2. Hard-coded text, manual plurals, non-localised dates

About 77 `Text("...")` literals plus dialogs, content descriptions, notification text and view-model messages; the only string resource used
from code is the app name. Plurals are hand written (`if (n == 1) "day" else "days"`) in a dozen places. Dates use pattern strings
(`"MMMM yyyy"`), which are wrong for many languages; use `DateTimeFormatter.ofLocalizedDate` or a best-pattern skeleton.
**Fix:** move strings to `strings.xml`, use `pluralStringResource`, format dates by locale.

### B3. Accessibility

- **The main "done" control** (`HabitListScreen.DoneToggle`) is a 40 dp clickable `Box`. It has no role, no checked state, and no label
  while unchecked (the "Done today" description is on the check icon, which only exists once done). Use `toggleable(role = Role.Checkbox)`
  with the habit's name, and a 48 dp target.
- **Other targets under 48 dp:** colour swatches (36), speed-dial mini buttons (40), the widget toggle (36).
- **Information carried by colour alone:** the week dots and calendar dots have no text equivalent for last-7-days state.
- **The trend chart** is one description for the whole canvas; individual bars can only be inspected by touch.
- **Pet descriptions repeat the species** ("Mochi: Mochi, happy") and omit outfits.
- **Motion:** infinite animations ignore the system's remove-animations setting (P4).

### B4. Architecture and dependency management

- **Service locator through `Application`.** Receivers, the widget and view-model factories reach into `HabitApplication`. Fine at this size;
  the SPEC's Hilt plan is the right next step.
- **Hard-coded dispatchers** (`Dispatchers.Default`, `.Main.immediate`, `.IO`) cannot be replaced in tests, which is why tests use real time and sleeps.
- **Layering.** `HabitUi` and `PetUi` live in `data/`; domain concepts (`Species`, `ShopCatalog`, `Outfit`) live under `ui/pet/`;
  `HabitRepository` now handles habits, labels and the pet (about 290 lines). Split `PetRepository`, and add a small `domain` package.
- **Large files:** `SettingsScreen` (629 lines), `TrendsWidgets` (507), `HabitListScreen` (420), `CalendarScreen` (412).
- **Hand-rolled navigation.** Three booleans and a tab enum, with early `return`s inside composables. System back from any tab exits the app
  instead of going to the first tab. Workable now; Navigation Compose will be wanted once screens multiply.

### B5. Room and persistence

- `exportSchema = false`, so there are no schema JSON files, no auto-migrations and no `MigrationTestHelper`. The migration tests build old
  databases from SQL copied by hand, which can drift from what shipped. The database is at version 6 with five hand-written migrations.
- A redundant index: `habit_entries` has a unique `(habitId, date)` index and a separate `(habitId)` index that the first already covers.
- No unique index on label names, no purge or retention for soft-deleted habits (C7).
- Preferences use `SharedPreferences` read on the main thread; DataStore is the recommended replacement.
- Good: no destructive-migration fallback, `LocalDate`/`Instant` converters, foreign keys with sensible cascade rules, and a transactional `buy`.

### B6. Error handling and observability

No logging of any kind (no `Log` calls) and no crash-reporting abstraction. `startActivity` for the privacy policy is not guarded and throws
`ActivityNotFoundException` on a phone without a browser. Coroutines launched from view models have no handlers. A debug-only StrictMode setup
would have caught the main-thread disk reads.

### B7. Backup and privacy

`allowBackup="true"` with the template's empty `backup_rules.xml` and `data_extraction_rules.xml`. Auto Backup therefore copies the whole
database and preferences, now including the gem ledger and owned items, and backing up a live SQLite file in WAL mode has a known risk of restoring inconsistently unless the log is checkpointed first.
Decide what to back up, and what a restore means for gems before they can be bought. On the positive side the app declares no `INTERNET`
permission, so nothing can leave the phone.

### B8. Tests

**Strengths:** 227 tests that run in about 12 seconds of test time, passed three runs in a row, cover pure logic, the repository with a real in-memory
Room database, migrations, and UI through Robolectric. The pet engine is easy to test because it is a pure function.

**Weaknesses:**

- `ExampleInstrumentedTest` asserts the package name `com.jaspermsnbk.habit_tracker_2`, which is not the app's id, and `ExampleUnitTest` asserts `2 + 2`.
  Both are template leftovers; the instrumented one would fail if it were ever run, which tells you instrumented tests are not run.
- The unit tests sit in `com/example/habit_tracker` directories, although every file declares `package com.jaspermsnbk.habit_tracker...`. A leftover from the rename.
- Four `Thread.sleep` calls and about 41 `waitUntil` polls on real time; `waitUntilSettled` exists only to work around main-thread scheduling.
  Injecting dispatchers and a test clock would remove it.
- Some tests use `LocalDate.now()` for data and could fail if they run across midnight.
- The Canvas art is only checked for "does not throw" (plus my one-off screenshots). There are no golden or screenshot tests.
- The process-wide database singleton leaks between `MainActivityTest` cases (the file notes it); pets and gems now leak too.
- There are no property tests for `petState` (health stays in 0..100, `fedDays` never exceeds days since birth, a freeze never lowers health).

### B9. Build, release and project hygiene

- Release has no signing config and no `proguard-rules.pro`; `rules.keep` is the template stub.
- No CI, README, CONTRIBUTING or CHANGELOG. (`docs/plans` is a good start.)
- `.kotlin/` was not git-ignored (added in this review).
- Leftover names from the rename: `Theme.Habittracker2`, the reminder action `com.jaspermsnbk.habit_tracker_2.action.REMIND`.
- `java` source and target 11 under a JDK 25 toolchain; consider `jvmToolchain`.
- `gradle.properties` could enable parallel builds and the build cache.

### B10. The home-screen widget

- It does not follow the brand colours or the Dynamic colour setting: `GlanceTheme` uses its own dynamic palette, and the fallback accent in
  both the widget and `parseColor` is the old template purple `0xFF6650A4`.
- Its toggle is 36 dp, below the 48 dp minimum. Its list item ids are `String.hashCode()` values, which can collide.
- It keeps its own collector of the whole habits pipeline for as long as its Glance session lives (P1).

### B11. The Admin tab ships in release builds

It is a developer gallery and sits in the bottom bar for every user. Gate it behind `BuildConfig.DEBUG`, or move it into Settings as a
developer option.

## 6. What is good

- **The core idea.** Streaks and the pet are derived from check-ins, so the displayed state can never disagree with the data. The pure
  functions (`petState`, `computeTrends`, `milestonesReached`) are small, documented and tested.
- **The gem ledger.** Append-only, signed amounts, balance as a sum, unique event keys, a transactional buy, and a clean seam for in-app purchases.
- **Migrations.** Every schema change has a migration and a test; nothing falls back to destructive recreation.
- **Code quality.** Zero compiler warnings, no `!!`, no TODOs, consistent KDoc on public functions, `internal` used well, no network permission.
- **Theme and design tokens.** One colour scheme file with light and dark, derived from the pets; window background matches to avoid a flash.
- **Receivers.** `goAsync()` used correctly, alarms re-armed after reboot, time change and zone change, and no exact-alarm permission needed.
- **Process.** Small commits with explanatory messages, written plans for each feature, and a ticket list in Notion.

## 7. Suggested order of work

1. **This week (small, high value):** switch on R8 and test a release build (P3); fix the six lint errors and add a CI job running tests and lint (B1);
   gate the Admin tab (B11); fix C1 by recording `archivedAt` (or add the day log); guard `adoptPet` and add a loading state (C4).
2. **Next:** one shared, off-main data pipeline with `distinctUntilChanged` and lifecycle-aware collection (P1, P5); rewrite `computeTrends`
   (P2); make the `Clock` follow the system zone (C3); wrap the multi-step writes in transactions (C5); move gem awarding to after the check-in.
3. **Before a public release:** accessibility pass (B3), strings and plurals (B2), explicit backup rules (B7), exported Room schemas (B5),
   error handling and logging (B6), a baseline profile and a Macrobenchmark (P9), and real instrumented tests on a device.
4. **Before in-app purchases:** the clock-trust fix (C6), the "delete all data and paid gems" decision, key gems on stable events (C2),
   server-side purchase verification, and a Play Billing implementation behind `GemPurchaseProvider`.

## 8. Tickets
Each finding was turned into one or more tickets in the Notion database Habigachi tickets (Plan = Review, 50 tickets), with a lettered id for each piece (C1a, C1b, and so on), a priority, a size, a checklist of what to do, acceptance criteria, and how to verify. This file is the repo copy of the source page that those tickets link to.

## 9. Appendix: how to reproduce

- Lint: `./gradlew --offline :app:lintDebug`. Report: `app/build/reports/lint-results-debug.html`.
- Sizes: `./gradlew :app:assembleDebug :app:assembleRelease`; for the R8 number set `release { optimization { enable = true } }` temporarily.
- Stability report: add `composeCompiler { reportsDestination = layout.buildDirectory.dir("compose_reports") }` temporarily to `app/build.gradle.kts`.
- The measurements and the reproductions of C1, C3 and C4 were short Robolectric tests (a seeded in-memory database, `MutableClock`, `System.nanoTime`,
  `ThreadMXBean.getThreadAllocatedBytes`). They were deleted afterwards. Turn the C1 and C4 ones into permanent regression tests when those are fixed.
