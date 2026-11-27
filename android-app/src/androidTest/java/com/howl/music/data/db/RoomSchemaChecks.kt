package com.howl.music.data.db

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import com.howl.music.data.db.entities.AlbumEntity
import com.howl.music.data.db.entities.ArtistEntity
import com.howl.music.data.db.entities.PlaylistEntity
import com.howl.music.data.db.entities.PlaylistTrackEntity
import com.howl.music.data.db.entities.TrackEntity
import kotlinx.coroutines.runBlocking

internal fun verifyRoomSchema(context: Context) = runBlocking {
    val db = Room.inMemoryDatabaseBuilder(context, HowlDatabase::class.java).build()
    try {
        val library = db.libraryDao()
        val artist = library.insertArtist(ArtistEntity("Shared name", sourceKey = "test:artist:1"))
        val otherArtist = library.insertArtist(ArtistEntity("Shared name", sourceKey = "test:artist:2"))
        check(artist != otherArtist)
        check(library.artists().map { it.id } == listOf(artist, otherArtist))
        check(library.artistBySourceKey("test:artist:1")?.id == artist)

        val album = library.insertAlbum(AlbumEntity("Demo", artist, "test:album:1"))
        check(library.albumBySourceKey("test:album:1")?.id == album)
        check(library.albumsByArtist(artist).single().id == album)

        val firstUri = "content://media/external_primary/audio/media/10"
        val secondUri = "content://media/external_primary/audio/media/11"
        val second = library.insertTrack(
            TrackEntity(secondUri, "Alpha", albumId = album, artistId = otherArtist, trackNumber = 2),
        )
        val first = library.insertTrack(
            TrackEntity(firstUri, "Zulu", albumId = album, artistId = artist, trackNumber = 1),
        )
        check(library.tracksByAlbum(album).map { it.id } == listOf(first, second))
        check(library.tracksByArtist(artist).single().id == first)
        check(library.tracksByArtist(otherArtist).single().id == second)
        check(library.tracks().map { it.id } == listOf(second, first))

        expectConstraint { library.insertTrack(TrackEntity(firstUri, "Duplicate URI")) }
        expectConstraint { library.insertTrack(TrackEntity("file:///orphan.wav", "Orphan", albumId = -1)) }
        val original = checkNotNull(library.trackByUri(firstUri))
        check(original.id == first && original.path == null)
        check(library.updateTrack(original.copy(title = "Updated")) == 1)
        check(library.trackByUri(firstUri)?.title == "Updated")

        val playlists = db.playlistDao()
        val playlist = playlists.insertPlaylist(PlaylistEntity("Favorites"))
        check(playlists.playlists().single().id == playlist)
        playlists.insertEntry(PlaylistTrackEntity(playlist, first, position = 2))
        playlists.insertEntry(PlaylistTrackEntity(playlist, second, position = 0))
        playlists.insertEntry(PlaylistTrackEntity(playlist, first, position = 1))
        check(playlists.entries(playlist).map { it.position } == listOf(0, 1, 2))
        check(playlists.tracks(playlist).map { it.id } == listOf(second, first, first))
        expectConstraint { playlists.insertEntry(PlaylistTrackEntity(playlist, second, position = 1)) }

        // Exercise the foreign-key actions without adding mutation APIs from the playlists phase.
        val sql = db.openHelper.writableDatabase
        sql.execSQL("DELETE FROM albums WHERE id = ?", arrayOf(album))
        check(library.trackByUri(firstUri)?.albumId == null)
        sql.execSQL("DELETE FROM artists WHERE id = ?", arrayOf(artist))
        check(library.trackByUri(firstUri)?.artistId == null)
        sql.execSQL("DELETE FROM tracks WHERE id = ?", arrayOf(first))
        check(playlists.tracks(playlist).single().id == second)
        sql.execSQL("DELETE FROM playlists WHERE id = ?", arrayOf(playlist))
        check(playlists.entries(playlist).isEmpty())
        check(library.trackByUri(secondUri)?.id == second)
    } finally {
        db.close()
    }
}

private suspend fun expectConstraint(operation: suspend () -> Unit) {
    try {
        operation()
        error("Expected a SQLite constraint failure")
    } catch (_: SQLiteConstraintException) {
        // The write must fail without changing the existing row.
    }
}
