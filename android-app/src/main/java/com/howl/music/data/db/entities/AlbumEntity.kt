package com.howl.music.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "albums",
    foreignKeys = [ForeignKey(
        entity = ArtistEntity::class,
        parentColumns = ["id"],
        childColumns = ["artistId"],
        onDelete = ForeignKey.SET_NULL,
    )],
    indices = [Index("title"), Index(value = ["artistId", "title"]), Index("sourceKey", unique = true)],
)
data class AlbumEntity(
    val title: String,
    val artistId: Long? = null,
    val sourceKey: String? = null,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
)
