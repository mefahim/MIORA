package com.fahim.powerampclone.data.scanner

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.fahim.powerampclone.data.db.MioraDatabaseHelper
import com.fahim.powerampclone.data.db.SQLiteTrackStore
import com.fahim.powerampclone.data.db.TrackStore
import com.fahim.powerampclone.data.local.PermissionHelper
import com.fahim.powerampclone.data.lyrics.LrcParser
import com.fahim.powerampclone.model.LyricLine
import com.fahim.powerampclone.model.Track
import com.fahim.powerampclone.model.seedColorForString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Scan progress notification state.
 */
data class ScanProgress(
    val isScanning: Boolean = false,
    val stage: String = "Idle",
    val scannedCount: Int = 0,
    val totalCount: Int = 0,
    val addedCount: Int = 0,
    val updatedCount: Int = 0,
    val deletedCount: Int = 0,
)

/**
 * Scan completion summary.
 */
data class ScanResult(
    val totalScanned: Int,
    val addedCount: Int,
    val updatedCount: Int,
    val deletedCount: Int,
    val durationMs: Long,
    val tracks: List<Track>,
)

/**
 * Advanced high-performance MediaStore audio scanner and incremental ingestion engine.
 *
 * Capabilities:
 * 1. Initial full-library ingestion with atomic database transaction caching.
 * 2. High-speed incremental scanning based on MediaStore.DATE_MODIFIED threshold.
 * 3. Move/rename/deletion detection with stale track cleanup.
 * 4. Automatic companion .lrc lyrics discovery and parsing.
 * 5. Extraction of high-res format indicators (FLAC, MP3, M4A, OGG, OPUS, WAV, DSD).
 * 6. Observable ScanProgress StateFlow for UI binding and notifications.
 */
class AdvancedMediaScanner(
    private val trackStore: TrackStore,
) {

    constructor(context: Context) : this(
        SQLiteTrackStore(context, MioraDatabaseHelper.getInstance(context)),
    )

    private val _scanProgress = MutableStateFlow(ScanProgress())
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    companion object {
        private val ALBUM_ART_URI = Uri.parse("content://media/external/audio/albumart")

        private val PROJECTION = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.SIZE,
        )

        private const val BASE_SELECTION =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 5000"
    }

    /**
     * Executes a complete storage scan, replacing stale database records and recording last scan timestamp.
     */
    suspend fun scanFull(context: Context): ScanResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        if (!PermissionHelper.hasMediaPermission(context)) {
            _scanProgress.value = ScanProgress(isScanning = false, stage = "Permission denied")
            return@withContext ScanResult(0, 0, 0, 0, 0L, emptyList())
        }

        _scanProgress.value = ScanProgress(isScanning = true, stage = "Querying MediaStore...")

        val scannedTracks = queryMediaStore(context, BASE_SELECTION, null)
        val scannedIds = scannedTracks.map { it.id }.toSet()

        _scanProgress.value = ScanProgress(
            isScanning = true,
            stage = "Saving tracks to database...",
            scannedCount = scannedTracks.size,
            totalCount = scannedTracks.size,
        )

        // 1. Batch upsert scanned tracks
        val upserted = trackStore.upsertTracks(scannedTracks)

        // 2. Prune records that no longer exist on device
        val deleted = trackStore.deleteTracksNotIn(scannedIds)

        // 3. Update scan timestamp
        val finishTime = System.currentTimeMillis()
        trackStore.setLastScanTimestamp(finishTime)

        val allTracks = trackStore.getAllTracks()

        _scanProgress.value = ScanProgress(
            isScanning = false,
            stage = "Scan Complete",
            scannedCount = scannedTracks.size,
            totalCount = scannedTracks.size,
            addedCount = upserted,
            deletedCount = deleted,
        )

        return@withContext ScanResult(
            totalScanned = scannedTracks.size,
            addedCount = upserted,
            updatedCount = 0,
            deletedCount = deleted,
            durationMs = finishTime - startTime,
            tracks = allTracks,
        )
    }

    /**
     * Executes an incremental scan detecting only files modified or added since the last scan timestamp.
     */
    suspend fun scanIncremental(context: Context): ScanResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        if (!PermissionHelper.hasMediaPermission(context)) {
            return@withContext ScanResult(0, 0, 0, 0, 0L, trackStore.getAllTracks())
        }

        val lastScanMs = trackStore.getLastScanTimestamp()
        if (lastScanMs <= 0L || trackStore.getTrackCount() == 0) {
            return@withContext scanFull(context)
        }

        val lastScanSec = lastScanMs / 1000
        val incrementalSelection = "$BASE_SELECTION AND ${MediaStore.Audio.Media.DATE_MODIFIED} > ?"
        val selectionArgs = arrayOf(lastScanSec.toString())

        _scanProgress.value = ScanProgress(isScanning = true, stage = "Checking for new media...")

        val newOrModified = queryMediaStore(context, incrementalSelection, selectionArgs)

        var addedCount = 0
        var updatedCount = 0
        if (newOrModified.isNotEmpty()) {
            for (track in newOrModified) {
                val existing = trackStore.getTrackById(track.id)
                if (existing == null) addedCount++ else updatedCount++
            }
            trackStore.upsertTracks(newOrModified)
        }

        // Check for deleted tracks in cache
        val deletedCount = purgeMissingTracks(context)

        val finishTime = System.currentTimeMillis()
        trackStore.setLastScanTimestamp(finishTime)

        val currentTracks = trackStore.getAllTracks()

        _scanProgress.value = ScanProgress(
            isScanning = false,
            stage = "Incremental Scan Complete",
            scannedCount = newOrModified.size,
            totalCount = currentTracks.size,
            addedCount = addedCount,
            updatedCount = updatedCount,
            deletedCount = deletedCount,
        )

        return@withContext ScanResult(
            totalScanned = newOrModified.size,
            addedCount = addedCount,
            updatedCount = updatedCount,
            deletedCount = deletedCount,
            durationMs = finishTime - startTime,
            tracks = currentTracks,
        )
    }

    /**
     * Queries MediaStore with given criteria and builds Track models with lyrics and metadata.
     */
    private fun queryMediaStore(
        context: Context,
        selection: String,
        selectionArgs: Array<String>?,
    ): List<Track> {
        val tracks = mutableListOf<Track>()
        val seenIds = HashSet<Long>()
        val seenPaths = HashSet<String>()

        try {
            val cursor: Cursor? = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                PROJECTION,
                selection,
                selectionArgs,
                "${MediaStore.Audio.Media.TITLE} ASC",
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val artistIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val trackCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val mimeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    if (id in seenIds) continue

                    val data = c.getString(dataCol) ?: ""
                    val canonicalPath = try {
                        if (data.isNotBlank()) File(data).canonicalPath else ""
                    } catch (_: Exception) {
                        data
                    }

                    if (canonicalPath.isNotEmpty() && canonicalPath in seenPaths) {
                        continue
                    }

                    val displayName = c.getString(nameCol) ?: ""
                    var rawTitle = c.getString(titleCol)?.trim() ?: ""
                    var rawArtist = c.getString(artistCol)?.trim() ?: ""
                    var rawAlbum = c.getString(albumCol)?.trim() ?: ""

                    if (rawArtist.equals("<unknown>", ignoreCase = true) || rawArtist.equals("unknown", ignoreCase = true)) {
                        rawArtist = ""
                    }
                    if (rawAlbum.equals("<unknown>", ignoreCase = true) || rawAlbum.equals("unknown", ignoreCase = true)) {
                        rawAlbum = ""
                    }

                    if (rawTitle.isBlank() || rawTitle.equals("<unknown>", ignoreCase = true)) {
                        rawTitle = if (displayName.isNotBlank()) displayName.substringBeforeLast('.') else "Unknown Track"
                    }

                    // Attempt clean "Artist - Title" extraction if artist is missing but title has separator
                    if (rawArtist.isBlank() && rawTitle.contains(" - ")) {
                        val parts = rawTitle.split(" - ", limit = 2)
                        rawArtist = parts[0].trim()
                        rawTitle = parts[1].trim()
                    }

                    val title = rawTitle.ifBlank { "Unknown Track" }
                    val artist = rawArtist.ifBlank { "Unknown Artist" }
                    val album = rawAlbum.ifBlank { "Unknown Album" }

                    val albumId = c.getLong(albumIdCol)
                    val artistId = c.getLong(artistIdCol)
                    val durationMs = c.getLong(durationCol)
                    val trackNumber = c.getInt(trackCol).let { if (it >= 1000) it % 1000 else it }
                    val year = c.getInt(yearCol)
                    val mimeType = c.getString(mimeCol) ?: ""

                    val folderPath = try {
                        if (data.isNotBlank()) {
                            val file = File(data)
                            file.parent ?: "/Music"
                        } else {
                            "/Music"
                        }
                    } catch (_: Exception) {
                        "/Music"
                    }

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id,
                    ).toString()

                    val artworkUri = if (albumId > 0) {
                        ContentUris.withAppendedId(ALBUM_ART_URI, albumId).toString()
                    } else null

                    val format = parseFormat(data, mimeType)

                    // Distinct artwork seed so different unknown album tracks don't all look identical
                    val seedKey = if (album != "Unknown Album") album else "$title:$artist:$id"
                    val artworkSeed = seedColorForString(seedKey)

                    // Companion lyrics discovery
                    val lyrics = loadCompanionLyrics(data)

                    seenIds.add(id)
                    if (canonicalPath.isNotEmpty()) seenPaths.add(canonicalPath)

                    tracks.add(
                        Track(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            albumId = albumId,
                            artistId = artistId,
                            durationMs = durationMs,
                            trackNumber = trackNumber,
                            year = if (year > 0) year else 2024,
                            genre = "Music",
                            folderPath = folderPath,
                            isFavorite = false,
                            artworkSeed = artworkSeed,
                            format = format,
                            lyrics = lyrics,
                            contentUriString = contentUri,
                            artworkUriString = artworkUri,
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            // Handled gracefully
        }

        return tracks
    }

    private fun loadCompanionLyrics(audioPath: String): List<LyricLine> {
        if (audioPath.isBlank()) return emptyList()
        return try {
            val siblingLrc = LrcParser.findSiblingLrcFile(audioPath)
            if (siblingLrc != null && siblingLrc.exists() && siblingLrc.canRead()) {
                val text = siblingLrc.readText(Charsets.UTF_8)
                LrcParser.parseLrc(text)
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun purgeMissingTracks(context: Context): Int {
        val cached = trackStore.getAllTracks()
        if (cached.isEmpty()) return 0

        val missingIds = mutableListOf<Long>()
        val projection = arrayOf(MediaStore.Audio.Media._ID)

        for (chunk in cached.chunked(300)) {
            val ids = chunk.map { it.id }
            val placeholders = ids.joinToString(",") { "?" }
            val args = ids.map { it.toString() }.toTypedArray()

            val liveIds = mutableSetOf<Long>()
            try {
                context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    "${MediaStore.Audio.Media._ID} IN ($placeholders)",
                    args,
                    null,
                )?.use { c ->
                    val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    while (c.moveToNext()) {
                        liveIds.add(c.getLong(idCol))
                    }
                }
            } catch (_: Exception) {
                // If contentResolver query fails, fallback to file existence check
                for (t in chunk) {
                    if (t.folderPath.isNotBlank() && !File(t.folderPath).exists()) {
                        missingIds.add(t.id)
                    }
                }
                continue
            }

            for (track in chunk) {
                if (track.id !in liveIds) {
                    missingIds.add(track.id)
                }
            }
        }

        return if (missingIds.isNotEmpty()) {
            trackStore.deleteTracks(missingIds)
        } else {
            0
        }
    }

    private fun parseFormat(filePath: String, mimeType: String): String {
        val ext = filePath.substringAfterLast('.', "").uppercase()
        return when {
            ext in listOf("FLAC", "MP3", "M4A", "AAC", "OGG", "OPUS", "WAV", "DSD", "DSF", "APE", "ALAC") -> ext
            mimeType.contains("flac", ignoreCase = true) -> "FLAC"
            mimeType.contains("mp3", ignoreCase = true) || mimeType.contains("mpeg", ignoreCase = true) -> "MP3"
            mimeType.contains("ogg", ignoreCase = true) -> "OGG"
            mimeType.contains("opus", ignoreCase = true) -> "OPUS"
            mimeType.contains("wav", ignoreCase = true) -> "WAV"
            mimeType.contains("aac", ignoreCase = true) || mimeType.contains("mp4", ignoreCase = true) -> "M4A"
            else -> "AUDIO"
        }
    }
}
