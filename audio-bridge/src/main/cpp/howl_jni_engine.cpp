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
