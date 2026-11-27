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

    @Insert suspend fun insertEntries(entries: List<PlaylistTrackEntity>)

    @Query("UPDATE playlists SET name = :name WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, name: String): Int

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long): Int

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND id = :entryId")
    suspend fun removeEntry(playlistId: Long, entryId: Long): Int

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearEntries(playlistId: Long)

    @Query("SELECT MAX(position) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun lastPosition(playlistId: Long): Int?

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
