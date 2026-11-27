package com.howl.music.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.howl.music.data.db.dao.LibraryDao
import com.howl.music.data.db.dao.PlaylistDao
import com.howl.music.data.db.entities.AlbumEntity
import com.howl.music.data.db.entities.ArtistEntity
import com.howl.music.data.db.entities.PlaylistEntity
import com.howl.music.data.db.entities.PlaylistTrackEntity
import com.howl.music.data.db.entities.TrackEntity

@Database(
    entities = [
        ArtistEntity::class,
        AlbumEntity::class,
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class HowlDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        /** The caller owns this instance and should share it across library consumers. */
        fun open(context: Context): HowlDatabase =
            Room.databaseBuilder(context.applicationContext, HowlDatabase::class.java, "howl-library.db")
                .build()
    }
}
