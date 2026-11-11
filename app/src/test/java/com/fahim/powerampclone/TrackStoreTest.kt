package com.fahim.powerampclone

import com.fahim.powerampclone.data.db.InMemoryTrackStore
import com.fahim.powerampclone.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TrackStoreTest {

    private lateinit var store: InMemoryTrackStore

    @Before
    fun setUp() {
        store = InMemoryTrackStore()
    }

    private fun sampleTrack(id: Long, title: String, artist: String = "Test Artist", album: String = "Test Album"): Track {
        return Track(
            id = id,
            title = title,
            artist = artist,
            album = album,
            contentUriString = "content://media/external/audio/media/$id",
            folderPath = "/storage/emulated/0/Music",
            durationMs = 180000L,
            trackNumber = id.toInt(),
            year = 2024,
            format = "FLAC",
        )
    }

    @Test
    fun `upsertTracks stores and returns all tracks ordered by title`() {
        val tracks = listOf(
            sampleTrack(1L, "Zebra"),
            sampleTrack(2L, "Alpha"),
            sampleTrack(3L, "Beta"),
        )

        val inserted = store.upsertTracks(tracks)
        assertEquals(3, inserted)
        assertEquals(3, store.getTrackCount())

        val stored = store.getAllTracks()
        assertEquals("Alpha", stored[0].title)
        assertEquals("Beta", stored[1].title)
        assertEquals("Zebra", stored[2].title)
    }

    @Test
    fun `getTrackById retrieves exact track`() {
        store.upsertTracks(listOf(sampleTrack(42L, "Answer Track")))
        val track = store.getTrackById(42L)
        assertNotNull(track)
        assertEquals("Answer Track", track!!.title)

        assertNull(store.getTrackById(999L))
    }

    @Test
    fun `deleteTracks removes specified tracks`() {
        store.upsertTracks(listOf(sampleTrack(1L, "T1"), sampleTrack(2L, "T2"), sampleTrack(3L, "T3")))
        assertEquals(3, store.getTrackCount())

        val deleted = store.deleteTracks(listOf(1L, 3L))
        assertEquals(2, deleted)
        assertEquals(1, store.getTrackCount())
        assertNotNull(store.getTrackById(2L))
    }

    @Test
    fun `deleteTracksNotIn purges stale tracks`() {
        store.upsertTracks(listOf(sampleTrack(1L, "T1"), sampleTrack(2L, "T2"), sampleTrack(3L, "T3")))

        val purged = store.deleteTracksNotIn(listOf(2L))
        assertEquals(2, purged)
        assertEquals(1, store.getTrackCount())
        assertEquals(2L, store.getAllTracks().first().id)
    }

    @Test
    fun `searchTracks matches title, artist, or album`() {
        store.upsertTracks(
            listOf(
                sampleTrack(1L, "Stargazing", "Kygo", "Cloud Nine"),
                sampleTrack(2L, "Midnight City", "M83", "Hurry Up"),
                sampleTrack(3L, "Solar Echoes", "Nigel Stanford", "Solar"),
            ),
        )

        val byTitle = store.searchTracks("Star")
        assertEquals(1, byTitle.size)
        assertEquals(1L, byTitle[0].id)

        val byArtist = store.searchTracks("M83")
        assertEquals(1, byArtist.size)
        assertEquals(2L, byArtist[0].id)

        val byAlbum = store.searchTracks("Solar")
        assertEquals(1, byAlbum.size)
        assertEquals(3L, byAlbum[0].id)
    }

    @Test
    fun `lastScanTimestamp can be set and retrieved`() {
        assertEquals(0L, store.getLastScanTimestamp())
        val timestamp = 1718000000000L
        store.setLastScanTimestamp(timestamp)
        assertEquals(timestamp, store.getLastScanTimestamp())
    }
}
