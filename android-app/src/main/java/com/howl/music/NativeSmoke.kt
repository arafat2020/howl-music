package com.howl.music

/**
 * Thin Kotlin façade over [libhowl_engine.so] for Phase 0.3.
 * Load once; surface failures instead of crashing the Activity.
 *
 * Phase 0.4 will move this into `audio-bridge`.
 */
object NativeSmoke {
    private val loadError: String?

    init {
        loadError = try {
            System.loadLibrary("howl_engine")
            null
        } catch (t: Throwable) {
            t.message ?: t.javaClass.simpleName
        }
    }

    private external fun nativeEngineVersion(): String

    private external fun nativeAdd(a: Int, b: Int): Int

    /** Human-readable status for the placeholder UI. */
    fun statusText(): String {
        loadError?.let { return "Native load failed:\n$it" }
        return try {
            val version = nativeEngineVersion()
            val sum = nativeAdd(1, 1)
            "$version\n1+1=$sum"
        } catch (t: Throwable) {
            "Native call failed:\n${t.message ?: t.javaClass.simpleName}"
        }
    }

    val isLoaded: Boolean
        get() = loadError == null
}
