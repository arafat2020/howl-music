# Phase 5.1 — Room Schema

**Milestone:** 5 — Library & data  
**Priority:** Foundational  
**Depends on:** 4.1  
**Status:** Not started

## Objective

Create Room entities/DAOs for tracks, albums, artists, and basic playlists schema — without full MediaStore scanning yet.

## Why it matters

Library UX needs a local index. Schema mistakes are expensive to migrate; start small and intentional.

## Architecture

```text
android-app (or future data layer)
  Room DB
    Artist / Album / Track / Playlist / PlaylistTrack
```

Playback still uses engine URIs; DB does not decode audio.

## Learning objectives

- Room entities, DAOs, migrations mindset.
- Normalization basics for music libraries.
- Why path/URI identity matters.

## Scope

**Included**
- Entities + DAOs + Database class.
- Simple insert/query unit tests (in-memory DB).
- Indices for common browse queries.

**Excluded**
- Scanner (5.2).
- Artwork files (5.4).
- Full-text search engine beyond basic LIKE/Query (can enhance later).

**Must not implement yet**
- Writing EQ DSP coefficients as blobs without need (presets later).

## Implementation steps

1. Draft ERD briefly in this phase’s notes.
2. Implement Room schema v1.
3. In-memory instrumented/unit tests.
4. Document migration policy (destructive OK early; versioned later).

## Expected project structure

```text
android-app/.../data/db/
  HowlDatabase.kt
  entities/*.kt
  dao/*.kt
```

## Testing strategy

- Insert track → query by album/artist.
- Playlist membership round-trip.

## Acceptance criteria

- [ ] Schema supports library browse essentials.
- [ ] Tests pass on in-memory DB.
- [ ] No scanner yet; no UI dependency required.
- [ ] URI/path field strategy documented.

## Review checklist

- [ ] Understand relationships and indices.
- [ ] Know what is deferred to migrations.

## Potential pitfalls

- Over-modeling tagging systems early.
- Using display titles as unique keys.
- Blocking main thread with Room (use correct dispatchers later).

## Completion report

| Field | Notes |
|-------|-------|
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 5.2 — Media scanning & permissions |

**Approval required before Phase 5.2.**
