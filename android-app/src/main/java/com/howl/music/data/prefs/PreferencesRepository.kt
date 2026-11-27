package com.howl.music.data.prefs

import com.howl.music.data.db.HowlDatabase
import com.howl.music.data.db.entities.PreferencesEntity

enum class RepeatDefault { Off, One, All }

data class PlaybackPreferences(
    val repeat: RepeatDefault = RepeatDefault.Off,
    val shuffle: Boolean = false,
    val reducedMotion: Boolean = false,
)

class PreferencesRepository(db: HowlDatabase) {
    private val dao = db.preferencesDao()

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
