cmake_minimum_required(VERSION 3.22.1)

project(howl_engine LANGUAGES CXX)

add_library(howl_engine SHARED howl_jni_smoke.cpp)

target_compile_features(howl_engine PRIVATE cxx_std_20)

find_library(log-lib log)
target_link_libraries(howl_engine PRIVATE ${log-lib})
