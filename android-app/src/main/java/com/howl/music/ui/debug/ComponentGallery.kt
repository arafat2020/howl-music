package com.howl.music.ui.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.howl.music.data.db.HowlDatabase
import com.howl.music.data.prefs.PreferencesRepository
import com.howl.music.ui.components.HowlButton
import com.howl.music.ui.components.HowlIconButton
import com.howl.music.ui.components.HowlListRow
import com.howl.music.ui.components.HowlSlider
import com.howl.music.ui.theme.HowlPalette
import com.howl.music.ui.theme.HowlSpacing
import com.howl.music.ui.theme.HowlTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class ComponentGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var dark by rememberSaveable { mutableStateOf(true) }
            var storedMotion by rememberSaveable { mutableStateOf(false) }
            var previewMotion by rememberSaveable { mutableStateOf<Boolean?>(null) }

            SideEffect {
                val transparent = android.graphics.Color.TRANSPARENT
                val bars = if (dark) SystemBarStyle.dark(transparent)
                else SystemBarStyle.light(transparent, transparent)
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }

            LaunchedEffect(Unit) {
                try {
                    storedMotion = withContext(Dispatchers.IO) {
                        val db = HowlDatabase.open(applicationContext)
                        try {
                            PreferencesRepository(db).get().reducedMotion
                        } finally {
                            db.close()
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // The gallery can still preview the default motion policy.
                }
            }

            HowlTheme(dark = dark, reduceMotion = previewMotion ?: storedMotion) {
                ComponentGallery(
                    dark = dark,
                    onDarkChange = { dark = it },
                    reduceMotion = previewMotion ?: storedMotion,
                    onReduceMotionChange = { previewMotion = it },
                )
            }
        }
    }
}

@Composable
private fun ComponentGallery(
    dark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    reduceMotion: Boolean,
    onReduceMotionChange: (Boolean) -> Unit,
) {
    val type = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme
    var playing by rememberSaveable { mutableStateOf(false) }
    var progress by rememberSaveable { mutableStateOf(0.35f) }
    var selected by rememberSaveable { mutableStateOf(0) }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 320.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(HowlSpacing.Screen),
        horizontalArrangement = Arrangement.spacedBy(HowlSpacing.Large),
        verticalArrangement = Arrangement.spacedBy(HowlSpacing.Large),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(HowlSpacing.Small)) {
                Text("Component gallery", style = type.displayMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(HowlSpacing.Large),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Dark theme", style = type.titleSmall, modifier = Modifier.weight(1f))
                    Switch(checked = dark, onCheckedChange = onDarkChange,
                        modifier = Modifier.semantics { contentDescription = "Dark theme" })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(HowlSpacing.Large),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Reduce motion", style = type.titleSmall, modifier = Modifier.weight(1f))
                    Switch(checked = reduceMotion, onCheckedChange = onReduceMotionChange,
                        modifier = Modifier.semantics { contentDescription = "Reduce motion" })
                }
            }
        }
        item {
            GallerySection("Palette") {
                Swatch("Canvas", "#101014", HowlPalette.Canvas)
                Swatch("Primary", "#B7F34A", HowlPalette.Lime)
                Swatch("Secondary", "#B65CFF", HowlPalette.Purple)
                Swatch("Warm accent", "#FF6B47", HowlPalette.Orange)
                Swatch("Neutral", "#E9E5DC", HowlPalette.Neutral)
            }
        }
        item {
            GallerySection("Typography") {
                Text("Howl", style = type.displayLarge, color = colors.primary)
                Text("Album title", style = type.headlineSmall)
                Text("Track title", style = type.titleMedium)
                Text("Artist · Album · 3:42", style = type.bodyMedium, color = colors.onSurfaceVariant)
                Text("Metadata label", style = type.labelMedium, color = colors.onSurfaceVariant)
            }
        }
        item {
            GallerySection("Buttons and slider") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(HowlSpacing.Small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HowlButton(if (playing) "Pause" else "Play", onClick = { playing = !playing })
                    HowlButton("Reset", onClick = {
                        playing = false
                        progress = 0f
                    }, primary = false)
                    HowlIconButton(
                        label = if (playing) "Pause preview" else "Play preview",
                        onClick = { playing = !playing },
                    ) { PlayIcon(playing) }
                }
                HowlButton("Unavailable", onClick = {}, enabled = false, primary = false)
                Text("Position · ${(progress * 100).roundToInt()}%", style = type.labelLarge)
                HowlSlider("Preview position", progress, { progress = it })
            }
        }
        item {
            GallerySection("List rows") {
                HowlListRow("Opening track", "Artist · 3:42", { selected = 0 }, selected = selected == 0)
                HowlListRow("Second track", "Artist · 4:11", { selected = 1 }, selected = selected == 1)
            }
        }
    }
}

@Composable
private fun GallerySection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(HowlSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(HowlSpacing.Small),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun Swatch(role: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(HowlSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(24.dp),
            color = color,
            shape = MaterialTheme.shapes.extraSmall,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {}
        Text(role, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun PlayIcon(playing: Boolean) {
    val tint = LocalContentColor.current
    Canvas(Modifier.size(20.dp)) {
        if (playing) {
            val bar = size.width / 4
            drawRect(tint, topLeft = Offset(bar / 2, 0f),
                size = Size(bar, size.height))
            drawRect(tint, topLeft = Offset(bar * 2.5f, 0f),
                size = Size(bar, size.height))
        } else {
            drawPath(Path().apply {
                moveTo(size.width * 0.15f, 0f)
                lineTo(size.width * 0.95f, size.height / 2)
                lineTo(size.width * 0.15f, size.height)
                close()
            }, tint)
        }
    }
}

@Preview(widthDp = 393, heightDp = 850)
@Composable
private fun PhoneGalleryPreview() {
    HowlTheme { ComponentGallery(true, {}, false, {}) }
}

@Preview(widthDp = 800, heightDp = 900)
@Composable
private fun TabletGalleryPreview() {
    HowlTheme(dark = false, reduceMotion = true) { ComponentGallery(false, {}, true, {}) }
}
