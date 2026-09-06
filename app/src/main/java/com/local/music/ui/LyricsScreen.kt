package com.local.music.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.local.music.data.Lyrics
import com.local.music.data.LyricsClient
import com.local.music.player.PlayerViewModel

@Composable
fun LyricsScreen(player: PlayerViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val track = player.currentTrack
    var lyrics by remember(track?.id) { mutableStateOf<Lyrics?>(null) }   // null = loading

    LaunchedEffect(track?.id) {
        lyrics = if (track != null) LyricsClient.fetch(context, track) else Lyrics.None
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Text("Lyrics", style = MaterialTheme.typography.titleLarge)
            }
            when (val l = lyrics) {
                null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is Lyrics.Synced -> SyncedLyrics(l, player)
                is Lyrics.Plain -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)) {
                    Text(l.text, style = MaterialTheme.typography.titleMedium)
                }
                Lyrics.None -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No lyrics found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SyncedLyrics(lyrics: Lyrics.Synced, player: PlayerViewModel) {
    val lines = lyrics.lines
    val active = remember(player.positionMs, lines) {
        lines.indexOfLast { it.timeMs <= player.positionMs }.takeIf { it >= 0 } ?: -1
    }
    val listState = rememberLazyListState()
    LaunchedEffect(active) {
        if (active >= 0) runCatching { listState.animateScrollToItem(active) }
    }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        itemsIndexed(lines) { i, line ->
            Text(
                line.text.ifBlank { "♪" },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (i == active) FontWeight.Bold else FontWeight.Normal,
                color = if (i == active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
