package com.howl.music.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.howl.music.data.db.entities.PreferencesEntity

@Dao
interface PreferencesDao {
    @Query("SELECT * FROM playback_preferences WHERE id = 1")
    suspend fun get(): PreferencesEntity?

    @Upsert suspend fun save(preferences: PreferencesEntity)
}
