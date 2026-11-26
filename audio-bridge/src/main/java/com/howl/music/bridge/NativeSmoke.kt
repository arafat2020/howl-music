package com.howl.music.bridge

/**
 * Kotlin façade over [libhowl_engine.so].
 * This module is the only place that should [System.loadLibrary] the engine.
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
