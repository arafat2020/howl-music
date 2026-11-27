package com.howl.music.bridge

/** Compatibility entry point for the original JNI bring-up screen. */
@Deprecated("Use HowlEngine for native playback and version checks.")
object NativeSmoke {
    fun statusText(): String {
        HowlEngine.libraryError()?.let { return "Native load failed:\n$it" }
        return "${HowlEngine.version()}\n1+1=2"
    }

    val isLoaded: Boolean
        get() = HowlEngine.libraryError() == null
}
