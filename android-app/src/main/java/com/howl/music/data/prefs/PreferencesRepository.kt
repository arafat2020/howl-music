package com.howl.music.data.prefs

import androidx.room.withTransaction
import com.howl.music.data.db.HowlDatabase
import com.howl.music.data.db.entities.PreferencesEntity
import kotlinx.coroutines.flow.map

enum class RepeatDefault { Off, One, All }

data class PlaybackPreferences(
    val repeat: RepeatDefault = RepeatDefault.Off,
    val shuffle: Boolean = false,
    val reducedMotion: Boolean = false,
)

class PreferencesRepository(private val db: HowlDatabase) {
    private val dao = db.preferencesDao()

    val reducedMotion = dao.observeReducedMotion().map { it ?: false }

    suspend fun setReducedMotion(enabled: Boolean) = db.withTransaction {
        save(get().copy(reducedMotion = enabled))
    }

    suspend fun get(): PlaybackPreferences {
        val row = dao.get() ?: return PlaybackPreferences()
        return PlaybackPreferences(
            repeat = RepeatDefault.entries.firstOrNull { it.name == row.repeatMode } ?: RepeatDefault.Off,
            shuffle = row.shuffle,
            reducedMotion = row.reducedMotion,
        )
    }

    suspend fun save(preferences: PlaybackPreferences) {
        dao.save(PreferencesEntity(preferences.repeat.name, preferences.shuffle, preferences.reducedMotion))
    }
}
