package com.howl.music.bridge

import java.io.Closeable

/**
 * Control-thread façade over the native Engine (Phase 1.3).
 *
 * Call [prepare]/[start]/[stop]/[diagnostics] from one control thread only —
 * never from an audio callback.
 */
class HowlEngine private constructor(
    private var handle: Long,
) : Closeable {

    val isClosed: Boolean
        get() = handle == 0L

    fun prepare(): Int = nativePrepare(requireHandle())

    fun start(): Int = nativeStart(requireHandle())

    fun stop(): Int = nativeStop(requireHandle())

    fun state(): Int = nativeState(requireHandle())

    fun stateName(): String = nativeStateString(state())

    fun statusName(code: Int): String = nativeStatusString(code)

    /**
     * Snapshot of stream config + callback counters.
     * Safe to call while running; cheap atomic loads on the native side.
     */
    fun diagnostics(): EngineDiagnostics {
        val buf = LongArray(FIELD_COUNT)
        val code = nativeGetDiagnostics(requireHandle(), buf)
        if (code != 0) {
            return EngineDiagnostics.Empty
        }
        return EngineDiagnostics(
            sampleRate = buf[0].toInt(),
            channelCount = buf[1].toInt(),
            framesPerBurst = buf[2].toInt(),
            framesPerCallback = buf[3].toInt(),
            callbackCount = buf[4],
            underrunCount = buf[5],
            errorCount = buf[6],
        )
    }

    /**
     * create → start → start(fail) → stop → destroy style check for UI/debug.
     */
    fun lifecycleSmokeReport(): String {
        val lines = mutableListOf<String>()
        lines += version()
        lines += "state=${stateName()}"

        val start1 = start()
        lines += "start#1=${statusName(start1)} state=${stateName()}"

        val start2 = start()
        lines += "start#2=${statusName(start2)} (expect AlreadyRunning)"

        val stop1 = stop()
        lines += "stop#1=${statusName(stop1)} state=${stateName()}"

        val stop2 = stop()
        lines += "stop#2=${statusName(stop2)} (idempotent)"

        return lines.joinToString("\n")
    }

    override fun close() {
        if (handle != 0L) {
            nativeDestroy(handle)
            handle = 0L
        }
    }

    private fun requireHandle(): Long {
        check(handle != 0L) { "HowlEngine already closed" }
        return handle
    }

    companion object {
        /** Must match JNI packing in howl_jni_engine.cpp. */
        private const val FIELD_COUNT = 7

        private val loadError: String? = try {
            System.loadLibrary("howl_engine")
            null
        } catch (t: Throwable) {
            t.message ?: t.javaClass.simpleName
        }

        fun libraryError(): String? = loadError

        fun version(): String {
            loadError?.let { throw IllegalStateException(it) }
            return nativeVersion()
        }

        fun create(): HowlEngine {
            loadError?.let { throw IllegalStateException("Native load failed: $it") }
            val handle = nativeCreate()
            require(handle != 0L) { "howl_engine_create returned null" }
            return HowlEngine(handle)
        }

        @JvmStatic private external fun nativeCreate(): Long
        @JvmStatic private external fun nativeDestroy(handle: Long)
        @JvmStatic private external fun nativePrepare(handle: Long): Int
        @JvmStatic private external fun nativeStart(handle: Long): Int
        @JvmStatic private external fun nativeStop(handle: Long): Int
        @JvmStatic private external fun nativeState(handle: Long): Int
        @JvmStatic private external fun nativeGetDiagnostics(handle: Long, out: LongArray): Int
        @JvmStatic private external fun nativeVersion(): String
        @JvmStatic private external fun nativeStatusString(status: Int): String
        @JvmStatic private external fun nativeStateString(state: Int): String
    }
}
