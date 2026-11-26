add_library(howl_audio_android STATIC
    src/oboe_output.cpp
)

target_include_directories(howl_audio_android
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

target_compile_features(howl_audio_android PUBLIC cxx_std_20)

# status.h / sine_osc live in audio-engine; ProcessGraph in audio-juce.
target_include_directories(howl_audio_android
    PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/../audio-engine/include
)

find_package(oboe REQUIRED CONFIG)
target_link_libraries(howl_audio_android
    PUBLIC
        oboe::oboe
    PRIVATE
        howl_audio_juce
)
