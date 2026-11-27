package com.howl.music.data.playlist

import androidx.room.withTransaction
import com.howl.music.data.db.HowlDatabase
import com.howl.music.data.db.entities.PlaylistEntity
import com.howl.music.data.db.entities.PlaylistTrackEntity

class PlaylistRepository(private val db: HowlDatabase) {
    private val dao = db.playlistDao()

    suspend fun playlists() = dao.playlists()

    suspend fun entries(playlistId: Long) = dao.entries(playlistId)

    suspend fun tracks(playlistId: Long) = dao.tracks(playlistId)

    suspend fun create(name: String): Long = dao.insertPlaylist(PlaylistEntity(cleanName(name)))

    suspend fun rename(playlistId: Long, name: String): Boolean =
        dao.renamePlaylist(playlistId, cleanName(name)) == 1

    suspend fun delete(playlistId: Long): Boolean = dao.deletePlaylist(playlistId) == 1

    suspend fun append(playlistId: Long, trackId: Long): Long = db.withTransaction {
        val last = dao.lastPosition(playlistId)?.coerceAtLeast(-1) ?: -1
        check(last < Int.MAX_VALUE) { "Playlist positions are full" }
        dao.insertEntry(PlaylistTrackEntity(playlistId, trackId, last + 1))
    }

    suspend fun remove(playlistId: Long, entryId: Long): Boolean =
        dao.removeEntry(playlistId, entryId) == 1

    /** Positions are zero-based indexes into the current ordered list. */
    suspend fun move(playlistId: Long, entryId: Long, newIndex: Int): Boolean = db.withTransaction {
        val entries = dao.entries(playlistId).toMutableList()
        val previous = entries.indexOfFirst { it.id == entryId }
        if (previous == -1) return@withTransaction false
        require(newIndex in entries.indices) { "Playlist destination is out of range" }
        if (previous == newIndex) return@withTransaction true

        entries.add(newIndex, entries.removeAt(previous))
        // Clear and reinsert with the same entry IDs to avoid intermediate unique-position conflicts.
        dao.clearEntries(playlistId)
        dao.insertEntries(entries.mapIndexed { position, entry -> entry.copy(position = position) })
        true
    }

    private fun cleanName(name: String): String =
        name.trim().also { require(it.isNotEmpty()) { "Playlist name is required" } }
}
