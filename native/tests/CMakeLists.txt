# Host-only GoogleTest suite (Phase 1.4). Never linked into the Android .so.

include(FetchContent)
include(GoogleTest)

# Pin tag — avoid floating main. Requires network on first configure.
FetchContent_Declare(
    googletest
    GIT_REPOSITORY https://github.com/google/googletest.git
    GIT_TAG        v1.15.2
    GIT_SHALLOW    TRUE
)

set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)
set(INSTALL_GTEST OFF CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(googletest)

add_executable(howl_native_tests
    test_engine_lifecycle.cpp
    test_sine_osc.cpp
)

target_link_libraries(howl_native_tests
    PRIVATE
        howl_audio_engine
        GTest::gtest_main
)

target_compile_features(howl_native_tests PRIVATE cxx_std_20)

gtest_discover_tests(howl_native_tests
    DISCOVERY_MODE PRE_TEST
)