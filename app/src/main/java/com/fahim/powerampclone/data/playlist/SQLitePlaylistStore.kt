package com.fahim.powerampclone.data.playlist

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.fahim.powerampclone.data.db.MioraDatabaseHelper
import com.fahim.powerampclone.model.Playlist
import com.fahim.powerampclone.model.seedColorForString

class SQLitePlaylistStore(
    context: Context,
    private val dbHelper: SQLiteOpenHelper = MioraDatabaseHelper.getInstance(context),
) : PlaylistStore {

    companion object {
        const val SMART_FAVORITES_ID = -100L
        const val SMART_RECENTLY_PLAYED_ID = -101L

        private const val TABLE_PLAYLISTS = MioraDatabaseHelper.TABLE_PLAYLISTS
        private const val TABLE_PLAYLIST_TRACKS = MioraDatabaseHelper.TABLE_PLAYLIST_TRACKS
        private const val TABLE_FAVORITES = MioraDatabaseHelper.TABLE_FAVORITES
        private const val TABLE_HISTORY = MioraDatabaseHelper.TABLE_HISTORY
    }

    override fun getAllPlaylists(): List<Playlist> {
        val result = mutableListOf<Playlist>()
        val db = dbHelper.readableDatabase

        // 1. Add Smart Playlists first
        val favTracks = getFavoriteTrackIds().toList()
        result.add(
            Playlist(
                id = SMART_FAVORITES_ID,
                name = "Favourites",
                artworkSeed = Color(0xFFEF4444),
                trackIds = favTracks,
                isSmart = true,
            )
        )

        val recentTracks = getRecentlyPlayedTrackIds(50)
        result.add(
            Playlist(
                id = SMART_RECENTLY_PLAYED_ID,
                name = "Recently Played",
                artworkSeed = Color(0xFFF59E0B),
                trackIds = recentTracks,
                isSmart = true,
            )
        )

        // 2. Add User Playlists
        val cursor = db.rawQuery("SELECT id, name, artwork_seed FROM $TABLE_PLAYLISTS ORDER BY name COLLATE NOCASE ASC", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val name = c.getString(1)
                val seedInt = c.getInt(2)
                val trackIds = getTrackIdsForPlaylist(db, id)
                result.add(
                    Playlist(
                        id = id,
                        name = name,
                        artworkSeed = Color(seedInt),
                        trackIds = trackIds,
                        isSmart = false,
                    )
                )
            }
        }

        return result
    }

    override fun getPlaylistById(id: Long): Playlist? {
        if (id == SMART_FAVORITES_ID) {
            return Playlist(
                id = SMART_FAVORITES_ID,
                name = "Favourites",
                artworkSeed = Color(0xFFEF4444),
                trackIds = getFavoriteTrackIds().toList(),
                isSmart = true,
            )
        }
        if (id == SMART_RECENTLY_PLAYED_ID) {
            return Playlist(
                id = SMART_RECENTLY_PLAYED_ID,
                name = "Recently Played",
                artworkSeed = Color(0xFFF59E0B),
                trackIds = getRecentlyPlayedTrackIds(50),
                isSmart = true,
            )
        }

        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id, name, artwork_seed FROM $TABLE_PLAYLISTS WHERE id = ?", arrayOf(id.toString()))
        cursor.use { c ->
            if (c.moveToFirst()) {
                val name = c.getString(1)
                val seedInt = c.getInt(2)
                val trackIds = getTrackIdsForPlaylist(db, id)
                return Playlist(
                    id = id,
                    name = name,
                    artworkSeed = Color(seedInt),
                    trackIds = trackIds,
                    isSmart = false,
                )
            }
        }
        return null
    }

    private fun getTrackIdsForPlaylist(db: SQLiteDatabase, playlistId: Long): List<Long> {
        val tracks = mutableListOf<Long>()
        val c = db.rawQuery(
            "SELECT track_id FROM $TABLE_PLAYLIST_TRACKS WHERE playlist_id = ? ORDER BY position ASC, id ASC",
            arrayOf(playlistId.toString()),
        )
        c.use {
            while (it.moveToNext()) {
                tracks.add(it.getLong(0))
            }
        }
        return tracks
    }

    override fun createPlaylist(name: String, artworkSeed: Color?): Playlist? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null

        val seed = artworkSeed ?: seedColorForString(trimmed)
        val db = dbHelper.writableDatabase

        val cv = ContentValues().apply {
            put("name", trimmed)
            put("artwork_seed", seed.toArgb())
            put("created_at", System.currentTimeMillis())
        }

        val id = db.insertWithOnConflict(TABLE_PLAYLISTS, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
        if (id <= 0) return null

        return Playlist(
            id = id,
            name = trimmed,
            artworkSeed = seed,
            trackIds = emptyList(),
            isSmart = false,
        )
    }

    override fun renamePlaylist(id: Long, newName: String): Boolean {
        if (id < 0L) return false // Cannot rename smart playlists
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return false

        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("name", trimmed)
        }
        val affected = db.updateWithOnConflict(TABLE_PLAYLISTS, cv, "id = ?", arrayOf(id.toString()), SQLiteDatabase.CONFLICT_IGNORE)
        return affected > 0
    }

    override fun deletePlaylist(id: Long): Boolean {
        if (id < 0L) return false // Cannot delete smart playlists
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_PLAYLIST_TRACKS, "playlist_id = ?", arrayOf(id.toString()))
            val count = db.delete(TABLE_PLAYLISTS, "id = ?", arrayOf(id.toString()))
            db.setTransactionSuccessful()
            return count > 0
        } finally {
            db.endTransaction()
        }
    }

    override fun addTracksToPlaylist(playlistId: Long, trackIds: List<Long>) {
        if (trackIds.isEmpty()) return
        if (playlistId == SMART_FAVORITES_ID) {
            trackIds.forEach { setFavorite(it, true) }
            return
        }
        if (playlistId < 0L) return // Cannot directly add to other smart playlists

        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            var maxPos = -1
            val posCursor = db.rawQuery(
                "SELECT MAX(position) FROM $TABLE_PLAYLIST_TRACKS WHERE playlist_id = ?",
                arrayOf(playlistId.toString()),
            )
            posCursor.use {
                if (it.moveToFirst() && !it.isNull(0)) {
                    maxPos = it.getInt(0)
                }
            }

            trackIds.forEachIndexed { idx, trackId ->
                val cv = ContentValues().apply {
                    put("playlist_id", playlistId)
                    put("track_id", trackId)
                    put("position", maxPos + 1 + idx)
                }
                db.insert(TABLE_PLAYLIST_TRACKS, null, cv)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun removeTrackFromPlaylist(playlistId: Long, trackId: Long, position: Int?) {
        if (playlistId == SMART_FAVORITES_ID) {
            setFavorite(trackId, false)
            return
        }
        if (playlistId < 0L) return

        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            if (position != null) {
                db.delete(
                    TABLE_PLAYLIST_TRACKS,
                    "playlist_id = ? AND track_id = ? AND position = ?",
                    arrayOf(playlistId.toString(), trackId.toString(), position.toString()),
                )
            } else {
                // Delete first match
                val c = db.rawQuery(
                    "SELECT id FROM $TABLE_PLAYLIST_TRACKS WHERE playlist_id = ? AND track_id = ? ORDER BY position ASC LIMIT 1",
                    arrayOf(playlistId.toString(), trackId.toString()),
                )
                var rowId: Long? = null
                c.use {
                    if (it.moveToFirst()) {
                        rowId = it.getLong(0)
                    }
                }
                if (rowId != null) {
                    db.delete(TABLE_PLAYLIST_TRACKS, "id = ?", arrayOf(rowId.toString()))
                }
            }

            // Re-sequence positions cleanly
            val resequenceCursor = db.rawQuery(
                "SELECT id FROM $TABLE_PLAYLIST_TRACKS WHERE playlist_id = ? ORDER BY position ASC, id ASC",
                arrayOf(playlistId.toString()),
            )
            val rowIds = mutableListOf<Long>()
            resequenceCursor.use {
                while (it.moveToNext()) {
                    rowIds.add(it.getLong(0))
                }
            }
            rowIds.forEachIndexed { newPos, rowId ->
                val cv = ContentValues().apply { put("position", newPos) }
                db.update(TABLE_PLAYLIST_TRACKS, cv, "id = ?", arrayOf(rowId.toString()))
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun reorderTracks(playlistId: Long, fromIndex: Int, toIndex: Int) {
        if (playlistId < 0L || fromIndex == toIndex) return
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val cursor = db.rawQuery(
                "SELECT id, track_id FROM $TABLE_PLAYLIST_TRACKS WHERE playlist_id = ? ORDER BY position ASC, id ASC",
                arrayOf(playlistId.toString()),
            )
            val items = mutableListOf<Pair<Long, Long>>()
            cursor.use {
                while (it.moveToNext()) {
                    items.add(it.getLong(0) to it.getLong(1))
                }
            }

            if (fromIndex in items.indices && toIndex in items.indices) {
                val moved = items.removeAt(fromIndex)
                items.add(toIndex, moved)

                items.forEachIndexed { newPos, (rowId, _) ->
                    val cv = ContentValues().apply { put("position", newPos) }
                    db.update(TABLE_PLAYLIST_TRACKS, cv, "id = ?", arrayOf(rowId.toString()))
                }
                db.setTransactionSuccessful()
            }
        } finally {
            db.endTransaction()
        }
    }

    override fun getFavoriteTrackIds(): Set<Long> {
        val result = LinkedHashSet<Long>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT track_id FROM $TABLE_FAVORITES ORDER BY created_at DESC", null)
        cursor.use {
            while (it.moveToNext()) {
                result.add(it.getLong(0))
            }
        }
        return result
    }

    override fun setFavorite(trackId: Long, isFavorite: Boolean) {
        val db = dbHelper.writableDatabase
        if (isFavorite) {
            val cv = ContentValues().apply {
                put("track_id", trackId)
                put("created_at", System.currentTimeMillis())
            }
            db.insertWithOnConflict(TABLE_FAVORITES, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        } else {
            db.delete(TABLE_FAVORITES, "track_id = ?", arrayOf(trackId.toString()))
        }
    }

    override fun getRecentlyPlayedTrackIds(limit: Int): List<Long> {
        val result = mutableListOf<Long>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT track_id, MAX(played_at) as latest FROM $TABLE_HISTORY GROUP BY track_id ORDER BY latest DESC LIMIT ?",
            arrayOf(limit.toString()),
        )
        cursor.use {
            while (it.moveToNext()) {
                result.add(it.getLong(0))
            }
        }
        return result
    }

    override fun recordPlayback(trackId: Long, timestamp: Long) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("track_id", trackId)
            put("played_at", timestamp)
        }
        db.insert(TABLE_HISTORY, null, cv)

        // Trim old history entries keeping only last 200
        db.execSQL(
            """
            DELETE FROM $TABLE_HISTORY WHERE id NOT IN (
                SELECT id FROM $TABLE_HISTORY ORDER BY played_at DESC LIMIT 200
            )
            """.trimIndent()
        )
    }
}
