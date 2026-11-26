add_library(howl_audio_engine STATIC
    src/engine.cpp
    src/engine_smoke.cpp
    src/wav_decoder.cpp
    src/pcm_ring_buffer.cpp
    src/decode_worker.cpp
    src/queue_controller.cpp
)

target_include_directories(howl_audio_engine
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

target_compile_features(howl_audio_engine PUBLIC cxx_std_20)

# DecodeWorker uses std::thread.
find_package(Threads REQUIRED)
target_link_libraries(howl_audio_engine PUBLIC Threads::Threads)

if(ANDROID)
    # engine.h includes audio_output.h on Android; symbols come from howl_audio_android.
    target_include_directories(howl_audio_engine
        PRIVATE
            ${CMAKE_CURRENT_SOURCE_DIR}/../audio-android/include
    )
endif()
