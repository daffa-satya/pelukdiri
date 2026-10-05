package com.makhp.pelukdiri.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppIconCache {
    private val cache = android.util.LruCache<String, Drawable>(64)

    fun get(packageName: String): Drawable? = cache.get(packageName)

    fun put(packageName: String, drawable: Drawable) {
        cache.put(packageName, drawable)
    }
}

@Composable
fun AppIcon(
    packageName: String,
    appName: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp
) {
    val context = LocalContext.current
    
    // Load drawable in background with in-memory caching to avoid UI thread blocking & IPC overhead
    val iconResult by produceState<Drawable?>(initialValue = AppIconCache.get(packageName), packageName) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                try {
                    val drawable = context.packageManager.getApplicationIcon(packageName)
                    AppIconCache.put(packageName, drawable)
                    drawable
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    if (iconResult != null) {
        AsyncImage(
            model = iconResult,
            contentDescription = "Icon for $appName",
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Fit
        )
    } else {
        // Fallback to initial
        val initial = appName.firstOrNull()?.uppercase() ?: "?"
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
