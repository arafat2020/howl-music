package com.howl.music.ui.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.howl.music.data.db.HowlDatabase
import com.howl.music.data.db.entities.TrackEntity
import com.howl.music.ui.components.AlbumArt
import com.howl.music.ui.theme.HowlTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ArtworkPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HowlTheme {
                var tracks by remember { mutableStateOf<List<TrackEntity>>(emptyList()) }
                var selected by remember { mutableStateOf<TrackEntity?>(null) }
                var message by remember { mutableStateOf("Loading tracks…") }

                LaunchedEffect(Unit) {
                    try {
                        tracks = withContext(Dispatchers.IO) {
                            val db = HowlDatabase.open(applicationContext)
                            try {
                                db.libraryDao().tracks()
                            } finally {
                                db.close()
                            }
                        }
                        message = if (tracks.isEmpty()) "Scan music to preview artwork." else ""
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        message = "Could not load the music library."
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Artwork preview")
                    if (message.isNotEmpty()) Text(message)
                    val track = selected
                    if (track != null) {
                        AlbumArt(track.uri, 224.dp, contentDescription = "Artwork for ${track.title}")
                        Text(track.title)
                        Button(onClick = { selected = null }) { Text("Back to tracks") }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(tracks, key = { it.id }) { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { selected = row },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    AlbumArt(row.uri, 56.dp)
                                    Text(row.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
