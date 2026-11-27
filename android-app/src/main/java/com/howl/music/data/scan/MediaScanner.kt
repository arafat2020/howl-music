package com.howl.music.data.scan

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.os.CancellationSignal
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

internal data class SourceCredit(val key: String, val name: String)

internal data class ScannedTrack(
    val uri: String,
    val title: String,
    val artist: SourceCredit? = null,
    val album: SourceCredit? = null,
    val durationMs: Long? = null,
    val discNumber: Int? = null,
    val trackNumber: Int? = null,
)

internal class MediaScanner(private val context: Context) {
    fun tracks() = flow {
        coroutineScope {
            val signal = CancellationSignal()
            val cancelQuery = launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    awaitCancellation()
                } finally {
                    signal.cancel()
                }
            }
            try {
                val volumes = if (Build.VERSION.SDK_INT >= 29) {
                    MediaStore.getExternalVolumeNames(context).sorted()
                } else {
                    listOf("external")
                }
                for (volume in volumes) {
                    val collection = Media.getContentUri(volume)
                    val cursor = checkNotNull(context.contentResolver.query(
                        collection,
                        projection,
                        "${Media.IS_MUSIC} != 0 AND ${Media.MIME_TYPE} LIKE ?",
                        arrayOf("audio/%"),
                        "${Media._ID} ASC",
                        signal,
                    )) { "MediaStore could not open $volume" }
                    cursor.use {
                        while (true) {
                            ensureActive()
                            if (!it.moveToNext()) break
                            val id = it.getLong(0)
                            val number = it.positiveLong(8)?.toInt()
                            emit(ScannedTrack(
                                uri = ContentUris.withAppendedId(collection, id).toString(),
                                title = it.text(1) ?: it.text(2) ?: "Track $id",
                                artist = it.credit(3, 4, "$volume:artist"),
                                album = it.credit(5, 6, "$volume:album"),
                                durationMs = it.positiveLong(7),
                                discNumber = number?.div(1000)?.takeIf { disc -> disc > 0 },
                                trackNumber = number?.rem(1000)?.takeIf { track -> track > 0 },
                            ))
                        }
                    }
                }
            } finally {
                cancelQuery.cancel()
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun Cursor.text(index: Int): String? =
        getString(index)?.trim()?.takeUnless { it.isEmpty() || it == MediaStore.UNKNOWN_STRING }

    private fun Cursor.positiveLong(index: Int): Long? = getLong(index).takeIf { it > 0 }

    private fun Cursor.credit(idIndex: Int, nameIndex: Int, prefix: String): SourceCredit? {
        val id = positiveLong(idIndex) ?: return null
        val name = text(nameIndex) ?: return null
        return SourceCredit("mediastore:$prefix:$id", name)
    }

    private companion object {
        val projection = arrayOf(
            Media._ID, Media.TITLE, Media.DISPLAY_NAME, Media.ARTIST_ID, Media.ARTIST,
            Media.ALBUM_ID, Media.ALBUM, Media.DURATION, Media.TRACK,
        )
    }
}
