package com.howl.music.bridge

import java.io.Closeable

/**
 * Control-thread façade over the native Engine (Phase 2.3).
 *
 * Call transport APIs from one control thread only — never from an audio callback.
 */
class HowlEngine private constructor(
    private var handle: Long,
) : Closeable {

    enum class PlayMode(val nativeCode: Int) {
        Sine(0),
        File(1),
    }

    val isClosed: Boolean
        get() = handle == 0L

    fun prepare(): Int = nativePrepare(requireHandle())

    fun start(): Int = nativeStart(requireHandle())

    fun stop(): Int = nativeStop(requireHandle())

    fun pause(): Int = nativePause(requireHandle())

    fun resume(): Int = nativeResume(requireHandle())

    fun seekMs(positionMs: Long): Int = nativeSeekMs(requireHandle(), positionMs)

    fun state(): Int = nativeState(requireHandle())

    fun stateName(): String = nativeStateString(state())

    fun statusName(code: Int): String = nativeStatusString(code)

    fun setPlayMode(mode: PlayMode): Int =
        nativeSetPlayMode(requireHandle(), mode.nativeCode)

    fun load(path: String): Int = nativeLoad(requireHandle(), path)

    fun playMode(): PlayMode =
        if (nativePlayMode(requireHandle()) == PlayMode.File.nativeCode) {
            PlayMode.File
        } else {
            PlayMode.Sine
        }

    fun transport(): TransportSnapshot {
        val buf = LongArray(TRANSPORT_FIELDS)
        val code = nativeGetTransport(requireHandle(), buf)
        if (code != 0) {
            return TransportSnapshot.Empty
        }
        return TransportSnapshot(
            positionFrames = buf[0],
            durationFrames = buf[1],
            sampleRate = buf[2].toInt(),
            paused = buf[3] != 0L,
            playing = buf[4] != 0L,
        )
    }

    fun diagnostics(): EngineDiagnostics {
        val buf = LongArray(DIAG_FIELDS)
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
        private const val DIAG_FIELDS = 7
        private const val TRANSPORT_FIELDS = 5

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
        @JvmStatic private external fun nativePause(handle: Long): Int
        @JvmStatic private external fun nativeResume(handle: Long): Int
        @JvmStatic private external fun nativeSeekMs(handle: Long, positionMs: Long): Int
        @JvmStatic private external fun nativeState(handle: Long): Int
        @JvmStatic private external fun nativeSetPlayMode(handle: Long, mode: Int): Int
        @JvmStatic private external fun nativeLoad(handle: Long, path: String): Int
        @JvmStatic private external fun nativePlayMode(handle: Long): Int
        @JvmStatic private external fun nativeGetTransport(handle: Long, out: LongArray): Int
        @JvmStatic private external fun nativeGetDiagnostics(handle: Long, out: LongArray): Int
        @JvmStatic private external fun nativeVersion(): String
        @JvmStatic private external fun nativeStatusString(status: Int): String
        @JvmStatic private external fun nativeStateString(state: Int): String
    }
}
