# Phase 6.1 — Compose Design System

**Milestone:** 6 — Visual product  
**Priority:** Foundational  
**Depends on:** 0.2  
**Status:** Not started

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

- [ ] Tokens match brief roles.
- [ ] Gallery demonstrates components.
- [ ] Font choice documented.
- [ ] No feature screens required to complete this phase.

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
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 6.2 — Navigation shell + mini-player |

**Approval required before Phase 6.2.**
