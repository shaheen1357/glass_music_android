package com.local.music.data

import android.content.Context
import com.local.music.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class LyricLine(val timeMs: Long, val text: String)

sealed interface Lyrics {
    data class Synced(val lines: List<LyricLine>) : Lyrics
    data class Plain(val text: String) : Lyrics
    data object None : Lyrics
}

/// LRCLIB (lrclib.net) — free, keyless synced-lyrics API for local players.
object LyricsClient {

    suspend fun fetch(context: Context, track: Track): Lyrics = withContext(Dispatchers.IO) {
        cacheRead(context, track)?.let { return@withContext it }
        val result = runCatching { request(track) }.getOrNull() ?: Lyrics.None
        cacheWrite(context, track, result)
        result
    }

    private fun request(track: Track): Lyrics {
        if (track.artist.isBlank() || track.title.isBlank()) return Lyrics.None
        val q = "artist_name=${enc(track.artist)}&track_name=${enc(track.title)}" +
            "&album_name=${enc(track.album)}&duration=${track.durationMs / 1000}"
        val conn = (URL("https://lrclib.net/api/get?$q").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 12000
            setRequestProperty("User-Agent", "MusicAndroid/1.0 (local player)")
        }
        try {
            when (conn.responseCode) {
                200 -> {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val o = JSONObject(body)
                    val synced = o.optString("syncedLyrics").takeIf { it.isNotBlank() }
                    val plain = o.optString("plainLyrics").takeIf { it.isNotBlank() }
                    return when {
                        synced != null -> parseLrc(synced).let { if (it.isEmpty()) Lyrics.None else Lyrics.Synced(it) }
                        plain != null -> Lyrics.Plain(plain)
                        else -> Lyrics.None
                    }
                }
                else -> return Lyrics.None
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun parseLrc(text: String): List<LyricLine> {
        val re = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")
        val out = mutableListOf<LyricLine>()
        text.split('\n').forEach { raw ->
            val matches = re.findAll(raw).toList()
            if (matches.isEmpty()) return@forEach
            val lyric = raw.substring(matches.last().range.last + 1).trim()
            matches.forEach { m ->
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val frac = m.groupValues[3]
                val ms = when (frac.length) { 0 -> 0L; 1 -> frac.toLong() * 100; 2 -> frac.toLong() * 10; else -> frac.take(3).toLong() }
                out += LyricLine(min * 60_000 + sec * 1000 + ms, lyric)
            }
        }
        return out.sortedBy { it.timeMs }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    // MARK: cache
    private fun cacheFile(context: Context, track: Track): File {
        val dir = File(context.filesDir, "lyrics").apply { mkdirs() }
        val key = "${track.artist}-${track.title}-${track.durationMs / 1000}"
            .lowercase().map { if (it.isLetterOrDigit()) it else '_' }.joinToString("").take(120)
        return File(dir, "$key.txt")
    }

    private fun cacheRead(context: Context, track: Track): Lyrics? = runCatching {
        val f = cacheFile(context, track)
        if (!f.exists()) return@runCatching null
        val text = f.readText()
        val nl = text.indexOf('\n')
        val tag = if (nl >= 0) text.substring(0, nl) else text
        val body = if (nl >= 0) text.substring(nl + 1) else ""
        when (tag) {
            "SYNC" -> Lyrics.Synced(parseLrc(body))
            "PLAIN" -> Lyrics.Plain(body)
            "NONE" -> Lyrics.None
            else -> null
        }
    }.getOrNull()

    private fun cacheWrite(context: Context, track: Track, lyrics: Lyrics) {
        runCatching {
            val (tag, body) = when (lyrics) {
                is Lyrics.Synced -> "SYNC" to lyrics.lines.joinToString("\n") { "[${lrcStamp(it.timeMs)}]${it.text}" }
                is Lyrics.Plain -> "PLAIN" to lyrics.text
                Lyrics.None -> "NONE" to ""
            }
            cacheFile(context, track).writeText("$tag\n$body")
        }
    }

    private fun lrcStamp(ms: Long): String {
        val cs = ms / 10
        return "%02d:%02d.%02d".format(cs / 6000, (cs / 100) % 60, cs % 100)
    }
}
