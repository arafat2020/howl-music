# Phase 5.4 — Artwork Loading & Cache

**Milestone:** 5 — Library & data  
**Priority:** Foundational  
**Depends on:** 5.2  
**Status:** Implemented — physical-device scroll performance pending

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

## Loading and cache policy

On Android 10 and later, Howl asks MediaStore for a thumbnail of the track URI. If the provider has no thumbnail, or on earlier Android versions, Howl tries the track's embedded cover. Requests use an I/O dispatcher, at most two concurrent artwork fetches, and bounded bitmap sizes. Files with unavailable or invalid artwork display a music-note placeholder. [Android shared media guide](https://developer.android.com/training/data-storage/shared/media)

The reusable `AlbumArt` composable rounds requests to 128, 256, 512, or 1,024 pixels. The debug preview uses 56 dp rows and a 224 dp detail view. The shared Coil loader keeps decoded covers within 15% of the app's memory class. Cache keys include the track URI, file modification time and length, and size bucket. A new request picks up a changed file revision. Howl leaves disk thumbnails to MediaStore and reads embedded covers from the audio file after a cold start. [Coil image loaders](https://coil-kt.github.io/coil/image_loaders/)

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

- [x] MediaStore audio with a cover displays artwork on the emulator.
- [x] A recreated request for the same cover and size hits Coil’s memory cache.
- [ ] Measure scrolling on a physical mid-range device with a real library.
- [x] The existing playback and audio focus instrumentation passes.

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
| What was built | Coil-backed local cover resolver, size-aware AlbumArt component, placeholder, and debug list and detail preview. |
| Files created/modified | Artwork repository, composable, debug Activity and launcher entry, dependency catalog, fixture, and instrumentation. |
| Tests executed | Android debug build and unit test, full emulator instrumentation, and artwork request checks for MediaStore, local embedded art, cache hits, size buckets, invalidation, and missing covers. Also checked the debug preview before and after a process restart. |
| Results | MediaStore artwork rendered in the debug preview after restart. A WAV with no cover showed the placeholder. A repeated request hit the Coil memory cache, and a file revision change produced a new key. |
| Known limitations | Physical-device scroll performance remains unverified. Local artwork is resolved per track URI. |
| Decisions made | Share a bounded Coil memory cache and rely on MediaStore for disk thumbnails. Keep artwork extraction out of library scanning and playback. |
| Next phase | 6.1 — Compose design system (if not already started in parallel) |

**Approval required before relying on artwork in UI phases.**
