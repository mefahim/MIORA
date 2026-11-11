package com.fahim.powerampclone.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Unified SQLiteOpenHelper managing miora_playlists.db.
 *
 * Schema history:
 * - Version 1: playlists, playlist_tracks, favorites, playback_history
 * - Version 2: adds persistent tracks cache table, indexes, and scan_metadata table.
 *
 * All migrations are non-destructive and preserve 100% of existing user data.
 */
class MioraDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DB_NAME,
    null,
    DB_VERSION,
) {

    companion object {
        const val DB_NAME = "miora_playlists.db"
        const val DB_VERSION = 2

        // Table names
        const val TABLE_PLAYLISTS = "playlists"
        const val TABLE_PLAYLIST_TRACKS = "playlist_tracks"
        const val TABLE_FAVORITES = "favorites"
        const val TABLE_HISTORY = "playback_history"
        const val TABLE_TRACKS = "tracks"
        const val TABLE_SCAN_METADATA = "scan_metadata"

        @Volatile
        private var instance: MioraDatabaseHelper? = null

        fun getInstance(context: Context): MioraDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: MioraDatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        try {
            db.setForeignKeyConstraintsEnabled(true)
        } catch (_: Exception) {
            db.execSQL("PRAGMA foreign_keys = ON;")
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // --- Version 1 Tables ---
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_PLAYLISTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                artwork_seed INTEGER NOT NULL,
                created_at INTEGER NOT NULL
            );
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_PLAYLIST_TRACKS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                playlist_id INTEGER NOT NULL,
                track_id INTEGER NOT NULL,
                position INTEGER NOT NULL,
                FOREIGN KEY(playlist_id) REFERENCES $TABLE_PLAYLISTS(id) ON DELETE CASCADE
            );
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_FAVORITES (
                track_id INTEGER PRIMARY KEY,
                created_at INTEGER NOT NULL
            );
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_HISTORY (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                track_id INTEGER NOT NULL,
                played_at INTEGER NOT NULL
            );
            """.trimIndent(),
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_playlist_tracks ON $TABLE_PLAYLIST_TRACKS(playlist_id, position);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_history ON $TABLE_HISTORY(played_at DESC);")

        // Seed default initial user playlists
        seedInitialPlaylists(db)

        // --- Version 2 Tables & Indexes ---
        createVersion2Tables(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            createVersion2Tables(db)
        }
    }

    private fun createVersion2Tables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_TRACKS (
                id INTEGER PRIMARY KEY,
                media_store_id INTEGER NOT NULL,
                stable_hash TEXT NOT NULL UNIQUE,
                uri TEXT NOT NULL,
                data_path TEXT NOT NULL,
                title TEXT NOT NULL,
                artist TEXT NOT NULL,
                album TEXT NOT NULL,
                album_id INTEGER NOT NULL DEFAULT 0,
                artist_id INTEGER NOT NULL DEFAULT 0,
                duration_ms INTEGER NOT NULL DEFAULT 0,
                track_number INTEGER NOT NULL DEFAULT 1,
                year INTEGER NOT NULL DEFAULT 2024,
                genre TEXT NOT NULL DEFAULT 'Music',
                folder_path TEXT NOT NULL DEFAULT '/Music',
                mime_type TEXT NOT NULL DEFAULT '',
                format TEXT NOT NULL DEFAULT 'AUDIO',
                bitrate_kbps INTEGER NOT NULL DEFAULT 0,
                sample_rate_hz INTEGER NOT NULL DEFAULT 44100,
                bit_depth INTEGER NOT NULL DEFAULT 16,
                size_bytes INTEGER NOT NULL DEFAULT 0,
                date_modified INTEGER NOT NULL DEFAULT 0,
                artwork_uri TEXT,
                lyrics_raw TEXT,
                last_scanned_at INTEGER NOT NULL
            );
            """.trimIndent(),
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_tracks_path ON $TABLE_TRACKS(data_path);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_tracks_artist ON $TABLE_TRACKS(artist);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_tracks_album ON $TABLE_TRACKS(album);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_tracks_date_modified ON $TABLE_TRACKS(date_modified);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_tracks_folder ON $TABLE_TRACKS(folder_path);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_tracks_hash ON $TABLE_TRACKS(stable_hash);")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_SCAN_METADATA (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL
            );
            """.trimIndent(),
        )
    }

    private fun seedInitialPlaylists(db: SQLiteDatabase) {
        val starterPlaylists = listOf(
            Triple("Chill Vibes", Color(0xFF06B6D4), listOf(1L, 4L, 8L, 12L, 15L, 18L, 19L)),
            Triple("Focus Flow", Color(0xFF10B981), listOf(1L, 7L, 8L, 9L, 15L, 19L)),
            Triple("Dreamscape", Color(0xFF3B82F6), listOf(1L, 3L, 7L, 8L, 9L, 11L, 19L)),
        )

        for ((name, color, tracks) in starterPlaylists) {
            val cv = ContentValues().apply {
                put("name", name)
                put("artwork_seed", color.toArgb())
                put("created_at", System.currentTimeMillis())
            }
            val playlistId = db.insert(TABLE_PLAYLISTS, null, cv)
            if (playlistId > 0) {
                tracks.forEachIndexed { index, trackId ->
                    val trackCv = ContentValues().apply {
                        put("playlist_id", playlistId)
                        put("track_id", trackId)
                        put("position", index)
                    }
                    db.insert(TABLE_PLAYLIST_TRACKS, null, trackCv)
                }
            }
        }

        // Seed starter favorites
        listOf(1L, 3L, 7L, 13L).forEach { id ->
            val cv = ContentValues().apply {
                put("track_id", id)
                put("created_at", System.currentTimeMillis())
            }
            db.insert(TABLE_FAVORITES, null, cv)
        }

        // Seed starter history
        listOf(1L, 2L, 3L, 4L, 5L, 6L).forEachIndexed { i, id ->
            val cv = ContentValues().apply {
                put("track_id", id)
                put("played_at", System.currentTimeMillis() - ((6 - i) * 60_000L))
            }
            db.insert(TABLE_HISTORY, null, cv)
        }
    }
}
