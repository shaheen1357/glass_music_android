package com.local.music.data

import android.content.Context
import com.local.music.model.Playlist
import com.local.music.model.RecentSearch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object PlaylistStorage {
    private fun file(ctx: Context) = File(ctx.filesDir, "playlists.json")

    fun load(ctx: Context): List<Playlist> = runCatching {
        val f = file(ctx)
        if (!f.exists()) return@runCatching emptyList()
        val arr = JSONArray(f.readText())
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val idsArr = o.getJSONArray("trackIds")
            val ids = (0 until idsArr.length()).map { idsArr.getLong(it) }
            Playlist(o.getString("id"), o.getString("name"), ids)
        }
    }.getOrDefault(emptyList())

    fun save(ctx: Context, playlists: List<Playlist>) {
        runCatching {
            val arr = JSONArray()
            playlists.forEach { pl ->
                val o = JSONObject()
                o.put("id", pl.id)
                o.put("name", pl.name)
                val ids = JSONArray()
                pl.trackIds.forEach { ids.put(it) }
                o.put("trackIds", ids)
                arr.put(o)
            }
            file(ctx).writeText(arr.toString())
        }
    }
}

object RecentsStorage {
    private fun file(ctx: Context) = File(ctx.filesDir, "recents.json")

    fun load(ctx: Context): List<RecentSearch> = runCatching {
        val f = file(ctx)
        if (!f.exists()) return@runCatching emptyList()
        val arr = JSONArray(f.readText())
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            when (o.getString("type")) {
                "song" -> RecentSearch.Song(o.getLong("id"))
                "query" -> RecentSearch.Query(o.getString("text"))
                else -> null
            }
        }
    }.getOrDefault(emptyList())

    fun save(ctx: Context, recents: List<RecentSearch>) {
        runCatching {
            val arr = JSONArray()
            recents.forEach { r ->
                val o = JSONObject()
                when (r) {
                    is RecentSearch.Song -> { o.put("type", "song"); o.put("id", r.trackId) }
                    is RecentSearch.Query -> { o.put("type", "query"); o.put("text", r.text) }
                }
                arr.put(o)
            }
            file(ctx).writeText(arr.toString())
        }
    }
}
