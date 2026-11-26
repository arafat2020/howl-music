#include <jni.h>

#include "howl/engine.h"

/**
 * Phase 1.1 — thin JNI over Engine lifecycle C API.
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
