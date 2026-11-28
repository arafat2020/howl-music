#!/usr/bin/env bash
set -euo pipefail

if [[ $# != 3 ]]; then
    echo "Usage: $0 SERIAL MEDIASTORE_URI OUTPUT_DIR" >&2
    exit 2
fi

serial=$1
uri=$2
out=$3
package=${HOWL_PROFILE_PACKAGE:-com.howl.music}
scenarios=${HOWL_PROFILE_SCENARIOS:-now-playing,eq,visualizer,background}
seconds=${HOWL_PROFILE_SECONDS:-15}
background_seconds=${HOWL_PROFILE_BACKGROUND_SECONDS:-1800}
setup_seconds=${HOWL_PROFILE_SETUP_SECONDS:-12}

if [[ ! $uri =~ ^content://media/[a-zA-Z0-9_-]+/audio/media/[0-9]+$ ||
      ! $package =~ ^[a-zA-Z][a-zA-Z0-9_.]+$ ||
      ! $scenarios =~ ^(background|now-playing|eq|visualizer)(,(background|now-playing|eq|visualizer))*$ ||
      ! $seconds =~ ^[0-9]+$ || ! $background_seconds =~ ^[0-9]+$ || ! $setup_seconds =~ ^[0-9]+$ ]]; then
    echo "Invalid profiling package, MediaStore URI, scenario list or duration." >&2
    exit 2
fi

mkdir -p "$out"
if [[ -e "$out/profile.log" ]]; then
    echo "Choose an output directory without an existing profile.log." >&2
    exit 2
fi
device=(adb -s "$serial")
capture_battery() {
    "${device[@]}" shell dumpsys battery > "$out/battery-$1.txt"
    "${device[@]}" shell dumpsys batterystats --charged "$package" > "$out/batterystats-$1.txt"
}

printf 'package=%s\ntrack=%s\nscenarios=%s\nforegroundSeconds=%s\nbackgroundSeconds=%s\nsetupSeconds=%s\n' \
    "$package" "$uri" "$scenarios" "$seconds" "$background_seconds" "$setup_seconds" > "$out/device.txt"
"${device[@]}" shell getprop ro.product.model >> "$out/device.txt"
"${device[@]}" shell getprop ro.build.fingerprint >> "$out/device.txt"
"${device[@]}" shell dumpsys package "$package" |
    awk '/version(Name|Code)=/ {print}' >> "$out/device.txt"
capture_battery before
"${device[@]}" shell am instrument -w -r \
    -e profileScenarios "$scenarios" -e profileTrackUri "$uri" \
    -e profileSeconds "$seconds" -e profileBackgroundSeconds "$background_seconds" \
    -e profileSetupSeconds "$setup_seconds" \
    "$package.test/com.howl.music.EngineApiInstrumentation" | tee "$out/profile.log"
capture_battery after
grep -q '^INSTRUMENTATION_CODE: -1' "$out/profile.log"
