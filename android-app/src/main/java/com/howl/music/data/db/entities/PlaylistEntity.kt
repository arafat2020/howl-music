package com.howl.music.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "playlists", indices = [Index("name")])
data class PlaylistEntity(
    val name: String,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
)
