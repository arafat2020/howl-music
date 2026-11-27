package com.howl.music.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.howl.music.bridge.EqState
import com.howl.music.bridge.HowlEngine
import com.howl.music.data.db.HowlDatabase
import com.howl.music.data.db.entities.TrackEntity
import com.howl.music.data.eq.EqPresetRepository
import com.howl.music.data.playlist.PlaylistRepository
import com.howl.music.data.prefs.PlaybackPreferences
import com.howl.music.data.prefs.PreferencesRepository
import com.howl.music.data.prefs.RepeatDefault
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.File

private const val NAME = "howl-user-data-instrumentation.db"
private const val ORIGINAL_URI = "file:///migration-original.wav"
private const val EXTRA_URI = "file:///migration-extra.wav"
private val preferences = PlaybackPreferences(RepeatDefault.All, shuffle = true, reducedMotion = true)
private val expectedEq = EqState(-6f, FloatArray(EqState.BAND_COUNT) { it - 5f })

internal fun writeUserData(context: Context, testContext: Context) = runBlocking {
    context.deleteDatabase(NAME)
    seedVersionOne(context, testContext)
    val db = Room.databaseBuilder(context, HowlDatabase::class.java, NAME).build()
    try {
        val playlists = PlaylistRepository(db)
        val settings = PreferencesRepository(db)
        val presets = EqPresetRepository(db)
        check(playlists.playlists().single().name == "Original")
        check(playlists.tracks(1).single().uri == ORIGINAL_URI)
        check(settings.get() == PlaybackPreferences())
        check(presets.all().isEmpty())

        val original = checkNotNull(db.libraryDao().trackByUri(ORIGINAL_URI))
        val extra = db.libraryDao().insertTrack(TrackEntity(EXTRA_URI, "Extra"))
        check(playlists.rename(1, "  Favorites  "))
        val oldEntry = playlists.entries(1).single()
        val moved = playlists.append(1, extra)
        val repeated = playlists.append(1, original.id)
        check(playlists.move(1, moved, 0))
        check(playlists.entries(1).map { it.id } == listOf(moved, oldEntry.id, repeated))
        check(playlists.remove(1, oldEntry.id))
        check(!playlists.remove(1, oldEntry.id))
        val deleted = playlists.create("Temporary")
        playlists.append(deleted, extra)
        check(playlists.delete(deleted))
        check(playlists.entries(deleted).isEmpty())

        settings.save(preferences)
        val preset = presets.create("Draft", EqState())
        check(presets.update(preset, "  Evening  ", expectedEq))
        val removedPreset = presets.create("Temporary", EqState())
        check(presets.delete(removedPreset))
        check(presets.get(removedPreset) == null)
    } finally {
        db.close()
    }
}

internal fun readUserData(context: Context, fixture: File) = runBlocking {
    val db = Room.databaseBuilder(context, HowlDatabase::class.java, NAME).build()
    try {
        val playlists = PlaylistRepository(db)
        val favorites = playlists.playlists().single()
        check(favorites.name == "Favorites")
        check(playlists.tracks(favorites.id).map { it.uri } == listOf(EXTRA_URI, ORIGINAL_URI))
        check(PreferencesRepository(db).get() == preferences)
        val presets = EqPresetRepository(db)
        val preset = presets.all().single()
        check(preset.name == "Evening")
        check(preset.state.preampDb == expectedEq.preampDb)
        check(preset.state.bandsDb.contentEquals(expectedEq.bandsDb))

        HowlEngine.create().use { engine ->
            check(engine.setPlayMode(HowlEngine.PlayMode.File) == 0)
            check(engine.load(fixture.absolutePath) == 0)
            check(engine.setRepeatMode(HowlEngine.RepeatMode.One) == 0)
            check(engine.start() == 0)
            check(presets.apply(preset.id, engine) == 0)
            check(engine.transport().playing)
            check(presets.apply(-1, engine) == null)
        }
    } finally {
        db.close()
        context.deleteDatabase(NAME)
    }
}

private fun seedVersionOne(context: Context, testContext: Context) {
    val path = context.getDatabasePath(NAME)
    check(path.parentFile?.let { it.isDirectory || it.mkdirs() } == true)
    val raw = testContext.assets.open("com.howl.music.data.db.HowlDatabase/1.json")
        .bufferedReader().use { it.readText() }
    val schema = JSONObject(raw).getJSONObject("database")
    SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val table = entities.getJSONObject(i)
            val name = table.getString("tableName")
            db.execSQL(table.getString("createSql").replace("\${TABLE_NAME}", name))
            val indices = table.getJSONArray("indices")
            for (index in 0 until indices.length()) {
                db.execSQL(indices.getJSONObject(index).getString("createSql").replace("\${TABLE_NAME}", name))
            }
        }
        val setup = schema.getJSONArray("setupQueries")
        for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
        db.execSQL("INSERT INTO tracks (id, uri, title) VALUES (1, ?, 'Original track')", arrayOf(ORIGINAL_URI))
        db.execSQL("INSERT INTO playlists (id, name) VALUES (1, 'Original')")
        db.execSQL("INSERT INTO playlist_tracks (id, playlistId, trackId, position) VALUES (1, 1, 1, 0)")
        db.version = 1
    }
}
