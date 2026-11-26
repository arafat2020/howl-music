add_library(howl_audio_engine STATIC
    src/engine_smoke.cpp
)

target_include_directories(howl_audio_engine
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

target_compile_features(howl_audio_engine PUBLIC cxx_std_20)
