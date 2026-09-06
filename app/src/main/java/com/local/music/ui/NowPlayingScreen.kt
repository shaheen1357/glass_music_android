package com.local.music.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.local.music.player.PlayerViewModel

private fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
fun NowPlayingScreen(player: PlayerViewModel, onClose: () -> Unit) {
    val track = player.currentTrack ?: return
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Filled.KeyboardArrowDown, "Close") }
                Spacer(Modifier.weight(1f))
                Text("Now Playing", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(48.dp))
            }

            Spacer(Modifier.weight(1f))
            Artwork(
                track.uri,
                Modifier.fillMaxWidth().aspectRatio(1f).padding(horizontal = 8.dp),
                corner = 16.dp
            )
            Spacer(Modifier.height(24.dp))

            Text(track.title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                track.artist,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(16.dp))
            Scrubber(player)

            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { player.toggleShuffle() }) {
                    Icon(
                        Icons.Filled.Shuffle, "Shuffle",
                        tint = if (player.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { player.previous() }) { Icon(Icons.Filled.SkipPrevious, "Previous", modifier = Modifier.size(40.dp)) }
                IconButton(onClick = { player.togglePlayPause() }) {
                    Icon(
                        if (player.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        "Play/pause",
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { player.next() }) { Icon(Icons.Filled.SkipNext, "Next", modifier = Modifier.size(40.dp)) }
                IconButton(onClick = { player.cycleRepeat() }) {
                    Icon(
                        if (player.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        "Repeat",
                        tint = if (player.repeatMode == Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun Scrubber(player: PlayerViewModel) {
    var scrubbing by remember { mutableStateOf(false) }
    var scrub by remember { mutableFloatStateOf(0f) }
    val dur = player.durationMs.coerceAtLeast(1L)
    val value = (if (scrubbing) scrub else player.positionMs.toFloat()).coerceIn(0f, dur.toFloat())

    Column(Modifier.fillMaxWidth()) {
        Slider(
            value = value,
            onValueChange = { scrubbing = true; scrub = it },
            onValueChangeFinished = { player.seekTo(scrub.toLong()); scrubbing = false },
            valueRange = 0f..dur.toFloat()
        )
        Row(Modifier.fillMaxWidth()) {
            Text(fmt(value.toLong()), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.weight(1f))
            Text(fmt(dur), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
        }
    }
}
