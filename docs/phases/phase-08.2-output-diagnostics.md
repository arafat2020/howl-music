# Phase 8.2 — Output / Sample-Rate Diagnostics

**Milestone:** 8 — Device validation  
**Priority:** Release-hardening  
**Depends on:** 1.3, 4.1  
**Status:** Not started

## Objective

Expose user-visible (and developer-visible) diagnostics for audio format, sample rate, output route label, underruns, and processing state — matching the product success criterion for clear diagnostics.

## Why it matters

Power users trust players that show what the output path is doing. It also prevents false marketing claims.

## Architecture

```text
Engine diagnostics → Bridge → Settings/Diagnostics screen
Include: source format, process rate, device rate, channels, xruns, EQ active
```

## Learning objectives

- Output route awareness limits on Android.
- Distinguishing source rate vs stream rate vs device rate.
- Communicating uncertainty honestly in UI copy.

## Scope

**Included**
- Diagnostics screen in app.
- Fields from engine + best-effort route info from Android APIs.
- Copy that avoids unverified bit-perfect claims.

**Excluded**
- USB DAC deep controls (8.3).
- Automated claims of 24-bit/192 kHz support.

**Must not implement yet**
- Fake “bit perfect” badges.

## Implementation steps

1. Extend diagnostics snapshot if needed.
2. Build Diagnostics UI using design system.
3. Verify values against known test signal configuration.
4. Write user-facing explanations for each field.

## Expected project structure

```text
android-app/.../ui/settings/DiagnosticsScreen.kt
```

## Testing strategy

- Play 44.1 vs 48 material; observe fields.
- Toggle EQ; processing state updates.
- Compare with `adb dumpsys audio` notes where useful.

## Acceptance criteria

- [ ] Diagnostics screen shows format, rates, processing, underruns.
- [ ] Values match engine configuration in controlled tests.
- [ ] UI language is precise (no overclaim).
- [ ] Accessible from settings.

## Review checklist

- [ ] Source vs output rate distinction clear.
- [ ] Route field labeled as best-effort if uncertain.

## Potential pitfalls

- Trusting UI labels without checking Oboe actual stream.
- Showing marketing sample rates that never reach the DAC.

## Completion report

| Field | Notes |
|-------|-------|
| What was built | |
| Files created/modified | |
| Tests executed | |
| Results | |
| Known limitations | |
| Decisions made | |
| Next phase | 8.3 USB (optional) or 8.4 release checklist |

**Approval required before next phase.**
