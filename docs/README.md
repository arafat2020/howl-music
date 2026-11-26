# Howl Music — Engineering Docs

Source of truth for product intent: [`../premium_music_player_product_brief.pdf`](../premium_music_player_product_brief.pdf) (v1.0, 25 November 2026).

These docs turn that brief into an incremental, reviewable engineering plan. **Do not start implementation until Phase 0 is explicitly approved.**

## Document map

| Doc | Purpose |
|-----|---------|
| [01 — Product understanding](01-product-understanding.md) | Concise product objective and success criteria |
| [02 — Architecture review](02-architecture-review.md) | Architecture assessment, gaps, open decisions |
| [03 — Project setup](03-project-setup.md) | Toolchain, SDK/NDK, build & debug workflow |
| [04 — Repository structure](04-repository-structure.md) | Modules, directories, ownership boundaries |
| [05 — Phased roadmap](05-phased-roadmap.md) | Full phase index, dependencies, foundational vs optional |

## Phase specs

Each phase lives under [`phases/`](phases/) and follows the mandatory mentoring template (objective, architecture, scope, steps, tests, acceptance, review checklist, pitfalls, completion report).

Start here: [Phase 0.1 — Development environment](phases/phase-00.1-dev-environment.md)

## Working rules (summary)

1. One clear objective per phase; no feature dumping.
2. Explain before implementing.
3. Small, reviewable diffs; you implement straightforward parts where useful.
4. Audio callback stays real-time safe — no I/O, locks with unbounded waits, JNI, allocs, or UI work.
5. A phase is complete only after acceptance criteria are validated — not when code is generated.
6. Explicit approval is required before starting the next phase.

## Project state snapshot

| Field | Value |
|-------|-------|
| Current phase | **1.2 — Oboe test signal** (audible on emulator; awaiting approval) |
| Completed phases | 0.1–0.4, 1.1 |
| Implementation status | Oboe Prefab output + 440 Hz sine; Start/Stop UI; audible tone confirmed on emulator |
| Technologies integrated | Prior + Oboe 1.9.3 (Prefab), `audio-android` |
| Outstanding issues | Open arch decisions in [02](02-architecture-review.md) (decoder, JUCE license, …) |
| Next proposed phase | Phase 1.3 — Underrun & callback diagnostics (after 1.2 approval) |
