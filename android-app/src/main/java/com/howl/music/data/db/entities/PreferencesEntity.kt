package com.howl.music.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_preferences")
data class PreferencesEntity(
    val repeatMode: String,
    val shuffle: Boolean,
    val reducedMotion: Boolean,
    @PrimaryKey val id: Int = 1,
)
