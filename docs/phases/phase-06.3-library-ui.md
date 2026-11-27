# Phase 6.3 — Library UI

**Milestone:** 6 — Visual product  
**Priority:** Foundational  
**Depends on:** 6.2, 5.2  
**Status:** Implemented

## Objective

Ship usable album/artist/track browsing with sorting/search basics and artwork-led lists/grids that can start playback via the session.

## Why it matters

Library is the everyday surface. It must stay clean and practical per the brief — not a visual playground.

## Architecture

```text
LibraryViewModel → Room flows
Library screens → play(track) → MediaController/queue commands
```

## Browse and playback

The Library has Tracks, Albums, and Artists sections. Opening an album or artist shows its tracks in place. Search matches a literal substring and ignores ASCII case. Tracks match titles, artists, and albums. Album and artist lists match their own names. The sort toggle switches between ascending and descending name order. A ViewModel keeps the query, section, selected group, sort, and page size in saved state. Room observes the underlying tables and limits each result to the requested page plus one row. The lazy list displays up to 100 results at first, with a **Load more** action.

The Library exposes the existing permission and scan screen. A track tap sends its MediaStore URI and display metadata through the app controller. The service reads the content URI on a background worker into app-private cache so the path-based native decoders can seek. Only the app can select library sources. The service removes the staged copy when the selected item is replaced or the service stops, and the mini-player uses the original URI for artwork. File playback requests the source sample rate and enables Oboe conversion when needed. [Oboe stream guide](https://github.com/google/oboe/blob/main/docs/FullGuide.md)

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

- [x] Browse artists/albums/tracks works.
- [x] Search returns expected results for sample data.
- [x] Playback starts from library.
- [x] Scrolling remains usable under playback.

## Review checklist

- [x] Lists use keys properly.
- [x] No main-thread DB queries.

## Potential pitfalls

- Loading full track list without paging on huge libraries.
- Recomposition storms from unstable lambdas (follow project Compose guidance).

## Completion report

| Field | Notes |
|-------|-------|
| What was built | Room-backed Tracks, Albums, and Artists browsing, artwork rows, detail browsing, search, sorting, and tap-to-play through MediaSession. |
| Files created/modified | Library screen and ViewModel, Room browse projections, scan entry, session source resolution, Oboe stream setup, and instrumentation. |
| Tests executed | Android build and JVM tests, Android instrumentation, JUCE-enabled native tests, and a manual emulator run with three MediaStore tracks and 2,500 additional Room entries. |
| Results | Browsing and search matched the fixtures, selecting a FLAC updated the mini-player to Playing, and the library scrolled during WAV playback. Native tests passed 62/62. Physical-device frame timing was not measured. |
| Known limitations | Native playback supports stereo WAV and FLAC. Other indexed formats can be browsed and surface a playback error. Staging a large track takes temporary cache space and may delay first playback. |
| Decisions made | Use Room Flow with capped results and explicit paging. Reuse the native player and existing artwork component. |
| Next phase | 6.4 — Now Playing |

**Approval required before Phase 6.4.**
