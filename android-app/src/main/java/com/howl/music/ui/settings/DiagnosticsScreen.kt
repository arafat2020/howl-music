package com.howl.music.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.howl.music.media.PlaybackOutputDiagnostics
import com.howl.music.ui.components.HowlButton
import com.howl.music.ui.theme.HowlSpacing
import kotlinx.coroutines.delay
import java.text.NumberFormat

@Composable
internal fun DiagnosticsScreen(
    mediaId: String?,
    readDiagnostics: suspend () -> PlaybackOutputDiagnostics?,
    onBack: () -> Unit,
) {
    val owner = LocalLifecycleOwner.current
    val read by rememberUpdatedState(readDiagnostics)
    val output by produceState<PlaybackOutputDiagnostics?>(null, owner, mediaId) {
        value = null
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                value = read()
                delay(1_000)
            }
        }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(HowlSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(HowlSpacing.Medium),
    ) {
        HowlButton("Back", onBack, primary = false)
        Text("Output diagnostics", style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics { heading() })
        val snapshot = output
        if (snapshot == null) {
            Text("Waiting for playback diagnostics")
        } else {
            OutputFields(snapshot)
        }
    }
}

@Composable
private fun OutputFields(output: PlaybackOutputDiagnostics) {
    val open = output.streamRate > 0
    val numbers = NumberFormat.getIntegerInstance()
    fun rate(value: Int) = if (value > 0) "${numbers.format(value)} Hz" else "Unavailable"
    fun channels(value: Int) = if (value > 0) numbers.format(value) else "Unavailable"
    val source = output.sourceCodec?.let {
        val encoding = if (output.sourceFloat) "float" else "integer"
        "$it · ${output.sourceBits}-bit $encoding"
    } ?: "No source loaded"

    Text(
        when {
            !open -> "No active output stream"
            output.paused -> "Output paused"
            else -> "Output active"
        },
        style = MaterialTheme.typography.titleMedium,
    )
    Field("Source format", source)
    Field("Source sample rate", rate(output.sourceRate), "The decoded file’s sample rate.")
    Field("Source channels", channels(output.sourceChannels))
    Field("App stream rate", rate(output.streamRate), "The rate used by Howl’s processing and output callback.")
    Field("Stream channels", channels(output.streamChannels))
    Field("Hardware-reported rate", rate(output.hardwareRate),
        "Android may mix or resample audio beyond the app stream.")
    Field("Output route (best effort)", output.route ?: "Unavailable",
        "The device label Android associates with this stream.")
    Field("Equalizer", if (output.eqEnabled) "Enabled" else "Bypassed")
    Field("Gapless handoff", when (output.gapless) {
        "Primed" -> "Next track ready"
        "Joined" -> "Joined in the current stream"
        "Fallback" -> "Standard transition"
        else -> "Idle"
    })
    Field("Ring underruns", if (open) numbers.format(output.underruns) else "Unavailable",
        "Callbacks that needed more decoded audio, including at stream end.")
    Field("Platform xruns", if (open && output.platformXruns >= 0) numbers.format(output.platformXruns) else "Unavailable",
        "Reported by Android for this stream when supported.")
    Field("Stream errors", if (open) numbers.format(output.errors) else "Unavailable")
}

@Composable
private fun Field(label: String, value: String, detail: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(HowlSpacing.ExtraSmall)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Text(value, style = MaterialTheme.typography.bodyLarge)
        if (detail != null) {
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
