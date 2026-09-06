package com.local.music.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.local.music.library.LibraryViewModel
import com.local.music.model.Album
import com.local.music.model.Artist
import com.local.music.model.Playlist
import com.local.music.model.RecentSearch
import com.local.music.model.Track
import com.local.music.player.PlayerViewModel

@Composable
fun HomeScreen(library: LibraryViewModel) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Music", style = MaterialTheme.typography.headlineMedium)
        Text(
            "${library.tracks.size} songs · ${library.albums.size} albums · ${library.artists.size} artists",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private enum class LibFilter(val label: String) { Playlists("Playlists"), Artists("Artists"), Albums("Albums"), Songs("Songs") }

@Composable
fun LibraryScreen(
    library: LibraryViewModel,
    player: PlayerViewModel,
    granted: Boolean,
    onGrant: () -> Unit,
    onAddToPlaylist: (Track) -> Unit,
) {
    if (!granted) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Allow access to your music to get started.")
            Button(onClick = onGrant, modifier = Modifier.padding(top = 16.dp)) { Text("Grant access") }
        }
        return
    }

    var filter by remember { mutableStateOf(LibFilter.Songs) }
    var openAlbum by remember { mutableStateOf<Album?>(null) }
    var openArtist by remember { mutableStateOf<Artist?>(null) }
    var openPlaylist by remember { mutableStateOf<Playlist?>(null) }

    // Detail screens (simple in-tab stack)
    openAlbum?.let { a ->
        TrackListDetail(a.title, a.tracks, player, onAddToPlaylist) { openAlbum = null }
        return
    }
    openArtist?.let { ar ->
        TrackListDetail(ar.name, ar.tracks, player, onAddToPlaylist) { openArtist = null }
        return
    }
    openPlaylist?.let { pl ->
        val tracks = library.tracksOf(pl)
        TrackListDetail(pl.name, tracks, player, onAddToPlaylist,
            onRemove = { library.removeFromPlaylist(pl.id, it.id) }) { openPlaylist = null }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LibFilter.entries.forEach { f ->
                FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.label) })
            }
        }
        when (filter) {
            LibFilter.Songs -> LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(library.tracks) { i, t ->
                    TrackRow(t, onClick = { player.play(library.tracks, i) }, onAddToPlaylist = { onAddToPlaylist(t) })
                }
            }
            LibFilter.Albums -> LazyColumn(Modifier.fillMaxSize()) {
                items(library.albums, key = { it.id }) { a ->
                    RowItem(a.title, a.artist, a.tracks.firstOrNull()?.uri) { openAlbum = a }
                }
            }
            LibFilter.Artists -> LazyColumn(Modifier.fillMaxSize()) {
                items(library.artists, key = { it.name }) { ar ->
                    RowItem(ar.name, "${ar.tracks.size} songs", ar.tracks.firstOrNull()?.uri) { openArtist = ar }
                }
            }
            LibFilter.Playlists -> LazyColumn(Modifier.fillMaxSize()) {
                items(library.playlists, key = { it.id }) { pl ->
                    RowItem(pl.name, "${pl.trackIds.size} songs", library.tracksOf(pl).firstOrNull()?.uri) { openPlaylist = pl }
                }
            }
        }
    }
}

@Composable
private fun RowItem(title: String, subtitle: String, artUri: android.net.Uri?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Artwork(artUri, Modifier.size(48.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TrackListDetail(
    title: String,
    tracks: List<Track>,
    player: PlayerViewModel,
    onAddToPlaylist: (Track) -> Unit,
    onRemove: ((Track) -> Unit)? = null,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(tracks) { i, t ->
                TrackRow(
                    t,
                    onClick = { player.play(tracks, i) },
                    onAddToPlaylist = if (onRemove != null) ({ onRemove(t) }) else ({ onAddToPlaylist(t) })
                )
            }
        }
    }
}

@Composable
fun SearchScreen(library: LibraryViewModel, player: PlayerViewModel, onAddToPlaylist: (Track) -> Unit) {
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()
    val results = remember(q, library.tracks) {
        if (q.isEmpty()) emptyList()
        else library.tracks.filter {
            it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) || it.album.lowercase().contains(q)
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Songs") },
            singleLine = true,
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, "Clear") }
            },
            modifier = Modifier.fillMaxWidth()
        )
        if (q.isEmpty()) {
            RecentSearches(library, player)
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(results) { i, t ->
                    TrackRow(t, onClick = {
                        player.play(results, i); library.recordRecentSong(t.id)
                    }, onAddToPlaylist = { onAddToPlaylist(t) })
                }
            }
        }
    }
}

@Composable
private fun RecentSearches(library: LibraryViewModel, player: PlayerViewModel) {
    if (library.recents.isEmpty()) return
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Recent Searches", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { library.clearRecents() }) { Icon(Icons.Filled.Clear, "Clear all") }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(library.recents.size) { idx ->
                when (val r = library.recents[idx]) {
                    is RecentSearch.Song -> {
                        val t = library.trackById(r.trackId)
                        if (t != null) TrackRow(t, onClick = { player.play(listOf(t), 0) })
                    }
                    is RecentSearch.Query -> {
                        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(r.text)
                        }
                    }
                }
            }
        }
    }
}
