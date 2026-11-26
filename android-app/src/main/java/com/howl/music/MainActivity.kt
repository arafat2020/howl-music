package com.howl.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.howl.music.bridge.HowlEngine
import com.howl.music.media.MediaModulePlaceholder

/**
 * Phase 1.1 host: shows Engine lifecycle smoke (no audio device yet).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HowlPlaceholder(
                nativeStatus = remember { runLifecycleSmoke() },
                mediaStatus = MediaModulePlaceholder.STATUS,
            )
        }
    }
}

private fun runLifecycleSmoke(): String {
    HowlEngine.libraryError()?.let { return "Native load failed:\n$it" }
    return try {
        HowlEngine.create().use { engine ->
            engine.lifecycleSmokeReport()
        }
    } catch (t: Throwable) {
        "Lifecycle smoke failed:\n${t.message ?: t.javaClass.simpleName}"
    }
}

@Composable
private fun HowlPlaceholder(
    nativeStatus: String,
    mediaStatus: String,
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
            text = nativeStatus,
            color = muted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            text = mediaStatus,
            color = muted.copy(alpha = 0.7f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HowlPlaceholderPreview() {
    val status = remember {
        "howl_engine/0.1.0-lifecycle\nstart#1=Ok\nstart#2=AlreadyRunning"
    }
    HowlPlaceholder(
        nativeStatus = status,
        mediaStatus = MediaModulePlaceholder.STATUS,
    )
}
