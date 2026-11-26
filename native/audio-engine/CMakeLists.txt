add_library(howl_audio_engine STATIC
    src/engine.cpp
    src/engine_smoke.cpp
)

target_include_directories(howl_audio_engine
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

target_compile_features(howl_audio_engine PUBLIC cxx_std_20)

# Host lifecycle test (Phase 1.1) — skipped when building inside Android NDK.
if(NOT ANDROID)
    add_executable(howl_engine_lifecycle_test
        ../tests/test_engine_lifecycle.cpp
    )
    target_link_libraries(howl_engine_lifecycle_test PRIVATE howl_audio_engine)
    target_compile_features(howl_engine_lifecycle_test PRIVATE cxx_std_20)
endif()
