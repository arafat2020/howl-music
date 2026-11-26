cmake_minimum_required(VERSION 3.22.1)

project(howl_bridge_native LANGUAGES CXX)

# Repo-root native/ tree (cpp -> main -> src -> audio-bridge -> root)
set(HOWL_NATIVE_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../../../../native")
add_subdirectory("${HOWL_NATIVE_ROOT}" "${CMAKE_BINARY_DIR}/howl-native")

add_library(howl_engine SHARED
    howl_jni_smoke.cpp
    howl_jni_engine.cpp
)

target_compile_features(howl_engine PRIVATE cxx_std_20)

find_library(log-lib log)
target_link_libraries(howl_engine
    PRIVATE
        howl_audio_engine
        ${log-lib}
)
