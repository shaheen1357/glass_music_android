package com.local.music.player

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.local.music.model.Track
import com.local.music.playback.PlaybackService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun Track.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(uri)   // content uri → Media3 can load a thumbnail
                .build()
        )
        .build()

private fun MediaItem.toTrack(): Track {
    val md = mediaMetadata
    return Track(
        id = mediaId.toLongOrNull() ?: 0L,
        uri = localConfiguration?.uri ?: Uri.EMPTY,
        title = md.title?.toString() ?: "",
        artist = md.artist?.toString() ?: "",
        album = md.albumTitle?.toString() ?: "",
        albumId = 0L,
        durationMs = 0L,
    )
}

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private var controller: MediaController? = null

    var currentTrack by mutableStateOf<Track?>(null); private set
    var isPlaying by mutableStateOf(false); private set
    var positionMs by mutableStateOf(0L); private set
    var durationMs by mutableStateOf(0L); private set
    var shuffle by mutableStateOf(false); private set
    var repeatMode by mutableStateOf(Player.REPEAT_MODE_OFF); private set
    var queue by mutableStateOf<List<Track>>(emptyList()); private set

    val hasTrack: Boolean get() = currentTrack != null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            currentTrack = mediaItem?.toTrack()
            durationMs = (controller?.duration ?: 0L).coerceAtLeast(0L)
        }
        override fun onShuffleModeEnabledChanged(enabled: Boolean) { shuffle = enabled }
        override fun onRepeatModeChanged(mode: Int) { repeatMode = mode }
        override fun onPlaybackStateChanged(state: Int) {
            durationMs = (controller?.duration ?: 0L).coerceAtLeast(0L)
        }
    }

    init {
        val ctx = getApplication<Application>()
        val token = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        val future = MediaController.Builder(ctx, token).buildAsync()
        future.addListener({
            val c = future.get()
            controller = c
            c.addListener(listener)
            currentTrack = c.currentMediaItem?.toTrack()
            isPlaying = c.isPlaying
            shuffle = c.shuffleModeEnabled
            repeatMode = c.repeatMode
            durationMs = c.duration.coerceAtLeast(0L)
        }, ContextCompat.getMainExecutor(ctx))

        viewModelScope.launch {
            while (true) {
                controller?.let {
                    positionMs = it.currentPosition.coerceAtLeast(0L)
                    durationMs = it.duration.coerceAtLeast(0L)
                }
                delay(500)
            }
        }
    }

    fun play(tracks: List<Track>, startIndex: Int) {
        val c = controller ?: return
        if (tracks.isEmpty() || startIndex !in tracks.indices) return
        c.shuffleModeEnabled = false
        c.setMediaItems(tracks.map { it.toMediaItem() }, startIndex, 0L)
        c.prepare()
        c.play()
        queue = tracks
    }

    fun playShuffled(tracks: List<Track>) {
        val c = controller ?: return
        if (tracks.isEmpty()) return
        c.shuffleModeEnabled = true
        c.setMediaItems(tracks.map { it.toMediaItem() }, tracks.indices.random(), 0L)
        c.prepare()
        c.play()
        queue = tracks
    }

    fun togglePlayPause() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun next() { controller?.seekToNextMediaItem() }
    fun previous() {
        val c = controller ?: return
        if (c.currentPosition > 3000) c.seekTo(0) else c.seekToPreviousMediaItem()
    }
    fun seekTo(ms: Long) { controller?.seekTo(ms) }
    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        super.onCleared()
    }
}
