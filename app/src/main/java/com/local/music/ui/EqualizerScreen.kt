package com.local.music.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.local.music.playback.AudioEffects

private fun freqLabel(hz: Int): String =
    if (hz >= 1000) {
        val k = hz / 1000f
        if (k % 1f == 0f) "${k.toInt()} kHz" else "%.1f kHz".format(k)
    } else "$hz Hz"

@Composable
fun EqualizerScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Text("Equalizer", style = MaterialTheme.typography.titleLarge)
            }

            if (AudioEffects.bandCount == 0) {
                Text(
                    "Start playing a song to enable the equalizer.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp)
                )
                return@Column
            }

            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Equalizer", Modifier.weight(1f))
                Switch(checked = AudioEffects.enabled, onCheckedChange = { AudioEffects.setEnabled(context, it) })
            }

            val min = AudioEffects.minLevel.toFloat()
            val max = AudioEffects.maxLevel.toFloat()
            for (i in 0 until AudioEffects.bandCount) {
                val freq = AudioEffects.centerFreqs.getOrElse(i) { 0 }
                val level = AudioEffects.levels.getOrElse(i) { 0 }
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(freqLabel(freq), Modifier.width(72.dp), style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = level.toFloat().coerceIn(min, max),
                        onValueChange = { AudioEffects.setBand(context, i, it.toInt()) },
                        valueRange = min..max,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.width(8.dp))
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Loudness boost")
                    Text(
                        "Lifts quiet tracks (+6 dB).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = AudioEffects.loudnessEnabled, onCheckedChange = { AudioEffects.setLoudness(context, it) })
            }
        }
    }
}
