#include <jni.h>

#include "howl/dsp/eq_state.h"
#include "howl/engine_api.h"
#include "howl/status.h"

#include <cstdint>
#include <new>
#include <string>

namespace {

void throwJava(JNIEnv* env, const char* type, const char* message) {
    if (!env->ExceptionCheck()) {
        if (jclass exception = env->FindClass(type)) {
            env->ThrowNew(exception, message);
            env->DeleteLocalRef(exception);
        }
    }
}

template <typename Call>
auto callNative(JNIEnv* env, Call call) noexcept -> decltype(call()) {
    using Result = decltype(call());
    try {
        return call();
    } catch (const std::bad_alloc&) {
        throwJava(env, "java/lang/OutOfMemoryError", "Howl native allocation failed");
    } catch (...) {
        throwJava(env, "java/lang/IllegalStateException", "Unexpected Howl native failure");
    }
    return Result{};
}

using PathCall = int32_t (*)(HowlEngineHandle*, const char*);

jint callWithPath(JNIEnv* env, jlong handle, jbyteArray bytes, PathCall call) {
    return callNative(env, [&]() -> jint {
        const jsize size = bytes == nullptr ? 0 : env->GetArrayLength(bytes);
        if (size == 0) {
            return static_cast<jint>(howl::Status::InvalidArgument);
        }
        std::string path(static_cast<std::size_t>(size), '\0');
        env->GetByteArrayRegion(bytes, 0, size, reinterpret_cast<jbyte*>(path.data()));
        if (env->ExceptionCheck() || path.find('\0') != std::string::npos) {
            return static_cast<jint>(howl::Status::InvalidArgument);
        }
        return call(reinterpret_cast<HowlEngineHandle*>(handle), path.c_str());
    });
}

}  // namespace

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeCreate(JNIEnv* env, jclass /* bridge */) {
    return callNative(env, [&] {
        return reinterpret_cast<jlong>(howl_engine_create());
    });
}

JNIEXPORT void JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeDestroy(
    JNIEnv* /* env */,
    jclass /* bridge */,
    jlong handle) {
    howl_engine_destroy(reinterpret_cast<HowlEngineHandle*>(handle));
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativePrepare(
    JNIEnv* env,
    jclass /* bridge */,
    jlong handle) {
    return callNative(env, [&] {
        return howl_engine_prepare(reinterpret_cast<HowlEngineHandle*>(handle));
    });
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeStart(
    JNIEnv* env,
    jclass /* bridge */,
    jlong handle) {
    return callNative(env, [&] {
        return howl_engine_start(reinterpret_cast<HowlEngineHandle*>(handle));
    });
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeStop(
    JNIEnv* env,
    jclass /* bridge */,
    jlong handle) {
    return callNative(env, [&] {
        return howl_engine_stop(reinterpret_cast<HowlEngineHandle*>(handle));
    });
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeState(
    JNIEnv* env,
    jclass /* bridge */,
    jlong handle) {
    return callNative(env, [&] {
        return howl_engine_state(reinterpret_cast<HowlEngineHandle*>(handle));
    });
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeSetPlayMode(
    JNIEnv* env,
    jclass /* bridge */,
    jlong handle,
    jint mode) {
    return callNative(env, [&] {
        return howl_engine_set_play_mode(reinterpret_cast<HowlEngineHandle*>(handle), mode);
    });
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeLoad(
    JNIEnv* env,
    jclass /* bridge */,
    jlong handle,
    jbyteArray path) {
    return callWithPath(env, handle, path, howl_engine_load);
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativePlayMode(
    JNIEnv* env,
    jclass /* bridge */,
    jlong handle) {
    return callNative(env, [&] {
        return howl_engine_play_mode(reinterpret_cast<HowlEngineHandle*>(handle));
    });
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeEnqueue(
    JNIEnv* env,
    jclass /* bridge */,
    jlong handle,
    jbyteArray path) {
    return callWithPath(env, handle, path, howl_engine_enqueue);
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeClearQueue(
    JNIEnv* env,
    jclass /* bridge */,
    jlong handle) {
    return callNative(env, [&] {
        return howl_engine_clear_queue(reinterpret_cast<HowlEngineHandle*>(handle));
    });
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativePlayAt(
    JNIEnv* env,
    jclass /* bridge */,
    jlong handle,
    jint index) {
    return callNative(env, [&] {
        return howl_engine_play_at(reinterpret_cast<HowlEngineHandle*>(handle), index);
    });
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeNext(
    JNIEnv* env,
    jclass /* bridge */,
    jlong handle) {
    return callNative(env, [&] {
        return howl_engine_next(reinterpret_cast<HowlEngineHandle*>(handle));
    });
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativePrevious(
    JNIEnv* env,
    jclass /* bridge */,
    jlong handle) {
    return callNative(env, [&] {
        return howl_engine_previous(reinterpret_cast<HowlEngineHandle*>(handle));
    });
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeSetPreampGainDb(
    JNIEnv* env,
    jclass /* bridge */,
    jlong handle,
    jfloat gainDb) {
    return callNative(env, [&] {
        return howl_engine_set_preamp_gain_db(reinterpret_cast<HowlEngineHandle*>(handle), gainDb);
    });
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeSetEqBandGainDb(
    JNIEnv* env,
    jclass /* bridge */,
    jlong handle,
    jint band,
    jfloat gainDb) {
    return callNative(env, [&] {
        return howl_engine_set_eq_band_gain_db(reinterpret_cast<HowlEngineHandle*>(handle), band, gainDb);
    });
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeApplyEqState(
    JNIEnv* env,
    jclass /* bridge */,
    jlong handle,
    jfloat preampDb,
    jfloatArray bandsDb) {
    constexpr jsize kBandCount = static_cast<jsize>(howl::kEqBandCount);
    if (bandsDb == nullptr || env->GetArrayLength(bandsDb) != kBandCount) {
        return static_cast<jint>(howl::Status::InvalidArgument);
    }
    jfloat gains[kBandCount];
    env->GetFloatArrayRegion(bandsDb, 0, kBandCount, gains);
    if (env->ExceptionCheck()) {
        return static_cast<jint>(howl::Status::InvalidArgument);
    }
    return callNative(env, [&] {
        return howl_engine_apply_eq_state(
            reinterpret_cast<HowlEngineHandle*>(handle), preampDb, gains, kBandCount);
    });
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeResetEqToFlat(
    JNIEnv* env,
    jclass /* bridge */,
    jlong handle) {
    return callNative(env, [&] {
        return howl_engine_reset_eq_to_flat(reinterpret_cast<HowlEngineHandle*>(handle));
    });
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeSetRepeatMode(
    JNIEnv* env,
    jclass /* bridge */,
    jlong handle,
    jint mode) {
    return callNative(env, [&] {
        return howl_engine_set_repeat_mode(reinterpret_cast<HowlEngineHandle*>(handle), mode);
    });
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeQueueSize(
    JNIEnv* env,
    jclass /* bridge */,
    jlong handle) {
    return callNative(env, [&] {
        return howl_engine_queue_size(reinterpret_cast<HowlEngineHandle*>(handle));
    });
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeCurrentIndex(
    JNIEnv* env,
    jclass /* bridge */,
    jlong handle) {
    return callNative(env, [&] {
        return howl_engine_current_index(reinterpret_cast<HowlEngineHandle*>(handle));
    });
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeRepeatMode(
    JNIEnv* env,
    jclass /* bridge */,
    jlong handle) {
    return callNative(env, [&] {
        return howl_engine_repeat_mode(reinterpret_cast<HowlEngineHandle*>(handle));
    });
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativePause(
    JNIEnv* env,
    jclass /* bridge */,
    jlong handle) {
    return callNative(env, [&] {
        return howl_engine_pause(reinterpret_cast<HowlEngineHandle*>(handle));
    });
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeResume(
    JNIEnv* env,
    jclass /* bridge */,
    jlong handle) {
    return callNative(env, [&] {
        return howl_engine_resume(reinterpret_cast<HowlEngineHandle*>(handle));
    });
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeSeekMs(
    JNIEnv* env,
    jclass /* bridge */,
    jlong handle,
    jlong positionMs) {
    return callNative(env, [&] {
        return howl_engine_seek_ms(reinterpret_cast<HowlEngineHandle*>(handle), positionMs);
    });
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeEnableGaplessTransport(JNIEnv* env, jclass, jlong handle) {
    return callNative(env, [&] {
        return howl_engine_enable_gapless_transport(reinterpret_cast<HowlEngineHandle*>(handle));
    });
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeSetGaplessNext(
    JNIEnv* env, jclass, jlong handle, jbyteArray bytes, jlong token, jlong acknowledged) {
    return callNative(env, [&]() -> jint {
        std::string path;
        if (bytes) {
            const auto size = env->GetArrayLength(bytes);
            path.resize(size);
            env->GetByteArrayRegion(bytes, 0, size, reinterpret_cast<jbyte*>(path.data()));
            if (env->ExceptionCheck() || path.find('\0') != std::string::npos) {
                return static_cast<jint>(howl::Status::InvalidArgument);
            }
        }
        return howl_engine_set_gapless_next(
            reinterpret_cast<HowlEngineHandle*>(handle), bytes ? path.c_str() : nullptr, token, acknowledged);
    });
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeGetTransport(
    JNIEnv* env, jclass, jlong handle, jlongArray out) {
    if (!out) return static_cast<jint>(howl::Status::NullHandle);
    if (env->GetArrayLength(out) < 7) return static_cast<jint>(howl::Status::InvalidState);
    HowlEngineGaplessTransport snap{};
    const auto code = callNative(env, [&] {
        return howl_engine_get_gapless_transport(reinterpret_cast<HowlEngineHandle*>(handle), &snap);
    });
    if (code != 0 || env->ExceptionCheck()) return code;
    const auto& t = snap.transport;
    const jlong buf[]{t.position_frames, t.duration_frames, t.sample_rate, t.paused, t.playing,
                      snap.handoff_token, snap.mode};
    env->SetLongArrayRegion(out, 0, 7, buf);
    return code;
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeGetDiagnostics(
    JNIEnv* env,
    jclass /* bridge */,
    jlong handle,
    jlongArray out) {
    if (out == nullptr) {
        return static_cast<jint>(howl::Status::NullHandle);
    }
    const jsize len = env->GetArrayLength(out);
    if (len < 7) {
        return static_cast<jint>(howl::Status::InvalidState);
    }

    HowlEngineDiagnostics snap{};
    const int32_t code = callNative(env, [&] {
        return howl_engine_get_diagnostics(reinterpret_cast<HowlEngineHandle*>(handle), &snap);
    });
    if (code != 0 || env->ExceptionCheck()) {
        return code;
    }

    jlong buf[7];
    buf[0] = snap.sample_rate;
    buf[1] = snap.channel_count;
    buf[2] = snap.frames_per_burst;
    buf[3] = snap.frames_per_callback;
    buf[4] = snap.callback_count;
    buf[5] = snap.underrun_count;
    buf[6] = snap.error_count;
    env->SetLongArrayRegion(out, 0, 7, buf);
    return code;
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeSetAnalysisEnabled(
    JNIEnv* env,
    jclass /* bridge */,
    jlong handle,
    jboolean enabled) {
    return callNative(env, [&] {
        return howl_engine_set_analysis_enabled(reinterpret_cast<HowlEngineHandle*>(handle), enabled ? 1 : 0);
    });
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeGetAnalysis(
    JNIEnv* env,
    jclass /* bridge */,
    jlong handle,
    jfloatArray out) {
    if (out == nullptr) {
        return static_cast<jint>(howl::Status::NullHandle);
    }
    if (env->GetArrayLength(out) < 2) {
        return static_cast<jint>(howl::Status::InvalidArgument);
    }

    HowlAnalysisSnapshot snapshot{};
    const auto code = callNative(env, [&] {
        return howl_engine_get_analysis(reinterpret_cast<HowlEngineHandle*>(handle), &snapshot);
    });
    if (code == 0 && !env->ExceptionCheck()) {
        const jfloat levels[]{snapshot.peak, snapshot.rms};
        env->SetFloatArrayRegion(out, 0, 2, levels);
    }
    return code;
}

JNIEXPORT jstring JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeVersion(JNIEnv* env, jclass /* bridge */) {
    return env->NewStringUTF(howl_engine_version());
}

JNIEXPORT jstring JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeStatusString(
    JNIEnv* env,
    jclass /* bridge */,
    jint status) {
    return env->NewStringUTF(howl_engine_status_string(status));
}

JNIEXPORT jstring JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeStateString(
    JNIEnv* env,
    jclass /* bridge */,
    jint state) {
    return env->NewStringUTF(howl_engine_state_string(state));
}

}  // extern "C"
