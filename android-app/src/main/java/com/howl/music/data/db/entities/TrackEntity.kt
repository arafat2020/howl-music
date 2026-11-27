package com.howl.music.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracks",
    foreignKeys = [
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = ArtistEntity::class,
            parentColumns = ["id"],
            childColumns = ["artistId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("uri", unique = true),
        Index("title"),
        Index(value = ["albumId", "discNumber", "trackNumber", "title"]),
        Index(value = ["artistId", "title"]),
    ],
)
data class TrackEntity(
    val uri: String,
    val title: String,
    val path: String? = null,
    val albumId: Long? = null,
    val artistId: Long? = null,
    val durationMs: Long? = null,
    val discNumber: Int? = null,
    val trackNumber: Int? = null,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
)
