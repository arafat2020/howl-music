package com.howl.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.howl.music.bridge.EngineDiagnostics
import com.howl.music.bridge.HowlEngine
import com.howl.music.bridge.TransportSnapshot
import kotlinx.coroutines.delay
import java.io.File

/**
 * Phase 2.5: two-track queue, next/prev, repeat, EOF auto-advance.
 */
class MainActivity : ComponentActivity() {

    private var engine: HowlEngine? = null

    private var status by mutableStateOf("Loading…")
    private var running by mutableStateOf(false)
    private var paused by mutableStateOf(false)
    private var fileMode by mutableStateOf(true)
    private var diagText by mutableStateOf(EngineDiagnostics.Empty.formatLines())
    private var transportText by mutableStateOf(TransportSnapshot.Empty.formatLine())
    private var queueText by mutableStateOf("queue —")
    private var wav440: String? = null
    private var wav880: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        status = HowlEngine.libraryError() ?: try {
            engine = HowlEngine.create()
            wav440 = copyAssetWav("sine_440_16bit_stereo.wav")
            wav880 = copyAssetWav("sine_880_16bit_stereo.wav")
            HowlEngine.version()
        } catch (t: Throwable) {
            "Create failed: ${t.message}"
        }

        setContent {
            LaunchedEffect(running) {
                while (true) {
                    val eng = engine
                    if (eng != null && !eng.isClosed) {
                        diagText = eng.diagnostics().formatLines()
                        val t = eng.transport()
                        transportText = t.formatLine()
                        paused = t.paused
                        running = t.playing
                        queueText = formatQueue(eng)
                    }
                    if (!running) break
                    delay(250)
                }
            }

            PlayerScreen(
                status = status,
                transport = transportText,
                queue = queueText,
                diagnostics = diagText,
                running = running,
                paused = paused,
                fileMode = fileMode,
                canControl = engine != null,
                onToggleMode = ::onToggleMode,
                onStart = ::onStartPlayback,
                onStop = ::onStopPlayback,
                onPauseResume = ::onPauseResume,
                onSeekStart = { seekTo(0) },
                onSeekMid = ::seekToMid,
                onPrev = ::onPrev,
                onNext = ::onNext,
                onCycleRepeat = ::onCycleRepeat,
            )
        }
    }

    override fun onDestroy() {
        onStopPlayback()
        engine?.close()
        engine = null
        super.onDestroy()
    }

    private fun copyAssetWav(name: String): String {
        val out = File(cacheDir, name)
        assets.open(name).use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        }
        return out.absolutePath
    }

    private fun formatQueue(eng: HowlEngine): String {
        val idx = eng.currentIndex()
        val size = eng.queueSize()
        val rep = when (eng.repeatMode()) {
            HowlEngine.RepeatMode.None -> "off"
            HowlEngine.RepeatMode.One -> "one"
            HowlEngine.RepeatMode.All -> "all"
        }
        return "track ${idx + 1}/$size  repeat=$rep"
    }

    private fun onToggleMode() {
        if (running) return
        fileMode = !fileMode
        status = if (fileMode) "mode=File (2-track queue)" else "mode=Sine"
    }

    private fun onStartPlayback() {
        val eng = engine ?: return
        if (fileMode) {
            val a = wav440
            val b = wav880
            if (a == null || b == null) {
                status = "WAV assets missing"
                return
            }
            val modeCode = eng.setPlayMode(HowlEngine.PlayMode.File)
            if (modeCode != 0) {
                status = "setPlayMode=${eng.statusName(modeCode)}"
                return
            }
            eng.clearQueue()
            val e1 = eng.enqueue(a)
            val e2 = eng.enqueue(b)
            if (e1 != 0 || e2 != 0) {
                status = "enqueue e1=${eng.statusName(e1)} e2=${eng.statusName(e2)}"
                return
            }
            eng.setRepeatMode(HowlEngine.RepeatMode.None)
        } else {
            val modeCode = eng.setPlayMode(HowlEngine.PlayMode.Sine)
            if (modeCode != 0) {
                status = "setPlayMode=${eng.statusName(modeCode)}"
                return
            }
        }

        val code = eng.start()
        running = code == 0
        paused = false
        status = "start=${eng.statusName(code)} state=${eng.stateName()}"
        transportText = eng.transport().formatLine()
        queueText = formatQueue(eng)
        diagText = eng.diagnostics().formatLines()
    }

    private fun onStopPlayback() {
        val eng = engine ?: return
        val code = eng.stop()
        running = false
        paused = false
        status = "stop=${eng.statusName(code)} state=${eng.stateName()}"
        transportText = eng.transport().formatLine()
        queueText = formatQueue(eng)
        diagText = eng.diagnostics().formatLines()
    }

    private fun onPauseResume() {
        val eng = engine ?: return
        if (!running) return
        val code = if (paused) eng.resume() else eng.pause()
        status = if (paused) {
            "resume=${eng.statusName(code)}"
        } else {
            "pause=${eng.statusName(code)}"
        }
        val t = eng.transport()
        paused = t.paused
        transportText = t.formatLine()
    }

    private fun seekTo(ms: Long) {
        val eng = engine ?: return
        if (!running || !fileMode) return
        val code = eng.seekMs(ms)
        status = "seekMs=$ms → ${eng.statusName(code)}"
        transportText = eng.transport().formatLine()
    }

    private fun seekToMid() {
        val eng = engine ?: return
        val mid = eng.transport().durationMs / 2
        seekTo(mid)
    }

    private fun onPrev() {
        val eng = engine ?: return
        if (!fileMode) return
        val code = eng.previous()
        status = "prev=${eng.statusName(code)}"
        queueText = formatQueue(eng)
        transportText = eng.transport().formatLine()
        running = eng.transport().playing
    }

    private fun onNext() {
        val eng = engine ?: return
        if (!fileMode) return
        val code = eng.next()
        status = "next=${eng.statusName(code)}"
        queueText = formatQueue(eng)
        transportText = eng.transport().formatLine()
        running = eng.transport().playing
    }

    private fun onCycleRepeat() {
        val eng = engine ?: return
        if (!fileMode) return
        val next = when (eng.repeatMode()) {
            HowlEngine.RepeatMode.None -> HowlEngine.RepeatMode.All
            HowlEngine.RepeatMode.All -> HowlEngine.RepeatMode.One
            HowlEngine.RepeatMode.One -> HowlEngine.RepeatMode.None
        }
        eng.setRepeatMode(next)
        status = "repeat=${next.name.lowercase()}"
        queueText = formatQueue(eng)
    }
}

@Composable
private fun PlayerScreen(
    status: String,
    transport: String,
    queue: String,
    diagnostics: String,
    running: Boolean,
    paused: Boolean,
    fileMode: Boolean,
    canControl: Boolean,
    onToggleMode: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onPauseResume: () -> Unit,
    onSeekStart: () -> Unit,
    onSeekMid: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onCycleRepeat: () -> Unit,
) {
    val background = Color(0xFF101014)
    val accent = Color(0xFFB7F34A)
    val muted = Color(0xFFE9E5DC)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Howl",
            color = accent,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = if (fileMode) "Queue handoff" else "440 Hz sine",
            color = muted,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onToggleMode,
            enabled = canControl && !running,
            colors = ButtonDefaults.buttonColors(
                containerColor = muted.copy(alpha = 0.25f),
                contentColor = muted,
            ),
        ) {
            Text(if (fileMode) "Switch to Sine" else "Switch to WAV queue")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row {
            Button(
                onClick = onStart,
                enabled = canControl && !running,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = background,
                ),
            ) {
                Text("Play")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onPauseResume,
                enabled = canControl && running,
                colors = ButtonDefaults.buttonColors(
                    containerColor = muted,
                    contentColor = background,
                ),
            ) {
                Text(if (paused) "Resume" else "Pause")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onStop,
                enabled = canControl && running,
                colors = ButtonDefaults.buttonColors(
                    containerColor = muted.copy(alpha = 0.7f),
                    contentColor = background,
                ),
            ) {
                Text("Stop")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row {
            Button(
                onClick = onPrev,
                enabled = canControl && fileMode,
                colors = ButtonDefaults.buttonColors(
                    containerColor = muted.copy(alpha = 0.25f),
                    contentColor = muted,
                ),
            ) {
                Text("Prev")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onNext,
                enabled = canControl && fileMode,
                colors = ButtonDefaults.buttonColors(
                    containerColor = muted.copy(alpha = 0.25f),
                    contentColor = muted,
                ),
            ) {
                Text("Next")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onCycleRepeat,
                enabled = canControl && fileMode,
                colors = ButtonDefaults.buttonColors(
                    containerColor = muted.copy(alpha = 0.25f),
                    contentColor = muted,
                ),
            ) {
                Text("Repeat")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row {
            Button(
                onClick = onSeekStart,
                enabled = canControl && running && fileMode,
                colors = ButtonDefaults.buttonColors(
                    containerColor = muted.copy(alpha = 0.25f),
                    contentColor = muted,
                ),
            ) {
                Text("Seek 0")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onSeekMid,
                enabled = canControl && running && fileMode,
                colors = ButtonDefaults.buttonColors(
                    containerColor = muted.copy(alpha = 0.25f),
                    contentColor = muted,
                ),
            ) {
                Text("Seek mid")
            }
        }
        Text(
            text = queue,
            color = accent,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            text = transport,
            color = accent.copy(alpha = 0.9f),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = status,
            color = muted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = diagnostics.trimEnd(),
            color = accent.copy(alpha = 0.75f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
