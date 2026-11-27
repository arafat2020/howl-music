package com.howl.music.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.howl.music.data.db.entities.AlbumEntity
import com.howl.music.data.db.entities.ArtistEntity
import com.howl.music.data.db.entities.TrackEntity
import com.howl.music.data.db.LibraryRow
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Query("""
        SELECT t.id, t.title, t.uri, a.name AS artist, b.title AS album
        FROM tracks t LEFT JOIN artists a ON a.id = t.artistId
        LEFT JOIN albums b ON b.id = t.albumId
        WHERE (:albumId = -1 OR t.albumId = :albumId) AND (:artistId = -1 OR t.artistId = :artistId)
        AND (instr(lower(t.title), lower(:query)) > 0 OR
             instr(lower(COALESCE(a.name, '')), lower(:query)) > 0 OR
             instr(lower(COALESCE(b.title, '')), lower(:query)) > 0)
        ORDER BY CASE WHEN NOT :descending THEN t.title END COLLATE NOCASE ASC,
                 CASE WHEN :descending THEN t.title END COLLATE NOCASE DESC, t.id
        LIMIT :limit
    """)
    fun browseTracks(query: String, descending: Boolean, albumId: Long, artistId: Long, limit: Int): Flow<List<LibraryRow>>

    @Query("""
        SELECT b.id, b.title, (SELECT t.uri FROM tracks t WHERE t.albumId = b.id ORDER BY t.id LIMIT 1) AS uri,
               a.name AS artist, NULL AS album
        FROM albums b LEFT JOIN artists a ON a.id = b.artistId
        WHERE instr(lower(b.title), lower(:query)) > 0
        ORDER BY CASE WHEN NOT :descending THEN b.title END COLLATE NOCASE ASC,
                 CASE WHEN :descending THEN b.title END COLLATE NOCASE DESC, b.id
        LIMIT :limit
    """)
    fun browseAlbums(query: String, descending: Boolean, limit: Int): Flow<List<LibraryRow>>

    @Query("""
        SELECT a.id, a.name AS title, (SELECT t.uri FROM tracks t WHERE t.artistId = a.id ORDER BY t.id LIMIT 1) AS uri,
               NULL AS artist, NULL AS album
        FROM artists a
        WHERE instr(lower(a.name), lower(:query)) > 0
        ORDER BY CASE WHEN NOT :descending THEN a.name END COLLATE NOCASE ASC,
                 CASE WHEN :descending THEN a.name END COLLATE NOCASE DESC, a.id
        LIMIT :limit
    """)
    fun browseArtists(query: String, descending: Boolean, limit: Int): Flow<List<LibraryRow>>

    @Insert suspend fun insertArtist(artist: ArtistEntity): Long

    @Insert suspend fun insertAlbum(album: AlbumEntity): Long

    @Insert suspend fun insertTrack(track: TrackEntity): Long

    @Update suspend fun updateTrack(track: TrackEntity): Int

    @Upsert suspend fun upsertArtists(artists: List<ArtistEntity>)

    @Upsert suspend fun upsertAlbums(albums: List<AlbumEntity>)

    @Upsert suspend fun upsertTracks(tracks: List<TrackEntity>)

    @Query("SELECT * FROM artists WHERE sourceKey IN (:keys)")
    suspend fun artistsBySourceKeys(keys: List<String>): List<ArtistEntity>

    @Query("SELECT * FROM albums WHERE sourceKey IN (:keys)")
    suspend fun albumsBySourceKeys(keys: List<String>): List<AlbumEntity>

    @Query("SELECT * FROM tracks WHERE uri IN (:uris)")
    suspend fun tracksByUris(uris: List<String>): List<TrackEntity>

    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun trackCount(): Int

    @Query("SELECT * FROM artists ORDER BY name, id")
    suspend fun artists(): List<ArtistEntity>

    @Query("SELECT * FROM artists WHERE sourceKey = :sourceKey")
    suspend fun artistBySourceKey(sourceKey: String): ArtistEntity?

    @Query("SELECT * FROM albums ORDER BY title, id")
    suspend fun albums(): List<AlbumEntity>

    @Query("SELECT * FROM albums WHERE sourceKey = :sourceKey")
    suspend fun albumBySourceKey(sourceKey: String): AlbumEntity?

    @Query("SELECT * FROM albums WHERE artistId = :artistId ORDER BY title, id")
    suspend fun albumsByArtist(artistId: Long): List<AlbumEntity>

    @Query("SELECT * FROM tracks ORDER BY title, id")
    suspend fun tracks(): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE uri = :uri")
    suspend fun trackByUri(uri: String): TrackEntity?

    @Query("SELECT * FROM tracks WHERE albumId = :albumId ORDER BY discNumber, trackNumber, title, id")
    suspend fun tracksByAlbum(albumId: Long): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE artistId = :artistId ORDER BY title, id")
    suspend fun tracksByArtist(artistId: Long): List<TrackEntity>
}
