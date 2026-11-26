add_library(howl_audio_engine STATIC
    src/engine.cpp
    src/engine_smoke.cpp
)

target_include_directories(howl_audio_engine
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

target_compile_features(howl_audio_engine PUBLIC cxx_std_20)

if(ANDROID)
    # engine.h includes audio_output.h on Android; symbols come from howl_audio_android.
    # sine_osc.h is header-only in this include tree.
    target_include_directories(howl_audio_engine
        PRIVATE
            ${CMAKE_CURRENT_SOURCE_DIR}/../audio-android/include
    )
endif()
