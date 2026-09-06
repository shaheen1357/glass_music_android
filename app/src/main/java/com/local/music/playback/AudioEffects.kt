package com.local.music.playback

import android.content.Context
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/// Process-wide holder for the system Equalizer + LoudnessEnhancer bound to the
/// ExoPlayer's audio session. The service attaches it when the session id is
/// known; the EQ screen reads/writes it. Compose observes the mutableState vars.
object AudioEffects {
    private const val PREFS = "audiofx"

    private var eq: Equalizer? = null
    private var loud: LoudnessEnhancer? = null
    private var currentSession = 0

    var enabled by mutableStateOf(false); private set
    var bandCount by mutableStateOf(0); private set
    var minLevel by mutableStateOf(0); private set        // millibels
    var maxLevel by mutableStateOf(0); private set        // millibels
    var levels by mutableStateOf<List<Int>>(emptyList()); private set   // per-band millibels
    var centerFreqs by mutableStateOf<List<Int>>(emptyList()); private set // Hz
    var loudnessEnabled by mutableStateOf(false); private set

    fun attach(context: Context, sessionId: Int) {
        if (sessionId == 0) return
        if (sessionId == currentSession && eq != null) return
        release()
        currentSession = sessionId
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        runCatching {
            val e = Equalizer(0, sessionId)
            val n = e.numberOfBands.toInt()
            val range = e.bandLevelRange
            bandCount = n
            minLevel = range[0].toInt()
            maxLevel = range[1].toInt()
            centerFreqs = (0 until n).map { e.getCenterFreq(it.toShort()) / 1000 }
            val saved = (0 until n).map { prefs.getInt("band_$it", 0) }
            enabled = prefs.getBoolean("eq_enabled", false)
            saved.forEachIndexed { i, lv -> e.setBandLevel(i.toShort(), lv.toShort()) }
            e.setEnabled(enabled)
            levels = saved
            eq = e
        }
        runCatching {
            val l = LoudnessEnhancer(sessionId)
            loudnessEnabled = prefs.getBoolean("loud_enabled", false)
            l.setTargetGain(if (loudnessEnabled) 600 else 0)   // +6 dB
            l.setEnabled(true)
            loud = l
        }
    }

    fun setBand(context: Context, band: Int, level: Int) {
        val e = eq ?: return
        runCatching { e.setBandLevel(band.toShort(), level.toShort()) }
        levels = levels.toMutableList().also { if (band in it.indices) it[band] = level }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("band_$band", level).apply()
    }

    fun setEnabled(context: Context, on: Boolean) {
        enabled = on
        runCatching { eq?.setEnabled(on) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("eq_enabled", on).apply()
    }

    fun setLoudness(context: Context, on: Boolean) {
        loudnessEnabled = on
        runCatching { loud?.setTargetGain(if (on) 600 else 0) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("loud_enabled", on).apply()
    }

    private fun release() {
        runCatching { eq?.release() }
        runCatching { loud?.release() }
        eq = null
        loud = null
    }
}
