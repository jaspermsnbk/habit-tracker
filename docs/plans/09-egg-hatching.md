# 9. Egg hatching (note for later)

**Status:** idea, not started. Do after plan 2, since it replaces the species picker built there.

**Goal:** at the start, the user opens an egg and gets a random pet. Their name is the seed, so the same
name always hatches the same pet, and the result sets the pet's type and starting talents.

## Decisions to make

- **Which name:** the app has no account name (guests only). Ask for it on the egg screen ("What's your name?").
  This is not the pet's name; the user names the pet after it hatches.
- **Reroll:** none. The pet being "yours" is the point. Replacing a dead pet uses a rebirth count in the seed,
  so it hatches a different pet and not the same one again.
- **Rarity:** none at first, every species equally likely. Weighted rarity can come later by weighting the draw.
- **Does it replace choosing a species?** Yes. Plan 2 ships a picker; this removes it from adoption.

## Design

- `hatch(name, rebirth = 0): Hatchling(species, affinity)`, a pure function in `ui/pet/PetHatching.kt`.
- **Normalise the name** (Unicode NFKC, lowercase, trim, collapse spaces) so "  Jasper  M" and "jasper m" match.
- **Seed:** first 8 bytes of SHA-256 of `"habigachi:v1:<normalized>:<rebirth>"`. Use SHA-256 and a fixed generator
  (SplitMix64) instead of `String.hashCode()` or `Random`, so results never change across platforms or versions,
  which matters if there is ever an iOS version.
- **Draw:** species is `below(Species.entries.size)`. Starting talents: 2 points each in Speed, Wisdom and Calm, then
  6 extra points each given to a random trait, so every pet has 12 points and none is stronger, only different.
  The highest is the pet's `favourite` trait (ties go Speed, Wisdom, Calm).
- **Using the talents:** they are a head start added to the trait scores from plan 3, and the favourite trait can
  pick the outfit before any habits exist. Branching evolutions (plan 6) can also read them.
- **Version the seed string** (`v1`). Changing the algorithm changes every existing pet, so never edit it in place.

## UI

1. Landing, then How it works, then the egg screen asks for a name.
2. The egg is drawn with `PetCanvas` (stage 0) and wobbles. Tap it several times: each tap cracks it more
   (add crack lines to the egg drawing, four steps), the last tap hatches it with a short burst.
3. A reveal card shows the species, its starting talents as three small bars, and a name field for the pet.
4. Adopt, which stores species and talents.

## Data changes

`PetEntity` gains `affinitySpeed`, `affinityWisdom`, `affinityCalm` (INTEGER NOT NULL DEFAULT 0) and `rebirth`
(INTEGER NOT NULL DEFAULT 0), in a new migration after plan 2's. `adoptPet(name, hatchling, rebirth)`.

## Tests

- Same name gives the same pet; case, spacing and full-width letters do not matter.
- Every pet has 12 points, and every species and every favourite trait appears across many names.
- Rebirth changes the result for the same name.
- **Golden test:** fixed results for three or four names, so any change to the algorithm fails loudly.
- Egg screen: tapping four times hatches; a blank name cannot start; the reveal matches `hatch()`.
- Migration test.

## Risks

- A name seed is predictable (anyone can try names to find a species). That is fine for a personal habit app, but
  say so if it ever becomes competitive.
- Unicode normalisation differs between platforms for rare characters; the golden test only covers ASCII, so add one non-ASCII name.
