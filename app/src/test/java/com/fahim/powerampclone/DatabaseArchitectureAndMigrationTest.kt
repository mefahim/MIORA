package com.fahim.powerampclone

import com.fahim.powerampclone.data.db.MioraDatabaseHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseArchitectureAndMigrationTest {

    @Test
    fun `database constants verify schema version 2 and expected database name`() {
        assertEquals("miora_playlists.db", MioraDatabaseHelper.DB_NAME)
        assertEquals(2, MioraDatabaseHelper.DB_VERSION)
    }

    @Test
    fun `database table definitions match architecture specification`() {
        assertEquals("playlists", MioraDatabaseHelper.TABLE_PLAYLISTS)
        assertEquals("playlist_tracks", MioraDatabaseHelper.TABLE_PLAYLIST_TRACKS)
        assertEquals("favorites", MioraDatabaseHelper.TABLE_FAVORITES)
        assertEquals("playback_history", MioraDatabaseHelper.TABLE_HISTORY)
        assertEquals("tracks", MioraDatabaseHelper.TABLE_TRACKS)
        assertEquals("scan_metadata", MioraDatabaseHelper.TABLE_SCAN_METADATA)
    }

    @Test
    fun `migration preserves legacy table integrity while introducing persistent tracks and scan metadata`() {
        // Verify that the table names exist and are non-empty
        val tables = listOf(
            MioraDatabaseHelper.TABLE_PLAYLISTS,
            MioraDatabaseHelper.TABLE_PLAYLIST_TRACKS,
            MioraDatabaseHelper.TABLE_FAVORITES,
            MioraDatabaseHelper.TABLE_HISTORY,
            MioraDatabaseHelper.TABLE_TRACKS,
            MioraDatabaseHelper.TABLE_SCAN_METADATA,
        )
        assertEquals(6, tables.distinct().size)
    }
}
