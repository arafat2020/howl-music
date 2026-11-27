#include <jni.h>

#include "howl/engine.h"

#include <cstdint>

/**
 * Phase 1.3 — thin JNI over Engine lifecycle + diagnostics C API.
 * Package: com.howl.music.bridge.HowlEngine
 */
extern "C" {

JNIEXPORT jlong JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeCreate(JNIEnv* /* env */, jobject /* thiz */) {
    return reinterpret_cast<jlong>(howl_engine_create());
}

JNIEXPORT void JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeDestroy(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong handle) {
    howl_engine_destroy(reinterpret_cast<HowlEngineHandle*>(handle));
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativePrepare(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong handle) {
    return howl_engine_prepare(reinterpret_cast<HowlEngineHandle*>(handle));
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeStart(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong handle) {
    return howl_engine_start(reinterpret_cast<HowlEngineHandle*>(handle));
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeStop(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong handle) {
    return howl_engine_stop(reinterpret_cast<HowlEngineHandle*>(handle));
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeState(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong handle) {
    return howl_engine_state(reinterpret_cast<HowlEngineHandle*>(handle));
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeSetPlayMode(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong handle,
    jint mode) {
    return howl_engine_set_play_mode(reinterpret_cast<HowlEngineHandle*>(handle), mode);
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeLoad(
    JNIEnv* env,
    jobject /* thiz */,
    jlong handle,
    jstring path) {
    if (path == nullptr) {
        return static_cast<jint>(howl::Status::NullHandle);
    }
    const char* utf = env->GetStringUTFChars(path, nullptr);
    if (utf == nullptr) {
        return static_cast<jint>(howl::Status::NullHandle);
    }
    const int32_t code = howl_engine_load(reinterpret_cast<HowlEngineHandle*>(handle), utf);
    env->ReleaseStringUTFChars(path, utf);
    return code;
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativePlayMode(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong handle) {
    return howl_engine_play_mode(reinterpret_cast<HowlEngineHandle*>(handle));
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeEnqueue(
    JNIEnv* env,
    jobject /* thiz */,
    jlong handle,
    jstring path) {
    if (path == nullptr) {
        return static_cast<jint>(howl::Status::NullHandle);
    }
    const char* utf = env->GetStringUTFChars(path, nullptr);
    if (utf == nullptr) {
        return static_cast<jint>(howl::Status::NullHandle);
    }
    const int32_t code = howl_engine_enqueue(reinterpret_cast<HowlEngineHandle*>(handle), utf);
    env->ReleaseStringUTFChars(path, utf);
    return code;
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeClearQueue(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong handle) {
    return howl_engine_clear_queue(reinterpret_cast<HowlEngineHandle*>(handle));
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativePlayAt(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong handle,
    jint index) {
    return howl_engine_play_at(reinterpret_cast<HowlEngineHandle*>(handle), index);
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeNext(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong handle) {
    return howl_engine_next(reinterpret_cast<HowlEngineHandle*>(handle));
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativePrevious(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong handle) {
    return howl_engine_previous(reinterpret_cast<HowlEngineHandle*>(handle));
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeSetPreampGainDb(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong handle,
    jfloat gainDb) {
    return howl_engine_set_preamp_gain_db(reinterpret_cast<HowlEngineHandle*>(handle), gainDb);
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeSetEqBandGainDb(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong handle,
    jint band,
    jfloat gainDb) {
    return howl_engine_set_eq_band_gain_db(reinterpret_cast<HowlEngineHandle*>(handle), band, gainDb);
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeApplyEqState(
    JNIEnv* env,
    jobject /* thiz */,
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
    return howl_engine_apply_eq_state(
        reinterpret_cast<HowlEngineHandle*>(handle), preampDb, gains, kBandCount);
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeResetEqToFlat(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong handle) {
    return howl_engine_reset_eq_to_flat(reinterpret_cast<HowlEngineHandle*>(handle));
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeSetRepeatMode(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong handle,
    jint mode) {
    return howl_engine_set_repeat_mode(reinterpret_cast<HowlEngineHandle*>(handle), mode);
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeQueueSize(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong handle) {
    return howl_engine_queue_size(reinterpret_cast<HowlEngineHandle*>(handle));
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeCurrentIndex(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong handle) {
    return howl_engine_current_index(reinterpret_cast<HowlEngineHandle*>(handle));
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeRepeatMode(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong handle) {
    return howl_engine_repeat_mode(reinterpret_cast<HowlEngineHandle*>(handle));
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativePause(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong handle) {
    return howl_engine_pause(reinterpret_cast<HowlEngineHandle*>(handle));
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeResume(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong handle) {
    return howl_engine_resume(reinterpret_cast<HowlEngineHandle*>(handle));
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeSeekMs(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong handle,
    jlong positionMs) {
    return howl_engine_seek_ms(reinterpret_cast<HowlEngineHandle*>(handle), positionMs);
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeGetTransport(
    JNIEnv* env,
    jobject /* thiz */,
    jlong handle,
    jlongArray out) {
    if (out == nullptr) {
        return static_cast<jint>(howl::Status::NullHandle);
    }
    if (env->GetArrayLength(out) < 5) {
        return static_cast<jint>(howl::Status::InvalidState);
    }
    HowlTransportSnapshot snap{};
    const int32_t code =
        howl_engine_get_transport(reinterpret_cast<HowlEngineHandle*>(handle), &snap);
    if (code != 0) {
        return code;
    }
    jlong buf[5];
    buf[0] = snap.position_frames;
    buf[1] = snap.duration_frames;
    buf[2] = snap.sample_rate;
    buf[3] = snap.paused;
    buf[4] = snap.playing;
    env->SetLongArrayRegion(out, 0, 5, buf);
    return code;
}

JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeGetDiagnostics(
    JNIEnv* env,
    jobject /* thiz */,
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
    const int32_t code =
        howl_engine_get_diagnostics(reinterpret_cast<HowlEngineHandle*>(handle), &snap);
    if (code != 0) {
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

JNIEXPORT jstring JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeVersion(JNIEnv* env, jobject /* thiz */) {
    return env->NewStringUTF(howl_engine_version());
}

JNIEXPORT jstring JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeStatusString(
    JNIEnv* env,
    jobject /* thiz */,
    jint status) {
    return env->NewStringUTF(howl_engine_status_string(status));
}

JNIEXPORT jstring JNICALL
Java_com_howl_music_bridge_HowlEngine_nativeStateString(
    JNIEnv* env,
    jobject /* thiz */,
    jint state) {
    return env->NewStringUTF(howl_engine_state_string(state));
}

}  // extern "C"
