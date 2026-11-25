# Phase 5.4 — Artwork Loading & Cache

**Milestone:** 5 — Library & data  
**Priority:** Foundational  
**Depends on:** 5.2  
**Status:** Not started

## Objective

Load and cache album/track artwork efficiently with Coil for Compose, including placeholders and reasonable memory/disk behavior.

## Why it matters

Artwork-led browsing is part of the brief’s visual identity. Poor image loading destroys scroll performance.

## Architecture

```text
Track/Album artwork URI → Coil → Compose Image
Optional: extracted embedded art cached to disk files referenced by DB
```

Keep decoding/extraction off the UI thread; never on the audio thread.

## Learning objectives

- Coil basics in Compose.
- Image request sizing for lists vs Now Playing.
- Cache keys and invalidation.

## Scope

**Included**
- Coil dependency.
- Artwork resolver (MediaStore album art and/or embedded).
- List-friendly image composable wrapper.
- Placeholder/error states.

**Excluded**
- Fancy shared-element polish (6.3/6.4).
- Online cover fetching services.

**Must not implement yet**
- Giant uncompressed bitmaps on main thread.

## Implementation steps

1. Add Coil.
2. Implement artwork URL/model resolution.
3. Create `AlbumArt` composable with size parameters.
4. Verify smooth scrolling on a real library.

## Expected project structure

```text
android-app/.../ui/components/AlbumArt.kt
android-app/.../data/artwork/ArtworkRepository.kt
```

## Testing strategy

- Manual scroll performance.
- Missing art → placeholder.
- Rotation/process recreate still loads.

## Acceptance criteria

- [ ] Artwork shows for typical MediaStore albums.
- [ ] Coil caching works across navigation.
- [ ] List scrolling remains acceptable on mid-range device.
- [ ] No audio regressions from image work.

## Review checklist

- [ ] Know list vs hero image sizing.
- [ ] Understand where embedded extraction runs.

## Potential pitfalls

- Loading full-res art into grid cells.
- Blocking scanner while extracting every embedded image synchronously.
- Mixing HTTP and content URIs without Coil config.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 6.1 — Compose design system (if not already started in parallel) |

**Approval required before relying on artwork in UI phases.**
