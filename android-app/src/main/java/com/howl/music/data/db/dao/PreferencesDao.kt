package com.howl.music.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import com.howl.music.data.db.entities.PreferencesEntity

@Dao
interface PreferencesDao {
    @Query("SELECT * FROM playback_preferences WHERE id = 1")
    suspend fun get(): PreferencesEntity?

    @Query("SELECT reducedMotion FROM playback_preferences WHERE id = 1")
    fun observeReducedMotion(): Flow<Boolean?>

    @Upsert suspend fun save(preferences: PreferencesEntity)
}
