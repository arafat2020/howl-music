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
import kotlinx.coroutines.delay

/**
 * Phase 1.3: 440 Hz tone + polled engine diagnostics (~2 Hz).
 */
class MainActivity : ComponentActivity() {

    private var engine: HowlEngine? = null

    private var status by mutableStateOf("Loading…")
    private var running by mutableStateOf(false)
    private var diagText by mutableStateOf(EngineDiagnostics.Empty.formatLines())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        status = HowlEngine.libraryError() ?: try {
            engine = HowlEngine.create()
            HowlEngine.version()
        } catch (t: Throwable) {
            "Create failed: ${t.message}"
        }

        setContent {
            // Poll snapshots on the UI/control thread — never from the audio callback.
            LaunchedEffect(running) {
                while (true) {
                    val eng = engine
                    if (eng != null && !eng.isClosed) {
                        diagText = eng.diagnostics().formatLines()
                    }
                    if (!running) break
                    delay(500)
                }
            }

            ToneScreen(
                status = status,
                diagnostics = diagText,
                running = running,
                canControl = engine != null,
                onStart = ::onStartTone,
                onStop = ::onStopTone,
            )
        }
    }

    override fun onDestroy() {
        // Stop tone only when the activity is finishing — not on every onStop
        // (volume UI / focus changes were killing the stream mid-test).
        onStopTone()
        engine?.close()
        engine = null
        super.onDestroy()
    }

    private fun onStartTone() {
        val eng = engine ?: return
        val code = eng.start()
        running = code == 0
        status = "start=${eng.statusName(code)} state=${eng.stateName()}"
        diagText = eng.diagnostics().formatLines()
    }

    private fun onStopTone() {
        val eng = engine ?: return
        val code = eng.stop()
        running = false
        status = "stop=${eng.statusName(code)} state=${eng.stateName()}"
        diagText = eng.diagnostics().formatLines()
    }
}

@Composable
private fun ToneScreen(
    status: String,
    diagnostics: String,
    running: Boolean,
    canControl: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
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
            text = "440 Hz + diagnostics",
            color = muted,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row {
            Button(
                onClick = onStart,
                enabled = canControl && !running,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = background,
                ),
            ) {
                Text("Start")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = onStop,
                enabled = canControl && running,
                colors = ButtonDefaults.buttonColors(
                    containerColor = muted,
                    contentColor = background,
                ),
            ) {
                Text("Stop")
            }
        }
        Text(
            text = status,
            color = muted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            text = diagnostics.trimEnd(),
            color = accent.copy(alpha = 0.85f),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}
