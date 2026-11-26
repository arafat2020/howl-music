cmake_minimum_required(VERSION 3.22.1)

# Pure C++ targets. Linked into libhowl_engine.so by audio-bridge.
project(howl_native LANGUAGES CXX)

add_subdirectory(audio-engine)

if(ANDROID)
    add_subdirectory(audio-android)
endif()

# Placeholder for later phases.
# add_subdirectory(audio-juce)     # Phase 3.1 JUCE DSP

# Host GoogleTest suite (Phase 1.4). Skipped inside the Android NDK build.
option(HOWL_BUILD_TESTS "Build host GoogleTest targets" ON)
if(HOWL_BUILD_TESTS AND NOT ANDROID)
    enable_testing()  # must be top-level so `ctest` at build-host root works
    add_subdirectory(tests)
endif()
