#include <jni.h>

#include <string>

namespace {

constexpr const char* kEngineVersion = "howl_engine/0.0.1-smoke";

}  // namespace

/**
 * Phase 0.3 JNI smoke — prove Kotlin can call into libhowl_engine.so.
 * No audio I/O. Real engine lifecycle arrives in Phase 1.1+.
 *
 * JNI naming: Java_<package>_<class>_<method> with '_' for dots/$ .
 */
extern "C" JNIEXPORT jstring JNICALL
Java_com_howl_music_NativeSmoke_nativeEngineVersion(JNIEnv* env, jobject /* thiz */) {
    return env->NewStringUTF(kEngineVersion);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_howl_music_NativeSmoke_nativeAdd(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jint a,
    jint b) {
    return a + b;
}
