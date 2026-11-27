package com.howl.music.data.scan

import android.content.Context
import androidx.room.Room
import com.howl.music.data.db.HowlDatabase
import com.howl.music.data.db.entities.PlaylistEntity
import com.howl.music.data.db.entities.PlaylistTrackEntity
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

internal fun verifyLibraryScan(context: Context) = runBlocking {
    val db = Room.inMemoryDatabaseBuilder(context, HowlDatabase::class.java).build()
    try {
        val repo = LibraryScanRepository(db)
        val dao = db.libraryDao()

        // Rows buffered after the first commit must be discarded on cancellation.
        repo.scan(flow {
            repeat(130) { emit(track(it)) }
            awaitCancellation()
        }).take(2).toList().also {
            check(it.last() == ScanProgress.Indexing(128))
        }
        check(dao.trackCount() == 128)

        val start = System.nanoTime()
        val count = 2_500
        val complete = repo.scan(flow { repeat(count) { emit(track(it)) } }).last()
        check(complete == ScanProgress.Complete(count, count))
        android.util.Log.i("HowlScanTest", "Indexed $count synthetic tracks in ${(System.nanoTime() - start) / 1_000_000} ms")

        val first = checkNotNull(dao.trackByUri(track(0).uri))
        check(first.artistId != null && first.albumId != null)
        val playlist = db.playlistDao().insertPlaylist(PlaylistEntity("Scan reference"))
        db.playlistDao().insertEntry(PlaylistTrackEntity(playlist, first.id, position = 0))
        val updated = track(0).copy(
            title = "Edited track",
            artist = SourceCredit("primary:artist:1", "Edited artist"),
            album = SourceCredit("primary:album:1", "Edited album"),
        )
        check(repo.scan(flow { emit(updated); emit(updated) }).last() == ScanProgress.Complete(2, count))
        val same = checkNotNull(dao.trackByUri(updated.uri))
        check(same.id == first.id && same.title == updated.title)
        check(dao.artistBySourceKey("primary:artist:1")?.name == "Edited artist")
        check(dao.albumBySourceKey("primary:album:1")?.title == "Edited album")
        check(db.playlistDao().tracks(playlist).single().id == first.id)
    } finally {
        db.close()
    }
}

private fun track(index: Int) = ScannedTrack(
    uri = "content://media/external_primary/audio/media/${index + 1}",
    title = "Track ${index + 1}",
    artist = SourceCredit("primary:artist:1", "Test artist"),
    album = SourceCredit("primary:album:1", "Test album"),
)
