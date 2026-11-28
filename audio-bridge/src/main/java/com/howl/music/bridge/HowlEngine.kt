package com.howl.music.bridge

import java.io.Closeable

/**
 * Owns one native playback engine. Close explicitly, or scope the instance with [use].
 *
 * Serialize every instance call, including snapshot reads and [close], on one control
 * thread. These calls are synchronous. File, queue and stream operations may block, so
 * use a dedicated control thread for production playback. Keep rendering and audio
 * callbacks out of this API.
 *
 * Commands return 0 on success and a native status code otherwise. [statusName] converts
 * a status to a readable name. Native linkage failures and calls after close throw.
 */
class HowlEngine private constructor(
    private var handle: Long,
) : Closeable {

    private val analysisBuffer = FloatArray(2)

    enum class PlayMode(val nativeCode: Int) {
        Sine(0),
        File(1),
    }

    enum class RepeatMode(val nativeCode: Int) {
        None(0),
        One(1),
        All(2),
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

    fun load(path: String): Int = nativeLoad(requireHandle(), path.encodeToByteArray())

    fun enqueue(path: String): Int = nativeEnqueue(requireHandle(), path.encodeToByteArray())

    fun clearQueue(): Int = nativeClearQueue(requireHandle())

    fun playAt(index: Int): Int = nativePlayAt(requireHandle(), index)

    fun next(): Int = nativeNext(requireHandle())

    fun previous(): Int = nativePrevious(requireHandle())

    /** Set file playback preamp in dB. Finite values clamp to -24..+12. */
    fun setPreampGainDb(gainDb: Float): Int = nativeSetPreampGainDb(requireHandle(), gainDb)

    /** Set one file playback EQ band (0 = 31.25 Hz, 9 = 16 kHz). Gains clamp to -12..+12 dB. */
    fun setEqBandGainDb(band: Int, gainDb: Float): Int =
        nativeSetEqBandGainDb(requireHandle(), band, gainDb)

    /** Apply preamp and all ten EQ bands in one update. */
    fun applyEqState(state: EqState): Int =
        nativeApplyEqState(requireHandle(), state.preampDb, state.bandsDb)

    /** Smoothly return the preamp and all EQ bands to 0 dB. */
    fun resetEqToFlat(): Int = nativeResetEqToFlat(requireHandle())

    fun setRepeatMode(mode: RepeatMode): Int =
        nativeSetRepeatMode(requireHandle(), mode.nativeCode)

    fun queueSize(): Int = nativeQueueSize(requireHandle())

    fun currentIndex(): Int = nativeCurrentIndex(requireHandle())

    fun repeatMode(): RepeatMode =
        when (nativeRepeatMode(requireHandle())) {
            RepeatMode.One.nativeCode -> RepeatMode.One
            RepeatMode.All.nativeCode -> RepeatMode.All
            else -> RepeatMode.None
        }

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

    /** Opt into block-level peak/RMS on this engine. Disabled by default. */
    fun setAnalysisEnabled(enabled: Boolean): Int =
        nativeSetAnalysisEnabled(requireHandle(), enabled)

    /** Read on the engine control thread, for example from a foreground UI polling loop. */
    fun analysis(): AnalysisSnapshot =
        if (nativeGetAnalysis(requireHandle(), analysisBuffer) == 0) {
            AnalysisSnapshot(analysisBuffer[0], analysisBuffer[1])
        } else {
            AnalysisSnapshot.Silent
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

    @Deprecated("Use the lifecycle commands directly.")
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

    /** Stops playback and releases the handle. Safe to repeat on the control thread. */
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
        } catch (t: LinkageError) {
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
        @JvmStatic private external fun nativeLoad(handle: Long, path: ByteArray): Int
        @JvmStatic private external fun nativeEnqueue(handle: Long, path: ByteArray): Int
        @JvmStatic private external fun nativeClearQueue(handle: Long): Int
        @JvmStatic private external fun nativePlayAt(handle: Long, index: Int): Int
        @JvmStatic private external fun nativeNext(handle: Long): Int
        @JvmStatic private external fun nativePrevious(handle: Long): Int
        @JvmStatic private external fun nativeSetPreampGainDb(handle: Long, gainDb: Float): Int
        @JvmStatic private external fun nativeSetEqBandGainDb(handle: Long, band: Int, gainDb: Float): Int
        @JvmStatic private external fun nativeApplyEqState(
            handle: Long,
            preampDb: Float,
            bandsDb: FloatArray,
        ): Int
        @JvmStatic private external fun nativeResetEqToFlat(handle: Long): Int
        @JvmStatic private external fun nativeSetRepeatMode(handle: Long, mode: Int): Int
        @JvmStatic private external fun nativeQueueSize(handle: Long): Int
        @JvmStatic private external fun nativeCurrentIndex(handle: Long): Int
        @JvmStatic private external fun nativeRepeatMode(handle: Long): Int
        @JvmStatic private external fun nativePlayMode(handle: Long): Int
        @JvmStatic private external fun nativeGetTransport(handle: Long, out: LongArray): Int
        @JvmStatic private external fun nativeSetAnalysisEnabled(handle: Long, enabled: Boolean): Int
        @JvmStatic private external fun nativeGetAnalysis(handle: Long, out: FloatArray): Int
        @JvmStatic private external fun nativeGetDiagnostics(handle: Long, out: LongArray): Int
        @JvmStatic private external fun nativeVersion(): String
        @JvmStatic private external fun nativeStatusString(status: Int): String
        @JvmStatic private external fun nativeStateString(state: Int): String
    }
}
