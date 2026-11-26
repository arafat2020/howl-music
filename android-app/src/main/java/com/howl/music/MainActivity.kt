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
 * Phase 2.3: sine/WAV playback with pause / seek / position.
 */
class MainActivity : ComponentActivity() {

    private var engine: HowlEngine? = null

    private var status by mutableStateOf("Loading…")
    private var running by mutableStateOf(false)
    private var paused by mutableStateOf(false)
    private var fileMode by mutableStateOf(true)
    private var diagText by mutableStateOf(EngineDiagnostics.Empty.formatLines())
    private var transportText by mutableStateOf(TransportSnapshot.Empty.formatLine())
    private var wavPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        status = HowlEngine.libraryError() ?: try {
            engine = HowlEngine.create()
            wavPath = copyAssetWav()
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
                    }
                    if (!running) break
                    delay(250)
                }
            }

            PlayerScreen(
                status = status,
                transport = transportText,
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
            )
        }
    }

    override fun onDestroy() {
        onStopPlayback()
        engine?.close()
        engine = null
        super.onDestroy()
    }

    private fun copyAssetWav(): String {
        val out = File(cacheDir, "sine_440_16bit_stereo.wav")
        assets.open("sine_440_16bit_stereo.wav").use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        }
        return out.absolutePath
    }

    private fun onToggleMode() {
        if (running) return
        fileMode = !fileMode
        status = if (fileMode) "mode=File (WAV ring)" else "mode=Sine"
    }

    private fun onStartPlayback() {
        val eng = engine ?: return
        if (fileMode) {
            val path = wavPath ?: run {
                status = "WAV asset missing"
                return
            }
            val modeCode = eng.setPlayMode(HowlEngine.PlayMode.File)
            if (modeCode != 0) {
                status = "setPlayMode=${eng.statusName(modeCode)}"
                return
            }
            val loadCode = eng.load(path)
            if (loadCode != 0) {
                status = "load=${eng.statusName(loadCode)}"
                return
            }
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
        diagText = eng.diagnostics().formatLines()
    }

    private fun onStopPlayback() {
        val eng = engine ?: return
        val code = eng.stop()
        running = false
        paused = false
        status = "stop=${eng.statusName(code)} state=${eng.stateName()}"
        transportText = eng.transport().formatLine()
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
}

@Composable
private fun PlayerScreen(
    status: String,
    transport: String,
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
            text = if (fileMode) "WAV transport" else "440 Hz sine",
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
            Text(if (fileMode) "Switch to Sine" else "Switch to WAV")
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
            text = transport,
            color = accent,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp),
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
