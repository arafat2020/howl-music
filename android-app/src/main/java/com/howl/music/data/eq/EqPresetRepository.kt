package com.howl.music.data.eq

import com.howl.music.bridge.EqState
import com.howl.music.bridge.HowlEngine
import com.howl.music.data.db.HowlDatabase
import com.howl.music.data.db.entities.EqBandGains
import com.howl.music.data.db.entities.EqPresetEntity

data class EqPreset(val id: Long, val name: String, val state: EqState)

class EqPresetRepository(db: HowlDatabase) {
    private val dao = db.eqPresetDao()

    suspend fun create(name: String, state: EqState): Long = dao.insert(entity(0, name, state))

    suspend fun update(id: Long, name: String, state: EqState): Boolean =
        dao.update(entity(id, name, state)) == 1

    suspend fun get(id: Long): EqPreset? = dao.get(id)?.let(::preset)

    suspend fun all(): List<EqPreset> = dao.all().map(::preset)

    suspend fun delete(id: Long): Boolean = dao.delete(id) == 1

    /** Call from the engine's control thread. Returns its status, or null if the preset is missing. */
    suspend fun apply(id: Long, engine: HowlEngine): Int? =
        get(id)?.let { engine.applyEqState(it.state) }

    private fun preset(row: EqPresetEntity): EqPreset =
        EqPreset(row.id, row.name, EqState(row.preampDb, row.bands.asArray()))

    private fun entity(id: Long, name: String, state: EqState): EqPresetEntity {
        val title = name.trim()
        require(title.isNotEmpty()) { "Preset name is required" }
        val b = state.bandsDb
        return EqPresetEntity(
            name = title,
            preampDb = state.preampDb,
            bands = EqBandGains(b[0], b[1], b[2], b[3], b[4], b[5], b[6], b[7], b[8], b[9]),
            id = id,
        )
    }
}
