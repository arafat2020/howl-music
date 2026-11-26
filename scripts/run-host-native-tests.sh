#!/usr/bin/env bash
# Phase 1.4 — build and run host GoogleTest suite (no Android device).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_DIR="${HOWL_NATIVE_BUILD_DIR:-$ROOT/native/build-host}"

# Prefer SDK CMake (matches AGP); fall back to PATH.
CMAKE_BIN="${CMAKE_BIN:-}"
if [[ -z "$CMAKE_BIN" ]]; then
  if [[ -n "${ANDROID_HOME:-}" && -x "$ANDROID_HOME/cmake/3.22.1/bin/cmake" ]]; then
    CMAKE_BIN="$ANDROID_HOME/cmake/3.22.1/bin/cmake"
  elif [[ -n "${ANDROID_SDK_ROOT:-}" && -x "$ANDROID_SDK_ROOT/cmake/3.22.1/bin/cmake" ]]; then
    CMAKE_BIN="$ANDROID_SDK_ROOT/cmake/3.22.1/bin/cmake"
  elif command -v cmake >/dev/null 2>&1; then
    CMAKE_BIN="$(command -v cmake)"
  else
    echo "cmake not found. Set ANDROID_HOME or CMAKE_BIN." >&2
    exit 1
  fi
fi

mkdir -p "$BUILD_DIR"
"$CMAKE_BIN" -S "$ROOT/native" -B "$BUILD_DIR" \
  -DHOWL_BUILD_TESTS=ON \
  -DCMAKE_EXPORT_COMPILE_COMMANDS=ON
"$CMAKE_BIN" --build "$BUILD_DIR" --target howl_native_tests

# Prefer ctest (discovered cases); fall back to the binary if needed.
if command -v ctest >/dev/null 2>&1; then
  ctest --test-dir "$BUILD_DIR" --output-on-failure
else
  "$BUILD_DIR/tests/howl_native_tests"
fi
