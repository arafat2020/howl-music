package com.howl.music.data.db.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "eq_presets", indices = [Index("name")])
data class EqPresetEntity(
    val name: String,
    val preampDb: Float,
    @Embedded val bands: EqBandGains,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
)

data class EqBandGains(
    val band31Hz: Float,
    val band63Hz: Float,
    val band125Hz: Float,
    val band250Hz: Float,
    val band500Hz: Float,
    val band1kHz: Float,
    val band2kHz: Float,
    val band4kHz: Float,
    val band8kHz: Float,
    val band16kHz: Float,
) {
    fun asArray(): FloatArray = floatArrayOf(
        band31Hz, band63Hz, band125Hz, band250Hz, band500Hz,
        band1kHz, band2kHz, band4kHz, band8kHz, band16kHz,
    )
}
