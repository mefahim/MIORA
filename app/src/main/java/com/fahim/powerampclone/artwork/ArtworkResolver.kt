package com.fahim.powerampclone.artwork

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import com.fahim.powerampclone.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.max

/**
 * Thread-safe LRU in-memory bitmap cache portable across Android and JVM test environments.
 */
class LruBitmapCache(private val maxEntries: Int = 120) {
    private val map = object : LinkedHashMap<String, Bitmap>(maxEntries, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean {
            return size > maxEntries
        }
    }

    @Synchronized
    fun get(key: String): Bitmap? = map[key]

    @Synchronized
    fun put(key: String, bitmap: Bitmap) {
        map[key] = bitmap
    }

    @Synchronized
    fun clear() {
        map.clear()
    }

    @Synchronized
    fun remove(key: String) {
        map.remove(key)
    }

    @Synchronized
    fun size(): Int = map.size
}

/**
 * Unified artwork resolution pipeline.
 *
 * Priority:
 * 1. Valid embedded track artwork (via MediaMetadataRetriever embeddedPicture)
 * 2. Valid MediaStore album / track content thumbnail (ContentResolver.loadThumbnail)
 * 3. MediaStore audio album art URI stream
 * 4. In-memory cache lookup
 * 5. Returns null when no artwork exists, allowing caller to render procedural fallback.
 */
object ArtworkResolver {

    private val cache = LruBitmapCache(maxEntries = 120)
    private val albumArtBaseUri by lazy { Uri.parse("content://media/external/audio/albumart") }

    fun getCached(key: String): Bitmap? = cache.get(key)

    fun putInCache(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }

    fun clearCache() {
        cache.clear()
    }

    /**
     * Resolves artwork for a track or album synchronously/asynchronously.
     */
    fun resolveArtwork(
        context: Context,
        track: Track?,
        albumId: Long? = null,
        targetSize: Int = 512,
    ): Bitmap? {
        if (track == null && albumId == null) return null

        val cacheKey = when {
            track != null && track.id > 0L -> "track_${track.id}"
            albumId != null && albumId > 0L -> "album_$albumId"
            track != null && track.albumId > 0L -> "album_${track.albumId}"
            else -> null
        }

        if (cacheKey != null) {
            val cached = cache.get(cacheKey)
            if (cached != null && !cached.isRecycled) {
                return cached
            }
        }

        // 1. Try resolving embedded ID3 / APIC picture directly from contentUri or file
        if (track != null) {
            val embeddedBitmap = extractEmbeddedArtwork(context, track, targetSize)
            if (embeddedBitmap != null) {
                if (cacheKey != null) cache.put(cacheKey, embeddedBitmap)
                return embeddedBitmap
            }
        }

        // 2. Try ContentResolver loadThumbnail on track contentUri
        if (track != null && track.contentUriString.isNotBlank() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val uri = Uri.parse(track.contentUriString)
                val thumb = context.contentResolver.loadThumbnail(uri, Size(targetSize, targetSize), null)
                if (cacheKey != null) cache.put(cacheKey, thumb)
                return thumb
            } catch (_: Throwable) {}
        }

        // 3. Try track.artworkUriString if provided by MediaStore scanner
        if (track != null && !track.artworkUriString.isNullOrBlank()) {
            val decoded = decodeUriBitmap(context.contentResolver, Uri.parse(track.artworkUriString), targetSize)
            if (decoded != null) {
                if (cacheKey != null) cache.put(cacheKey, decoded)
                return decoded
            }
        }

        // 4. Try MediaStore albumart URI by albumId
        val effectiveAlbumId = albumId ?: track?.albumId ?: 0L
        if (effectiveAlbumId > 0L) {
            val albumUri = ContentUris.withAppendedId(albumArtBaseUri, effectiveAlbumId)
            val decoded = decodeUriBitmap(context.contentResolver, albumUri, targetSize)
            if (decoded != null) {
                if (cacheKey != null) cache.put(cacheKey, decoded)
                return decoded
            }
        }

        return null
    }

    private fun extractEmbeddedArtwork(context: Context, track: Track, targetSize: Int): Bitmap? {
        if (track.contentUriString.isBlank() && track.folderPath.isBlank()) return null
        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            if (track.contentUriString.isNotBlank()) {
                retriever.setDataSource(context, Uri.parse(track.contentUriString))
            } else {
                retriever.setDataSource(track.folderPath)
            }
            val pictureBytes = retriever.embeddedPicture
            if (pictureBytes != null && pictureBytes.isNotEmpty()) {
                return decodeByteArrayScaled(pictureBytes, targetSize)
            }
        } catch (_: Throwable) {
            // Ignore format / permission / io failures gracefully
        } finally {
            try {
                retriever?.release()
            } catch (_: Throwable) {}
        }
        return null
    }

    private fun decodeUriBitmap(resolver: ContentResolver, uri: Uri, targetSize: Int): Bitmap? {
        var input: InputStream? = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    return resolver.loadThumbnail(uri, Size(targetSize, targetSize), null)
                } catch (_: Throwable) {}
            }
            input = resolver.openInputStream(uri) ?: return null
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, boundsOptions)
            input.close()

            val sampleSize = calculateInSampleSize(boundsOptions, targetSize, targetSize)
            input = resolver.openInputStream(uri) ?: return null
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            return BitmapFactory.decodeStream(input, null, decodeOptions)
        } catch (_: Throwable) {
            return null
        } finally {
            try {
                input?.close()
            } catch (_: Throwable) {}
        }
    }

    private fun decodeByteArrayScaled(data: ByteArray, targetSize: Int): Bitmap? {
        try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(data, 0, data.size, boundsOptions)

            val sampleSize = calculateInSampleSize(boundsOptions, targetSize, targetSize)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            return BitmapFactory.decodeByteArray(data, 0, data.size, decodeOptions)
        } catch (_: Throwable) {
            return null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return max(1, inSampleSize)
    }
}

/**
 * Reactive Compose helper that retrieves artwork safely on Dispatchers.IO.
 * Returns cached bitmap immediately to avoid any flickering.
 */
@Composable
fun rememberArtworkBitmap(
    track: Track?,
    albumId: Long? = null,
    targetSize: Int = 512,
): Bitmap? {
    val context = LocalContext.current
    val key = when {
        track != null && track.id > 0L -> "track_${track.id}"
        albumId != null && albumId > 0L -> "album_$albumId"
        track != null && track.albumId > 0L -> "album_${track.albumId}"
        else -> null
    }

    return produceState<Bitmap?>(
        initialValue = key?.let { ArtworkResolver.getCached(it) },
        key1 = key,
    ) {
        if (key == null) {
            value = null
            return@produceState
        }
        val cached = ArtworkResolver.getCached(key)
        if (cached != null && !cached.isRecycled) {
            value = cached
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            ArtworkResolver.resolveArtwork(context, track, albumId, targetSize)
        }
    }.value
}
