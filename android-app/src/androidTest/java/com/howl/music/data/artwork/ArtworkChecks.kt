package com.howl.music.data.artwork

import android.content.ContentValues
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import coil.decode.DataSource
import coil.request.ErrorResult
import coil.request.SuccessResult
import com.howl.music.bridge.HowlEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File

internal fun verifyArtwork(context: Context, testContext: Context, noCover: File) = runBlocking {
    val covered = File.createTempFile("howl-cover-", ".mp3", context.cacheDir)
    val loader = ArtworkRepository.imageLoader(context)
    val engine = HowlEngine.create()
    try {
        check(engine.setPlayMode(HowlEngine.PlayMode.File) == 0)
        check(engine.load(noCover.absolutePath) == 0)
        check(engine.setRepeatMode(HowlEngine.RepeatMode.One) == 0)
        check(engine.start() == 0)
        val before = engine.diagnostics().callbackCount
        testContext.assets.open("artwork-cover.mp3").use { input ->
            covered.outputStream().use { input.copyTo(it) }
        }
        val localUri = Uri.fromFile(covered).toString()
        val small = checkNotNull(ArtworkRepository.request(context, localUri, 112))
        val first = loader.execute(small) as SuccessResult
        val thumbnail = (first.drawable as BitmapDrawable).bitmap
        check(thumbnail.width <= 128 && thumbnail.height <= 128)

        val revisited = checkNotNull(ArtworkRepository.request(context, localUri, 112))
        val hit = loader.execute(revisited) as SuccessResult
        check(hit.dataSource == DataSource.MEMORY_CACHE)
        check(hit.memoryCacheKey == first.memoryCacheKey)

        val hero = loader.execute(checkNotNull(ArtworkRepository.request(context, localUri, 640))) as SuccessResult
        check((hero.drawable as BitmapDrawable).bitmap.width > thumbnail.width)
        check(hero.memoryCacheKey != first.memoryCacheKey)

        check(covered.setLastModified(covered.lastModified() + 5_000))
        val changed = loader.execute(checkNotNull(ArtworkRepository.request(context, localUri, 112))) as SuccessResult
        check(changed.memoryCacheKey != first.memoryCacheKey)
        check(changed.dataSource != DataSource.MEMORY_CACHE)

        val missing = checkNotNull(ArtworkRepository.request(context, Uri.fromFile(noCover).toString(), 112))
        check(loader.execute(missing) is ErrorResult)
        check(ArtworkRepository.request(context, "https://example.invalid/cover.png", 112) == null)
        if (Build.VERSION.SDK_INT >= 29) verifyMediaStoreArtwork(context, covered)
        delay(80)
        check(engine.transport().playing)
        val diagnostics = engine.diagnostics()
        check(diagnostics.callbackCount > before && diagnostics.errorCount == 0L)
    } finally {
        engine.close()
        covered.delete()
    }
}

@androidx.annotation.RequiresApi(29)
private suspend fun verifyMediaStoreArtwork(context: Context, fixture: File) {
    val resolver = context.contentResolver
    val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    val params = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fixture.name)
        put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg")
        put(MediaStore.MediaColumns.RELATIVE_PATH, "Music/HowlArtworkTest/")
        put(MediaStore.MediaColumns.IS_PENDING, 1)
    }
    val uri = checkNotNull(resolver.insert(collection, params))
    try {
        checkNotNull(resolver.openOutputStream(uri)).use { output ->
            fixture.inputStream().use { it.copyTo(output) }
        }
        resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        val result = ArtworkRepository.imageLoader(context).execute(
            checkNotNull(ArtworkRepository.request(context, uri.toString(), 112)),
        ) as SuccessResult
        check((result.drawable as BitmapDrawable).bitmap.width <= 128)
    } finally {
        resolver.delete(uri, null, null)
    }
}
