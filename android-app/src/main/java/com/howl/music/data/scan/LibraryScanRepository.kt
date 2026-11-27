package com.howl.music.data.scan

import androidx.room.withTransaction
import com.howl.music.data.db.HowlDatabase
import com.howl.music.data.db.entities.AlbumEntity
import com.howl.music.data.db.entities.ArtistEntity
import com.howl.music.data.db.entities.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

internal sealed interface ScanProgress {
    data class Indexing(val processed: Int) : ScanProgress
    data class Complete(val processed: Int, val librarySize: Int) : ScanProgress
}

internal class LibraryScanRepository(private val db: HowlDatabase) {
    private val dao = db.libraryDao()

    fun scan(source: Flow<ScannedTrack>): Flow<ScanProgress> = flow {
        var processed = 0
        val batch = ArrayList<ScannedTrack>(BATCH_SIZE)

        suspend fun flush() {
            if (batch.isEmpty()) return
            saveBatch(batch)
            processed += batch.size
            batch.clear()
            emit(ScanProgress.Indexing(processed))
        }

        emit(ScanProgress.Indexing(0))
        source.collect { track ->
            batch += track
            if (batch.size == BATCH_SIZE) flush()
        }
        flush()
        emit(ScanProgress.Complete(processed, dao.trackCount()))
    }.flowOn(Dispatchers.IO)

    private suspend fun saveBatch(rows: List<ScannedTrack>) = db.withTransaction {
        val unique = rows.associateBy(ScannedTrack::uri).values
        val artists = unique.mapNotNull(ScannedTrack::artist).associateBy(SourceCredit::key)
        val albums = unique.mapNotNull(ScannedTrack::album).associateBy(SourceCredit::key)
        val oldArtists = dao.artistsBySourceKeys(artists.keys.toList()).associateBy { it.sourceKey }
        val oldAlbums = dao.albumsBySourceKeys(albums.keys.toList()).associateBy { it.sourceKey }
        dao.upsertArtists(artists.values.map {
            oldArtists[it.key]?.copy(name = it.name) ?: ArtistEntity(it.name, it.key)
        })
        dao.upsertAlbums(albums.values.map {
            oldAlbums[it.key]?.copy(title = it.name) ?: AlbumEntity(it.name, sourceKey = it.key)
        })
        val artistIds = dao.artistsBySourceKeys(artists.keys.toList()).associate { it.sourceKey to it.id }
        val albumIds = dao.albumsBySourceKeys(albums.keys.toList()).associate { it.sourceKey to it.id }
        val oldTracks = dao.tracksByUris(unique.map(ScannedTrack::uri)).associateBy { it.uri }
        dao.upsertTracks(unique.map {
            val old = oldTracks[it.uri]
            TrackEntity(
                uri = it.uri,
                title = it.title,
                path = old?.path,
                albumId = albumIds[it.album?.key],
                artistId = artistIds[it.artist?.key],
                durationMs = it.durationMs,
                discNumber = it.discNumber,
                trackNumber = it.trackNumber,
                id = old?.id ?: 0,
            )
        })
    }

    private companion object {
        const val BATCH_SIZE = 128
    }
}
