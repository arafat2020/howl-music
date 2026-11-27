package com.howl.music.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.howl.music.data.db.entities.AlbumEntity
import com.howl.music.data.db.entities.ArtistEntity
import com.howl.music.data.db.entities.TrackEntity

@Dao
interface LibraryDao {
    @Insert suspend fun insertArtist(artist: ArtistEntity): Long

    @Insert suspend fun insertAlbum(album: AlbumEntity): Long

    @Insert suspend fun insertTrack(track: TrackEntity): Long

    @Update suspend fun updateTrack(track: TrackEntity): Int

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
