cmake_minimum_required(VERSION 3.22.1)

# Pure C++ targets. Linked into libhowl_engine.so by audio-bridge.
project(howl_native LANGUAGES CXX)

add_subdirectory(audio-engine)

# Placeholders for later phases — do not link into howl_engine yet.
# add_subdirectory(audio-android)  # Phase 1.2 Oboe
# add_subdirectory(audio-juce)     # Phase 3.1 JUCE DSP
