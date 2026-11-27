package com.howl.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.howl.music.data.artwork.ArtworkRepository

@Composable
fun AlbumArt(
    trackUri: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val context = LocalContext.current.applicationContext
    val pixels = with(LocalDensity.current) { size.roundToPx() }
    val loader = remember(context) { ArtworkRepository.imageLoader(context) }
    key(trackUri, pixels) {
        val request by produceState<ImageRequest?>(null, context) {
            value = ArtworkRepository.request(context, trackUri, pixels)
        }
        Box(
            modifier = modifier.size(size).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text("♪", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (request != null) {
                AsyncImage(
                    model = request,
                    imageLoader = loader,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
    }
}
