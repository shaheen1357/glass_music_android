package com.local.music.model

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val tracks: List<Track>,
)

data class Artist(
    val name: String,
    val tracks: List<Track>,
) {
    val albumCount: Int get() = tracks.map { it.album }.toSet().size
}

data class Playlist(
    val id: String,
    val name: String,
    val trackIds: List<Long>,
)

/// A recent-search entry: a played song or a typed query (songs-only search, like iOS).
sealed interface RecentSearch {
    data class Song(val trackId: Long) : RecentSearch
    data class Query(val text: String) : RecentSearch
}
