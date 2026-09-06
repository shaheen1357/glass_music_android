package com.local.music.model

import android.net.Uri

data class Track(
    val id: Long,
    val uri: Uri,                 // content://media/external/audio/media/<id>
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
)
