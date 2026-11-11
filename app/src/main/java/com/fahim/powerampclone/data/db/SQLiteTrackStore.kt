package com.fahim.powerampclone.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.fahim.powerampclone.data.lyrics.LrcParser
import com.fahim.powerampclone.model.Track
import com.fahim.powerampclone.model.seedColorForString

/**
 * High-performance SQLite-backed implementation of [TrackStore].
 *
 * Uses transaction-safe batch operations, parameterized queries, and
 * fast indexed reads for instant cold-start library rendering.
 */
class SQLiteTrackStore(
    context: Context,
    private val dbHelper: SQLiteOpenHelper = MioraDatabaseHelper.getInstance(context),
) : TrackStore {

    companion object {
        private const val KEY_LAST_SCAN_TIMESTAMP = "last_scan_timestamp"
        private const val KEY_TOTAL_TRACKS = "total_tracks"
    }

    override fun getAllTracks(): List<Track> {
        val tracks = mutableListOf<Track>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM ${MioraDatabaseHelper.TABLE_TRACKS} ORDER BY title COLLATE NOCASE ASC",
            null,
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                tracks.add(cursorToTrack(c))
            }
        }
        return tracks
    }

    override fun getTrackById(id: Long): Track? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM ${MioraDatabaseHelper.TABLE_TRACKS} WHERE id = ? LIMIT 1",
            arrayOf(id.toString()),
        )
        cursor.use { c ->
            if (c.moveToFirst()) {
                return cursorToTrack(c)
            }
        }
        return null
    }

    override fun getTrackByPath(path: String): Track? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM ${MioraDatabaseHelper.TABLE_TRACKS} WHERE data_path = ? LIMIT 1",
            arrayOf(path),
        )
        cursor.use { c ->
            if (c.moveToFirst()) {
                return cursorToTrack(c)
            }
        }
        return null
    }

    override fun getTrackByUri(uri: String): Track? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM ${MioraDatabaseHelper.TABLE_TRACKS} WHERE uri = ? LIMIT 1",
            arrayOf(uri),
        )
        cursor.use { c ->
            if (c.moveToFirst()) {
                return cursorToTrack(c)
            }
        }
        return null
    }

    override fun getTrackByStableHash(hash: String): Track? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM ${MioraDatabaseHelper.TABLE_TRACKS} WHERE stable_hash = ? LIMIT 1",
            arrayOf(hash),
        )
        cursor.use { c ->
            if (c.moveToFirst()) {
                return cursorToTrack(c)
            }
        }
        return null
    }

    override fun upsertTracks(tracks: List<Track>): Int {
        if (tracks.isEmpty()) return 0
        val db = dbHelper.writableDatabase
        var affected = 0
        val now = System.currentTimeMillis()

        db.beginTransaction()
        try {
            for (track in tracks) {
                val stableHash = computeStableHash(track)
                val cv = ContentValues().apply {
                    put("id", track.id)
                    put("media_store_id", track.id)
                    put("stable_hash", stableHash)
                    put("uri", track.contentUriString)
                    put("data_path", track.folderPath)
                    put("title", track.title)
                    put("artist", track.artist)
                    put("album", track.album)
                    put("album_id", track.albumId)
                    put("artist_id", track.artistId)
                    put("duration_ms", track.durationMs)
                    put("track_number", track.trackNumber)
                    put("year", track.year)
                    put("genre", track.genre)
                    put("folder_path", track.folderPath)
                    put("mime_type", "")
                    put("format", track.format)
                    put("bitrate_kbps", track.bitrateKbps)
                    put("sample_rate_hz", track.sampleRateHz)
                    put("bit_depth", track.bitDepth)
                    put("size_bytes", 0L)
                    put("date_modified", now / 1000)
                    put("artwork_uri", track.artworkUriString)
                    if (track.lyrics.isNotEmpty()) {
                        put("lyrics_raw", LrcParser.formatLrc(track.lyrics))
                    } else {
                        putNull("lyrics_raw")
                    }
                    put("last_scanned_at", now)
                }

                val rowId = db.insertWithOnConflict(
                    MioraDatabaseHelper.TABLE_TRACKS,
                    null,
                    cv,
                    SQLiteDatabase.CONFLICT_REPLACE,
                )
                if (rowId != -1L) affected++
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        // Update cached track count in metadata
        setMetadata(KEY_TOTAL_TRACKS, getTrackCount().toString())
        return affected
    }

    override fun deleteTracks(trackIds: Collection<Long>): Int {
        if (trackIds.isEmpty()) return 0
        val db = dbHelper.writableDatabase
        var deletedCount = 0
        db.beginTransaction()
        try {
            for (chunk in trackIds.chunked(500)) {
                val placeholders = chunk.joinToString(",") { "?" }
                val args = chunk.map { it.toString() }.toTypedArray()
                deletedCount += db.delete(
                    MioraDatabaseHelper.TABLE_TRACKS,
                    "id IN ($placeholders)",
                    args,
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        setMetadata(KEY_TOTAL_TRACKS, getTrackCount().toString())
        return deletedCount
    }

    override fun deleteTracksNotIn(activeIds: Collection<Long>): Int {
        val db = dbHelper.writableDatabase
        if (activeIds.isEmpty()) {
            val count = db.delete(MioraDatabaseHelper.TABLE_TRACKS, null, null)
            setMetadata(KEY_TOTAL_TRACKS, "0")
            return count
        }

        // Fetch existing IDs to compare in memory to avoid huge SQL statements
        val existingIds = mutableSetOf<Long>()
        db.rawQuery("SELECT id FROM ${MioraDatabaseHelper.TABLE_TRACKS}", null).use { c ->
            while (c.moveToNext()) {
                existingIds.add(c.getLong(0))
            }
        }

        val staleIds = existingIds - activeIds.toSet()
        return if (staleIds.isNotEmpty()) {
            deleteTracks(staleIds)
        } else {
            0
        }
    }

    override fun searchTracks(query: String, limit: Int): List<Track> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()

        val db = dbHelper.readableDatabase
        val wildcard = "%$q%"
        val cursor = db.rawQuery(
            """
            SELECT * FROM ${MioraDatabaseHelper.TABLE_TRACKS}
            WHERE title LIKE ? OR artist LIKE ? OR album LIKE ? OR folder_path LIKE ?
            ORDER BY title COLLATE NOCASE ASC
            LIMIT ?
            """.trimIndent(),
            arrayOf(wildcard, wildcard, wildcard, wildcard, limit.toString()),
        )

        val results = mutableListOf<Track>()
        cursor.use { c ->
            while (c.moveToNext()) {
                results.add(cursorToTrack(c))
            }
        }
        return results
    }

    override fun getTrackCount(): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM ${MioraDatabaseHelper.TABLE_TRACKS}", null)
        cursor.use { c ->
            if (c.moveToFirst()) {
                return c.getInt(0)
            }
        }
        return 0
    }

    override fun getLastScanTimestamp(): Long {
        return getMetadata(KEY_LAST_SCAN_TIMESTAMP)?.toLongOrNull() ?: 0L
    }

    override fun setLastScanTimestamp(timestampMs: Long) {
        setMetadata(KEY_LAST_SCAN_TIMESTAMP, timestampMs.toString())
    }

    override fun clearAllTracks() {
        val db = dbHelper.writableDatabase
        db.delete(MioraDatabaseHelper.TABLE_TRACKS, null, null)
        setMetadata(KEY_TOTAL_TRACKS, "0")
    }

    private fun getMetadata(key: String): String? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT value FROM ${MioraDatabaseHelper.TABLE_SCAN_METADATA} WHERE key = ? LIMIT 1",
            arrayOf(key),
        )
        cursor.use { c ->
            if (c.moveToFirst()) {
                return c.getString(0)
            }
        }
        return null
    }

    private fun setMetadata(key: String, value: String) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("key", key)
            put("value", value)
        }
        db.insertWithOnConflict(
            MioraDatabaseHelper.TABLE_SCAN_METADATA,
            null,
            cv,
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    private fun computeStableHash(track: Track): String {
        return "${track.contentUriString}|${track.folderPath}|${track.title}|${track.album}|${track.durationMs}".hashCode().toString()
    }

    private fun cursorToTrack(c: Cursor): Track {
        val id = c.getLong(c.getColumnIndexOrThrow("id"))
        val title = c.getString(c.getColumnIndexOrThrow("title")) ?: "Unknown Track"
        val artist = c.getString(c.getColumnIndexOrThrow("artist")) ?: "Unknown Artist"
        val album = c.getString(c.getColumnIndexOrThrow("album")) ?: "Unknown Album"
        val albumId = c.getLong(c.getColumnIndexOrThrow("album_id"))
        val artistId = c.getLong(c.getColumnIndexOrThrow("artist_id"))
        val durationMs = c.getLong(c.getColumnIndexOrThrow("duration_ms"))
        val trackNumber = c.getInt(c.getColumnIndexOrThrow("track_number"))
        val year = c.getInt(c.getColumnIndexOrThrow("year"))
        val genre = c.getString(c.getColumnIndexOrThrow("genre")) ?: "Music"
        val folderPath = c.getString(c.getColumnIndexOrThrow("folder_path")) ?: "/Music"
        val format = c.getString(c.getColumnIndexOrThrow("format")) ?: "AUDIO"
        val bitrateKbps = c.getInt(c.getColumnIndexOrThrow("bitrate_kbps"))
        val sampleRateHz = c.getInt(c.getColumnIndexOrThrow("sample_rate_hz"))
        val bitDepth = c.getInt(c.getColumnIndexOrThrow("bit_depth"))
        val uri = c.getString(c.getColumnIndexOrThrow("uri")) ?: ""
        val artworkUri = c.getString(c.getColumnIndexOrThrow("artwork_uri"))
        val lyricsRaw = c.getString(c.getColumnIndexOrThrow("lyrics_raw"))

        val seedKey = if (album != "Unknown Album") album else "$title:$artist:$id"
        val artworkSeed = seedColorForString(seedKey)
        val lyrics = if (!lyricsRaw.isNullOrBlank()) LrcParser.parseLrc(lyricsRaw) else emptyList()

        return Track(
            id = id,
            title = title,
            artist = artist,
            album = album,
            albumId = albumId,
            artistId = artistId,
            durationMs = durationMs,
            trackNumber = trackNumber,
            year = year,
            genre = genre,
            folderPath = folderPath,
            isFavorite = false,
            artworkSeed = artworkSeed,
            format = format,
            bitrateKbps = bitrateKbps,
            sampleRateHz = sampleRateHz,
            bitDepth = bitDepth,
            lyrics = lyrics,
            contentUriString = uri,
            artworkUriString = artworkUri,
        )
    }
}
