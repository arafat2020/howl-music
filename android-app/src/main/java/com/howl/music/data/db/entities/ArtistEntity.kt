package com.howl.music.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "artists",
    indices = [Index("name"), Index("sourceKey", unique = true)],
)
data class ArtistEntity(
    val name: String,
    val sourceKey: String? = null,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
)
