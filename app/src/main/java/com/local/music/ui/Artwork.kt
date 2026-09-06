package com.local.music.ui

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val artCache = object : LruCache<String, Bitmap>(48) {}

@Composable
fun Artwork(uri: Uri?, modifier: Modifier = Modifier, corner: Dp = 6.dp) {
    val context = LocalContext.current
    val key = uri?.toString()
    var bitmap by remember(key) { mutableStateOf(key?.let { artCache.get(it) }) }

    LaunchedEffect(key) {
        if (uri != null && key != null && bitmap == null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        ) {
            val bmp = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.loadThumbnail(uri, Size(256, 256), null) }.getOrNull()
            }
            if (bmp != null) {
                artCache.put(key, bmp)
                bitmap = bmp
            }
        }
    }

    Box(
        modifier
            .clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
