package com.local.music.library

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.local.music.data.PlaylistStorage
import com.local.music.data.RecentsStorage
import com.local.music.model.Album
import com.local.music.model.Artist
import com.local.music.model.Playlist
import com.local.music.model.RecentSearch
import com.local.music.model.Track
import com.local.music.data.MusicRepository
import kotlinx.coroutines.launch

class LibraryViewModel(app: Application) : AndroidViewModel(app) {

    var tracks by mutableStateOf<List<Track>>(emptyList()); private set
    var isScanning by mutableStateOf(false); private set
    var playlists by mutableStateOf<List<Playlist>>(emptyList()); private set
    var recents by mutableStateOf<List<RecentSearch>>(emptyList()); private set

    private val tracksById: Map<Long, Track> get() = tracks.associateBy { it.id }

    val albums: List<Album>
        get() = tracks.groupBy { it.albumId }.map { (albumId, list) ->
            Album(albumId, list.first().album, list.first().artist,
                list.sortedBy { it.title.lowercase() })
        }.sortedBy { it.title.lowercase() }

    val artists: List<Artist>
        get() = tracks.groupBy { it.artist }.map { (name, list) ->
            Artist(name, list.sortedBy { it.title.lowercase() })
        }.sortedBy { it.name.lowercase() }

    init {
        playlists = PlaylistStorage.load(app)
        recents = RecentsStorage.load(app)
    }

    fun scan() {
        viewModelScope.launch {
            isScanning = true
            tracks = MusicRepository.scan(getApplication())
            isScanning = false
        }
    }

    // MARK: playlists
    fun createPlaylist(name: String): String {
        val id = "pl-" + System.currentTimeMillis().toString(36)
        playlists = playlists + Playlist(id, name.ifBlank { "New Playlist" }, emptyList())
        persistPlaylists()
        return id
    }

    fun addToPlaylist(playlistId: String, trackId: Long) {
        playlists = playlists.map { pl ->
            if (pl.id == playlistId && trackId !in pl.trackIds)
                pl.copy(trackIds = pl.trackIds + trackId) else pl
        }
        persistPlaylists()
    }

    fun removeFromPlaylist(playlistId: String, trackId: Long) {
        playlists = playlists.map { pl ->
            if (pl.id == playlistId) pl.copy(trackIds = pl.trackIds - trackId) else pl
        }
        persistPlaylists()
    }

    fun deletePlaylist(playlistId: String) {
        playlists = playlists.filterNot { it.id == playlistId }
        persistPlaylists()
    }

    fun tracksOf(playlist: Playlist): List<Track> {
        val byId = tracksById
        return playlist.trackIds.mapNotNull { byId[it] }
    }

    private fun persistPlaylists() = PlaylistStorage.save(getApplication(), playlists)

    // MARK: recents
    fun recordRecentSong(trackId: Long) = addRecent(RecentSearch.Song(trackId))
    fun recordRecentQuery(text: String) {
        if (text.isBlank()) return
        addRecent(RecentSearch.Query(text.trim()))
    }

    private fun addRecent(item: RecentSearch) {
        val key: (RecentSearch) -> String = {
            when (it) {
                is RecentSearch.Song -> "s:${it.trackId}"
                is RecentSearch.Query -> "q:${it.text.lowercase()}"
            }
        }
        val k = key(item)
        recents = (listOf(item) + recents.filterNot { key(it) == k }).take(20)
        RecentsStorage.save(getApplication(), recents)
    }

    fun clearRecents() {
        recents = emptyList()
        RecentsStorage.save(getApplication(), recents)
    }

    fun trackById(id: Long): Track? = tracksById[id]
}
