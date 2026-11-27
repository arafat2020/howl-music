# Phase 6.1 — Compose Design System

**Milestone:** 6 — Visual product  
**Priority:** Foundational  
**Depends on:** 0.2  
**Status:** Implemented and verified on the Android emulator

## Objective

Build a reusable Compose design system from the brief: color tokens, typography, shapes, spacing, and core components — before proliferating screens.

## Why it matters

Without a system, screens diverge and the “premium” identity collapses into one-off styling. Brief requires coherent design across sizes.

## Architecture

```text
ui/theme/
  Color.kt  Typography.kt  Shape.kt  Theme.kt
ui/components/
  HowlButton, HowlSlider, List rows, etc. (minimal set)
```

Follow brief palette; dark-first. Expressive sans font (not default Inter/system-only if we can license/bundle appropriately — document font choice).

## Tokens and components

| Role | Dark theme | Light theme |
|---|---|---|
| Canvas | `#101014` | `#F9F7F2` |
| Primary | `#B7F34A` | `#395500` |
| Secondary | `#B65CFF` | `#7026A6` |
| Warm accent | `#FF6B47` | `#A6351A` |
| Main text | `#E9E5DC` | `#1B1A20` |

`HowlTheme` maps these roles to Material 3. Dark is the app default. The debug gallery can preview the light color scheme, which darkens accent text and controls to preserve contrast. Spacing uses 4, 8, 12, 16, 24, and 32 dp tokens. Corners range from 6 to 28 dp. The reusable controls use a minimum 48 dp touch target and retain Material pressed and disabled behavior.

The gallery shows the palette, type scale, filled and outlined buttons, an icon button, a slider, and selectable rows. Its grid adapts to wider windows. The reduced-motion preview starts from the stored preference and replaces the demo row animation with an immediate change.

## Font

Bundle Barlow by Jeremy Tribby in regular, medium, semibold, and bold. It is an expressive sans serif with legible small metadata. Its font files and SIL Open Font License 1.1 are packaged with the app. [Barlow project and license](https://github.com/jpt/barlow)

## Contrast

Primary text on the canvas measures 15.10:1 in dark and 16.14:1 in light. Text on primary buttons measures 14.40:1 and 8.49:1 respectively. All seven checked foreground/background pairs in each theme met 4.5:1. [WCAG minimum contrast](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum)

## Learning objectives

- Design tokens in Compose.
- Theming with `MaterialTheme` customization vs fully custom.
- Component API consistency.

## Scope

**Included**
- Color roles from brief.
- Typography scale for titles/metadata.
- Spacing/shape conventions.
- Few base components used by later screens.
- Light accessibility contrast check on primary text.

**Excluded**
- Full screen builds (6.2+).
- Visualizer shaders.
- Rive/Lottie/Filament.

**Must not implement yet**
- One-off colors inside feature screens that bypass tokens.

## Implementation steps

1. Encode tokens as Compose `Color` / `TextStyle`.
2. Create `HowlTheme`.
3. Build Button / IconButton / Slider primitives.
4. Gallery debug screen listing components.
5. Record font licensing decision.

## Expected project structure

```text
android-app/src/main/java/.../ui/theme/
android-app/src/main/java/.../ui/components/
android-app/.../ui/debug/ComponentGallery.kt
```

## Testing strategy

- Screenshot/manual review of gallery on phone + tablet width.
- Toggle dark theme (dark primary).
- Reduced-motion preference stub respected by any demo animations.

## Acceptance criteria

- [x] Dark tokens match the five brief roles.
- [x] Debug gallery demonstrates colors, typography, controls, and list rows.
- [x] Font choice and redistribution license documented.
- [x] Existing screens consume the shared theme. Navigation and new feature screens are deferred.

## Review checklist

- [ ] Can map UI colors to token roles.
- [ ] Components have clear enabled/pressed/disabled states.

## Potential pitfalls

- Purple-on-everything noise despite brief secondary accent — use accents selectively.
- Overbuilding a huge component library unused by screens.
- Ignoring touch target sizes.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | Shared dark and light Compose theme, typography, spacing, shapes, controls, and responsive debug gallery. |
| Files created/modified | Theme and component sources, gallery, debug manifest, existing Compose roots, bundled fonts and license. |
| Tests executed | Android debug APK build and unit test, emulator instrumentation, contrast checks, and screenshot review at phone width and an 800 dp tablet-width configuration. |
| Results | Verified the dark and light gallery, reduced-motion preview, play/pause control, selectable rows, and side-by-side sections at tablet width. The checked text pairs met 4.5:1. |
| Known limitations | The gallery uses emulator dimensions for phone and tablet review. Its theme and motion switches are previews. |
| Decisions made | Keep the brief palette exact in dark mode and tune light-mode accents for contrast. Bundle Barlow for offline loading. |
| Next phase | 6.2 — Navigation shell + mini-player |

**Approval required before Phase 6.2.**
