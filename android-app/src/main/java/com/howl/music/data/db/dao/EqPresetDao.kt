package com.howl.music.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.howl.music.data.db.entities.EqPresetEntity

@Dao
interface EqPresetDao {
    @Insert suspend fun insert(preset: EqPresetEntity): Long

    @Update suspend fun update(preset: EqPresetEntity): Int

    @Query("SELECT * FROM eq_presets WHERE id = :id")
    suspend fun get(id: Long): EqPresetEntity?

    @Query("SELECT * FROM eq_presets ORDER BY name, id")
    suspend fun all(): List<EqPresetEntity>

    @Query("DELETE FROM eq_presets WHERE id = :id")
    suspend fun delete(id: Long): Int
}
