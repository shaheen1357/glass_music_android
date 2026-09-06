package com.local.music.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.local.music.model.Track
import com.local.music.player.PlayerViewModel

@Composable
fun TrackRow(track: Track, onClick: () -> Unit, onAddToPlaylist: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Artwork(track.uri, Modifier.size(48.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
            Text(
                track.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onAddToPlaylist != null) {
            IconButton(onClick = onAddToPlaylist) {
                Icon(Icons.Filled.PlaylistAdd, contentDescription = "Add to playlist")
            }
        }
    }
}

@Composable
fun MiniPlayer(player: PlayerViewModel, onExpand: () -> Unit) {
    val track = player.currentTrack ?: return
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth().clickable(onClick = onExpand)) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Artwork(track.uri, Modifier.size(44.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        track.artist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { player.previous() }) { Icon(Icons.Filled.SkipPrevious, "Previous") }
                IconButton(onClick = { player.togglePlayPause() }) {
                    Icon(if (player.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play/pause")
                }
                IconButton(onClick = { player.next() }) { Icon(Icons.Filled.SkipNext, "Next") }
            }
            val dur = player.durationMs.coerceAtLeast(1L)
            LinearProgressIndicator(
                progress = { (player.positionMs.toFloat() / dur.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(2.dp)
            )
        }
    }
}
