package com.local.music.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.local.music.library.LibraryViewModel
import com.local.music.model.Track
import com.local.music.player.PlayerViewModel

private val audioPermission: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

@Composable
fun RootScreen() {
    val context = LocalContext.current
    val player: PlayerViewModel = viewModel()
    val library: LibraryViewModel = viewModel()

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        )
    }
    var selectedTab by remember { mutableIntStateOf(1) }
    var showNowPlaying by remember { mutableStateOf(false) }
    var addToPlaylistFor by remember { mutableStateOf<Track?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> granted = result[audioPermission] == true }

    fun requestPermissions() {
        val perms = buildList {
            add(audioPermission)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
        launcher.launch(perms)
    }

    LaunchedEffect(Unit) { if (!granted) requestPermissions() }
    LaunchedEffect(granted) { if (granted) library.scan() }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                Column {
                    if (player.hasTrack) MiniPlayer(player, onExpand = { showNowPlaying = true })
                    NavigationBar {
                        NavigationBarItem(
                            selected = selectedTab == 0, onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Filled.Home, "Home") }, label = { Text("Home") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1, onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Filled.LibraryMusic, "Library") }, label = { Text("Library") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2, onClick = { selectedTab = 2 },
                            icon = { Icon(Icons.Filled.Search, "Search") }, label = { Text("Search") }
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (selectedTab) {
                    0 -> HomeScreen(library)
                    1 -> LibraryScreen(library, player, granted, onGrant = ::requestPermissions,
                        onAddToPlaylist = { addToPlaylistFor = it })
                    else -> SearchScreen(library, player, onAddToPlaylist = { addToPlaylistFor = it })
                }
            }
        }

        if (showNowPlaying && player.hasTrack) {
            NowPlayingScreen(player, onClose = { showNowPlaying = false })
        }
    }

    addToPlaylistFor?.let { track ->
        AddToPlaylistDialog(
            library = library,
            onDismiss = { addToPlaylistFor = null },
            onPick = { playlistId ->
                library.addToPlaylist(playlistId, track.id)
                addToPlaylistFor = null
            }
        )
    }
}

@Composable
private fun AddToPlaylistDialog(
    library: LibraryViewModel,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val id = library.createPlaylist(newName.ifBlank { "New Playlist" })
                onPick(id)
            }) { Text("New playlist") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Add to playlist") },
        text = {
            Column {
                library.playlists.forEach { pl ->
                    TextButton(onClick = { onPick(pl.id) }) { Text(pl.name) }
                }
            }
        }
    )
}
