# Phase 5.3 — Playlists & Preferences

**Milestone:** 5 — Library & data  
**Priority:** Foundational  
**Depends on:** 5.1  
**Status:** Not started

## Objective

Implement user playlists (CRUD, ordered entries) and app preferences (including EQ preset storage definitions) in Room/DataStore as appropriate.

## Why it matters

Playlists and presets are durable product state. Engine remains numeric-state-only; names and ordering live in Android storage.

## Architecture

```text
PlaylistRepository → Room
PreferencesRepository → DataStore or Room
EQ preset definitions → Room (name + EqState payload)
apply preset → HowlEngine.applyEqState
```

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

- [ ] Playlists persist across process death.
- [ ] Ordered entries stable.
- [ ] EQ presets apply to engine via EqState.
- [ ] Engine still has no preset database.

## Review checklist

- [ ] Know why presets are app-owned.
- [ ] Understand reorder algorithm choice.

## Potential pitfalls

- Fragile reorder (renumbering entire list on each move — OK early if documented).
- Bloated preference bags without schema.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 5.4 — Artwork loading & cache |

**Approval required before Phase 5.4.**
