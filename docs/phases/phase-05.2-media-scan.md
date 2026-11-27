# Phase 5.2 — Media Scanning & Permissions

**Milestone:** 5 — Library & data  
**Priority:** Foundational  
**Depends on:** 5.1  
**Status:** Implemented and verified on the Android emulator

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

## Source and identity

The debug **Scan music** screen requests `READ_MEDIA_AUDIO` on Android 13 and later and `READ_EXTERNAL_STORAGE` on Android 8 through 12L. Scanning uses mounted shared-storage MediaStore audio collections. Rows must be marked as music and have an audio MIME type. The Storage Access Framework can be added separately for user-selected files or directories outside this index. [Android shared media storage guide](https://developer.android.com/training/data-storage/shared/media)

A track uses the complete volume-specific MediaStore content URI as its key. Artist and album keys use the MediaStore volume, entity type, and provider ID. Rescans update existing local row IDs and preserve playlist references. Albums are grouped by provider ID, and the scan leaves the album-artist field unset unless it already has a value. MediaStore paths are unnecessary for indexing. Existing tracks remain indexed when absent from a scan, since permissions and mounted volumes can change.

The scanner streams records off the main thread. The repository commits batches of up to 128 rows in Room transactions and publishes progress after each commit. Cancellation signals the provider query and discards uncommitted work. Completed batches are retained and can be rescanned.

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

- [x] Denial and grant flows verified on the API 36 emulator with target SDK 35.
- [x] Tracks appear in Room after a MediaStore scan.
- [x] Scanner and Room writes run off the UI thread. Cancellation retains completed batches.
- [x] Duplicate strategy documented.

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
| What was built | Debug permission and scan screen, MediaStore reader, and transactional Room import with progress and cancellation. |
| Files created/modified | Android manifests, debug scan Activity and launcher entry, scanner, scan repository, library DAO, and instrumentation. |
| Tests executed | Android debug and instrumentation APK builds, app unit test, cancellation and 2,500-row in-memory import checks, and a MediaStore WAV-file scan on the API 36 emulator. |
| Results | Rescans preserved local IDs and playlist links. Cancelling kept the first 128-row batch. The 2,500-row synthetic import completed in 71 ms on one emulator run. The permission dialog handled denial and grant. One MediaStore WAV row appeared in Room and a second scan retained a single row. |
| Known limitations | Physical-device scan timing and the Android 8–12L permission path remain unverified. The scanner indexes content URIs for the library. Selection and playback from that library are deferred. |
| Decisions made | Use MediaStore on mounted shared storage. Retain previously indexed rows when the current scan cannot see them. Keep permission and scan controls in the debug UI. |
| Next phase | 5.3 — Playlists & preferences |

**Approval required before Phase 5.3.**
