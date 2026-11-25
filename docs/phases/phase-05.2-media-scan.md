# Phase 5.2 — Media Scanning & Permissions

**Milestone:** 5 — Library & data  
**Priority:** Foundational  
**Depends on:** 5.1  
**Status:** Not started

## Objective

Scan on-device audio into Room with correct storage permissions for the target SDK, and keep the UI responsive during large scans.

## Why it matters

A player without a library is a debug tone app. Permission and scan performance define first-run quality.

## Architecture

```text
Permission flow → MediaStore query / SAF (decision)
  → metadata extract
  → Room upsert (batch)
  → progress Flow to UI
```

Prefer MediaStore where possible on modern Android; document SAF needs.

## Learning objectives

- `READ_MEDIA_AUDIO` vs legacy storage permissions.
- MediaStore columns for music.
- Batching DB writes; cancellation.

## Scope

**Included**
- Permission request UX (functional, not final visual polish).
- Scanner producing Track/Album/Artist rows.
- Progress + cancellation.
- Basic large-library test (thousands of rows synthetic or real).

**Excluded**
- Perfect ID3 edge cases worldwide.
- Cloud libraries.
- Artwork extraction polish (5.4).

**Must not implement yet**
- Scanning on the audio thread / inside engine.

## Implementation steps

1. Lock permission strategy for min/target SDK.
2. Implement scanner UseCase + repository.
3. Upsert logic (stable IDs).
4. Hook a debug “Scan” button.
5. Measure scan time on a real device library.

## Expected project structure

```text
android-app/.../data/scan/MediaScanner.kt
android-app/.../ui/debug/ScanScreen.kt  # temporary OK
```

## Testing strategy

- Deny permission → graceful message.
- Grant → rows appear.
- Rescan updates without duplicating incorrectly.
- UI remains interactive (progress indicator).

## Acceptance criteria

- [ ] Permission flow works on target SDK.
- [ ] Tracks appear in DB after scan.
- [ ] Scan is cancellable and non-blocking for UI.
- [ ] Duplicate strategy documented.

## Review checklist

- [ ] Know MediaStore vs file-path trade-offs.
- [ ] Understand upsert keys.

## Potential pitfalls

- Scoped storage assumptions wrong for min SDK.
- Scanning on main thread.
- Treating every file as music without MIME filters.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 5.3 — Playlists & preferences |

**Approval required before Phase 5.3.**
