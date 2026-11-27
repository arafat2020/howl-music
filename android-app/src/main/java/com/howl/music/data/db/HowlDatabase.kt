package com.howl.music.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.howl.music.data.db.dao.EqPresetDao
import com.howl.music.data.db.dao.LibraryDao
import com.howl.music.data.db.dao.PlaylistDao
import com.howl.music.data.db.dao.PreferencesDao
import com.howl.music.data.db.entities.AlbumEntity
import com.howl.music.data.db.entities.ArtistEntity
import com.howl.music.data.db.entities.EqPresetEntity
import com.howl.music.data.db.entities.PlaylistEntity
import com.howl.music.data.db.entities.PlaylistTrackEntity
import com.howl.music.data.db.entities.TrackEntity
import com.howl.music.data.db.entities.PreferencesEntity

@Database(
    entities = [
        ArtistEntity::class,
        AlbumEntity::class,
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        PreferencesEntity::class,
        EqPresetEntity::class,
    ],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
    exportSchema = true,
)
abstract class HowlDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun preferencesDao(): PreferencesDao
    abstract fun eqPresetDao(): EqPresetDao

    companion object {
        /** The caller owns this instance and should share it across library consumers. */
        fun open(context: Context): HowlDatabase =
            Room.databaseBuilder(context.applicationContext, HowlDatabase::class.java, "howl-library.db")
                .build()
    }
}
