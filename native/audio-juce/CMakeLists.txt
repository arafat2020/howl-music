cmake_minimum_required(VERSION 3.22.1)

# HOWL_WITH_JUCE: fetch juce_dsp (no GUI). Default ON for host, OFF for Android
# until NDK+JUCE packaging is hardened (ProcessGraph API stays identical).
if(ANDROID)
    option(HOWL_WITH_JUCE "Link juce_dsp into ProcessGraph" OFF)
else()
    option(HOWL_WITH_JUCE "Link juce_dsp into ProcessGraph" ON)
endif()

add_library(howl_audio_juce STATIC
    src/process_graph.cpp
    src/gain_node.cpp
)

target_include_directories(howl_audio_juce
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

target_compile_features(howl_audio_juce PUBLIC cxx_std_20)

if(HOWL_WITH_JUCE)
    include(FetchContent)
    FetchContent_Declare(
        juce
        GIT_REPOSITORY https://github.com/juce-framework/JUCE.git
        GIT_TAG        8.0.6
        GIT_SHALLOW    TRUE
    )
    set(JUCE_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
    set(JUCE_BUILD_EXTRAS OFF CACHE BOOL "" FORCE)
    set(JUCE_MODULES_ONLY ON CACHE BOOL "" FORCE)
    FetchContent_MakeAvailable(juce)

    # JUCE_MODULES_ONLY already registers module INTERFACE targets — do not juce_add_module again.
    # INTERFACE modules must be linked PRIVATE (JUCE CMake rule).
    target_link_libraries(howl_audio_juce
        PRIVATE
            juce::juce_dsp
    )

    target_compile_definitions(howl_audio_juce
        PUBLIC
            HOWL_WITH_JUCE=1
        PRIVATE
            JUCE_GLOBAL_MODULE_SETTINGS_INCLUDED=1
            JUCE_USE_CURL=0
            JUCE_WEB_BROWSER=0
            JUCE_STANDALONE_APPLICATION=0
            $<$<BOOL:${ANDROID}>:JUCE_ANDROID=1>
    )

    message(STATUS "howl_audio_juce: HOWL_WITH_JUCE=ON (juce_dsp 8.0.6, no GUI)")
else()
    target_compile_definitions(howl_audio_juce
        PUBLIC
            HOWL_WITH_JUCE=0
    )
    message(STATUS "howl_audio_juce: HOWL_WITH_JUCE=OFF (native ProcessGraph)")
endif()
