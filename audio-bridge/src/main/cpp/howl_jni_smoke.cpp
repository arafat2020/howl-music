#include <jni.h>

#include "howl/engine_smoke.h"

/**
 * Phase 0.4 — JNI lives only in audio-bridge.
 * Package: com.howl.music.bridge.NativeSmoke
 */
extern "C" JNIEXPORT jstring JNICALL
Java_com_howl_music_bridge_NativeSmoke_nativeEngineVersion(JNIEnv* env, jobject /* thiz */) {
    return env->NewStringUTF(howl_engine_smoke_version());
}

extern "C" JNIEXPORT jint JNICALL
Java_com_howl_music_bridge_NativeSmoke_nativeAdd(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jint a,
    jint b) {
    return a + b;
}
