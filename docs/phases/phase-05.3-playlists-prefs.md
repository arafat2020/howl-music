# Phase 5.3 — Playlists & Preferences

**Milestone:** 5 — Library & data  
**Priority:** Foundational  
**Depends on:** 5.1  
**Status:** Implemented and verified on Android

## Objective

Implement user playlists (CRUD, ordered entries) and app preferences (including EQ preset storage definitions) in Room/DataStore as appropriate.

## Why it matters

Playlists and presets are durable product state. Engine remains numeric-state-only; names and ordering live in Android storage.

## Architecture

```text
PlaylistRepository → Room
PreferencesRepository → Room
EQ preset definitions → Room (name, preamp, ten band gains)
apply preset → HowlEngine.applyEqState
```

## Storage and ordering

Room version 2 adds a preferences table and an EQ preset table. The additive v1-to-v2 migration is generated from the exported schemas and preserves existing library and playlist rows. [Room migration guide](https://developer.android.com/training/data-storage/room/migrating-db-versions)

A playlist can contain the same track more than once. Editing targets an entry ID. A reorder rewrites the positions in one transaction and preserves each entry ID. Positions remain unique within a playlist. Removal can leave gaps, and append uses the next position.

Preferences store repeat mode, shuffle, and reduced motion in one row. Defaults are repeat off, shuffle off, and reduced motion off. An EQ preset stores its name, preamp, and ten band gains as scalar columns. The repository reconstructs an `EqState` and sends it to the engine on its control thread. The native engine uses only the numeric gain values.

## Learning objectives

- Playlist ordering models.
- DataStore vs Room for preferences.
- Keeping preset persistence off the audio engine.

## Scope

**Included**
- Playlist create/rename/delete; add/remove/reorder tracks.
- Preference keys: repeat/shuffle defaults, theme later, reduced-motion flag stub.
- EQ preset save/load/apply through repository → engine.

**Excluded**
- Cloud sync.
- Smart playlists / rules engine.
- Final Equalizer UI (6.5).

**Must not implement yet**
- Storing presets only inside native process memory.

## Implementation steps

1. Playlist DAO operations + tests.
2. Reorder mechanism (position integers or cross-table).
3. Preset entity + apply use case.
4. Simple debug UI or tests proving CRUD.

## Expected project structure

```text
android-app/.../data/playlist/
android-app/.../data/prefs/
android-app/.../data/eq/EqPresetRepository.kt
```

## Testing strategy

- Playlist reorder persistence after reopen DB.
- Preset apply changes engine diagnostics or audible EQ.
- Preference round-trip.

## Acceptance criteria

- [x] Playlists persist after app process restart.
- [x] Ordered entries and their identifiers survive reordering and reopening.
- [x] A persisted preset applies through EqState during native playback.
- [x] Preset storage remains app-owned.

## Review checklist

- [ ] Know why presets are app-owned.
- [ ] Understand reorder algorithm choice.

## Potential pitfalls

- Fragile reorder (renumbering entire list on each move — OK early if documented).
- Bloated preference bags without schema.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | Playlist create, rename, delete, append, remove and reorder, preferences storage, and EQ preset create, read, update, delete and apply. |
| Files created/modified | Room database and v2 schema, playlist DAO, preferences and preset entities and DAOs, repositories, and Android instrumentation. |
| Tests executed | Android debug build and unit test, full emulator instrumentation, and separate persistence write and read runs with an app force-stop between them. |
| Results | Created a v1 database from its exported schema and migrated it without losing the existing playlist. Verified playlist CRUD and ordering, preference and preset round-trips after restart, and successful JNI preset application during playback. |
| Known limitations | Preferences and presets are available through repositories. Settings UI, equalizer UI, and app-level playlist playback integration are deferred. The preset test verified application through JNI during playback. Audible output was not measured. |
| Decisions made | Keep preferences and presets in Room. Renumber positions transactionally while preserving existing playlist-entry IDs. |
| Next phase | 5.4 — Artwork loading & cache |

**Approval required before Phase 5.4.**
