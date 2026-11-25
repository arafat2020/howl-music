# Phase 6.3 — Library UI

**Milestone:** 6 — Visual product  
**Priority:** Foundational  
**Depends on:** 6.2, 5.2  
**Status:** Not started

## Objective

Ship usable album/artist/track browsing with sorting/search basics and artwork-led lists/grids that can start playback via the session.

## Why it matters

Library is the everyday surface. It must stay clean and practical per the brief — not a visual playground.

## Architecture

```text
LibraryViewModel → Room flows
Library screens → play(track) → MediaController/queue commands
```

## Learning objectives

- Compose lists/grids performance (`Lazy*`).
- ViewModel + Flow collection patterns.
- Search/filter without blocking UI.

## Scope

**Included**
- Artists / Albums / Tracks tabs or sections.
- Basic sort + search.
- Tap to play; add-to-queue optional if 2.5 APIs ready.
- Empty/loading/error states.

**Excluded**
- Heavy shared-element hero transitions (nice-to-have later).
- Folder browser edge cases.
- Social/sharing features.

**Must not implement yet**
- Over-animated card physics competing with scrolling.

## Implementation steps

1. ViewModels for browse queries.
2. Screens using design system + AlbumArt.
3. Wire playback actions.
4. Test on large library for jank.

## Expected project structure

```text
android-app/.../ui/library/
```

## Testing strategy

- Manual browse/search on real library.
- Play from track list updates mini-player.
- Configuration change retains query where reasonable.

## Acceptance criteria

- [ ] Browse artists/albums/tracks works.
- [ ] Search returns expected results for sample data.
- [ ] Playback starts from library.
- [ ] Scrolling remains usable under playback.

## Review checklist

- [ ] Lists use keys properly.
- [ ] No main-thread DB queries.

## Potential pitfalls

- Loading full track list without paging on huge libraries.
- Recomposition storms from unstable lambdas (follow project Compose guidance).

## Completion report

| Field | Notes |
|-------|-------|
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 6.4 — Now Playing |

**Approval required before Phase 6.4.**
