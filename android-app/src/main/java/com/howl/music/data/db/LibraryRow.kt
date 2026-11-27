package com.howl.music.data.db

data class LibraryRow(
    val id: Long,
    val title: String,
    val uri: String?,
    val artist: String?,
    val album: String?,
)
