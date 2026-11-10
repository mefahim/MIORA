package com.fahim.powerampclone

import com.fahim.powerampclone.data.local.LocalMediaAggregator
import com.fahim.powerampclone.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalMediaAggregatorTest {

    @Test
    fun `aggregate empty tracks produces empty collections`() {
        val result = LocalMediaAggregator.aggregate(emptyList())
        assertTrue(result.tracks.isEmpty())
        assertTrue(result.albums.isEmpty())
        assertTrue(result.artists.isEmpty())
        assertTrue(result.folders.isEmpty())
        assertTrue(result.genres.isEmpty())
    }

    @Test
    fun `aggregate groups tracks into albums and preserves album id and track ids`() {
        val t1 = Track(
            id = 101L,
            title = "Track One",
            artist = "Artist A",
            album = "Album Alpha",
            albumId = 501L,
            artistId = 901L,
            durationMs = 180000L,
            year = 2021,
            artworkUriString = "content://media/external/audio/albumart/501",
        )
        val t2 = Track(
            id = 102L,
            title = "Track Two",
            artist = "Artist A",
            album = "Album Alpha",
            albumId = 501L,
            artistId = 901L,
            durationMs = 210000L,
            year = 2021,
            artworkUriString = "content://media/external/audio/albumart/501",
        )
        val t3 = Track(
            id = 103L,
            title = "Track Three",
            artist = "Artist B",
            album = "Album Beta",
            albumId = 502L,
            artistId = 902L,
            durationMs = 150000L,
            year = 2023,
        )

        val result = LocalMediaAggregator.aggregate(listOf(t1, t2, t3))

        assertEquals(3, result.tracks.size)
        assertEquals(2, result.albums.size)

        val albumAlpha = result.albums.firstOrNull { it.id == 501L }
        assertNotNull(albumAlpha)
        assertEquals("Album Alpha", albumAlpha?.title)
        assertEquals("Artist A", albumAlpha?.artist)
        assertEquals(listOf(101L, 102L), albumAlpha?.trackIds)
        assertEquals("content://media/external/audio/albumart/501", albumAlpha?.artworkUriString)

        val albumBeta = result.albums.firstOrNull { it.id == 502L }
        assertNotNull(albumBeta)
        assertEquals(listOf(103L), albumBeta?.trackIds)
    }

    @Test
    fun `aggregate groups tracks into artists correctly`() {
        val t1 = Track(id = 1L, title = "Song 1", artist = "Cosmic Star", album = "A1", albumId = 10L, artistId = 20L)
        val t2 = Track(id = 2L, title = "Song 2", artist = "Cosmic Star", album = "A2", albumId = 11L, artistId = 20L)
        val t3 = Track(id = 3L, title = "Song 3", artist = "Solar Wind", album = "A3", albumId = 12L, artistId = 21L)

        val result = LocalMediaAggregator.aggregate(listOf(t1, t2, t3))

        assertEquals(2, result.artists.size)
        val cosmic = result.artists.firstOrNull { it.id == 20L }
        assertNotNull(cosmic)
        assertEquals("Cosmic Star", cosmic?.name)
        assertEquals(listOf(10L, 11L), cosmic?.albumIds)
        assertEquals(listOf(1L, 2L), cosmic?.trackIds)
    }

    @Test
    fun `aggregate extracts folders and genres properly`() {
        val t1 = Track(
            id = 1L,
            title = "Song A",
            artist = "Art",
            album = "Alb",
            folderPath = "/storage/emulated/0/Music/Rock",
            genre = "Rock",
        )
        val t2 = Track(
            id = 2L,
            title = "Song B",
            artist = "Art",
            album = "Alb",
            folderPath = "/storage/emulated/0/Music/Rock",
            genre = "Rock",
        )
        val t3 = Track(
            id = 3L,
            title = "Song C",
            artist = "Art",
            album = "Alb",
            folderPath = "/storage/emulated/0/Music/Jazz",
            genre = "Jazz",
        )

        val result = LocalMediaAggregator.aggregate(listOf(t1, t2, t3))

        assertEquals(2, result.folders.size)
        val rockFolder = result.folders.firstOrNull { it.path == "/storage/emulated/0/Music/Rock" }
        assertNotNull(rockFolder)
        assertEquals(listOf(1L, 2L), rockFolder?.trackIds)

        assertEquals(2, result.genres.size)
        val rockGenre = result.genres.firstOrNull { it.name == "Rock" }
        assertNotNull(rockGenre)
        assertEquals(listOf(1L, 2L), rockGenre?.trackIds)
    }

    @Test
    fun `aggregate handles blank strings and unknown metadata gracefully`() {
        val t = Track(
            id = 99L,
            title = " ",
            artist = " ",
            album = " ",
            albumId = 0L,
            artistId = 0L,
            folderPath = "",
            genre = "",
            format = "MP3 (320kbps)",
        )

        val result = LocalMediaAggregator.aggregate(listOf(t))

        assertEquals(1, result.albums.size)
        assertEquals("Unknown Album", result.albums.first().title)
        assertEquals("Unknown Artist", result.albums.first().artist)

        assertEquals(1, result.artists.size)
        assertEquals("Unknown Artist", result.artists.first().name)

        assertEquals(1, result.folders.size)
        assertEquals("/Music", result.folders.first().path)

        assertEquals(1, result.genres.size)
        assertEquals("Unknown", result.genres.first().name)
    }
}
