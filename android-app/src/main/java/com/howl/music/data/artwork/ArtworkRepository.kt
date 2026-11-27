package com.howl.music.data.artwork

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.provider.MediaStore
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.Fetcher
import coil.key.Keyer
import coil.memory.MemoryCache
import coil.request.ImageRequest
import coil.request.Options
import coil.size.Precision
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

internal object ArtworkRepository {
    private data class Cover(val uri: Uri, val pixels: Int, val revision: String)

    private val permits = Semaphore(2)
    @Volatile private var loader: ImageLoader? = null

    fun imageLoader(context: Context): ImageLoader = loader ?: synchronized(this) {
        loader ?: createLoader(context.applicationContext).also { loader = it }
    }

    suspend fun request(context: Context, trackUri: String?, targetPixels: Int): ImageRequest? =
        withContext(Dispatchers.IO) {
            val uri = trackUri?.let(Uri::parse) ?: return@withContext null
            val revision = try {
                revision(context.contentResolver, uri)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: RuntimeException) {
                null
            } ?: return@withContext null
            val pixels = when {
                targetPixels <= 128 -> 128
                targetPixels <= 256 -> 256
                targetPixels <= 512 -> 512
                else -> 1024
            }
            ImageRequest.Builder(context)
                .data(Cover(uri, pixels, revision))
                .size(pixels)
                .precision(Precision.INEXACT)
                .build()
        }

    private fun revision(resolver: ContentResolver, uri: Uri): String? =
        when {
            uri.scheme == ContentResolver.SCHEME_CONTENT && uri.authority == MediaStore.AUTHORITY -> {
                val columns = arrayOf(MediaStore.MediaColumns.DATE_MODIFIED, MediaStore.MediaColumns.SIZE)
                resolver.query(uri, columns, null, null, null)?.use {
                    if (it.moveToFirst()) "${it.getLong(0)}:${it.getLong(1)}" else null
                }
            }
            uri.scheme == ContentResolver.SCHEME_FILE -> uri.path?.let(::File)?.takeIf(File::isFile)?.let {
                "${it.lastModified()}:${it.length()}"
            }
            else -> null
        }

    private fun createLoader(context: Context): ImageLoader = ImageLoader.Builder(context)
        .memoryCache { MemoryCache.Builder(context).maxSizePercent(0.15).build() }
        .diskCache(null)
        .components {
            add(object : Keyer<Cover> {
                override fun key(data: Cover, options: Options): String =
                    "artwork:${data.uri}|${data.revision}|${data.pixels}"
            })
            add(object : Fetcher.Factory<Cover> {
                override fun create(data: Cover, options: Options, imageLoader: ImageLoader): Fetcher =
                    Fetcher {
                        val bitmap = load(context, data)
                            ?: throw IOException("Artwork unavailable")
                        // The model represents a final size bucket, including legitimately small covers.
                        DrawableResult(BitmapDrawable(context.resources, bitmap), false, DataSource.DISK)
                    }
            })
        }
        .build()

    private suspend fun load(context: Context, cover: Cover): Bitmap? = withContext(Dispatchers.IO) {
        permits.withPermit {
            val thumbnail = if (Build.VERSION.SDK_INT >= 29 && cover.uri.scheme == ContentResolver.SCHEME_CONTENT) {
                loadThumbnail(context.contentResolver, cover)
            } else {
                null
            }
            ensureActive()
            thumbnail ?: loadEmbedded(context, cover)
        }
    }

    @androidx.annotation.RequiresApi(29)
    private suspend fun loadThumbnail(resolver: ContentResolver, cover: Cover): Bitmap? = coroutineScope {
        val signal = CancellationSignal()
        val cancellation = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                awaitCancellation()
            } finally {
                signal.cancel()
            }
        }
        try {
            resolver.loadThumbnail(cover.uri, Size(cover.pixels, cover.pixels), signal)
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        } finally {
            cancellation.cancel()
        }
    }

    private fun loadEmbedded(context: Context, cover: Cover): Bitmap? {
        val retriever = MediaMetadataRetriever()
        val bytes = try {
            retriever.setDataSource(context, cover.uri)
            retriever.embeddedPicture
        } catch (_: IllegalArgumentException) {
            null
        } finally {
            retriever.release()
        } ?: return null
        if (bytes.isEmpty() || bytes.size > 16 * 1024 * 1024) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val largestSide = maxOf(bounds.outWidth, bounds.outHeight)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (largestSide / sample > cover.pixels) sample *= 2
        return BitmapFactory.decodeByteArray(
            bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample },
        )
    }
}
