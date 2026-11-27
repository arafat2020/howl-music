package com.howl.music.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.howl.music.data.db.entities.PlaylistEntity
import com.howl.music.data.db.entities.PlaylistTrackEntity
import com.howl.music.data.db.entities.TrackEntity

@Dao
interface PlaylistDao {
    @Insert suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Insert suspend fun insertEntry(entry: PlaylistTrackEntity): Long

    @Query("SELECT * FROM playlists ORDER BY name, id")
    suspend fun playlists(): List<PlaylistEntity>

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position")
    suspend fun entries(playlistId: Long): List<PlaylistTrackEntity>

    @Query("""
        SELECT tracks.* FROM playlist_tracks
        JOIN tracks ON tracks.id = playlist_tracks.trackId
        WHERE playlist_tracks.playlistId = :playlistId
        ORDER BY playlist_tracks.position
    """)
    suspend fun tracks(playlistId: Long): List<TrackEntity>
}
