package com.fahim.powerampclone

import android.content.Context
import android.content.ContextWrapper
import com.fahim.powerampclone.data.MockMusicRepository
import com.fahim.powerampclone.data.db.InMemoryTrackStore
import com.fahim.powerampclone.data.local.MediaStoreMusicRepository
import com.fahim.powerampclone.data.local.PermissionHelper
import com.fahim.powerampclone.data.playlist.InMemoryPlaylistStore
import com.fahim.powerampclone.data.scanner.AdvancedMediaScanner
import com.fahim.powerampclone.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryDataIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private class FakeTestContext : ContextWrapper(null) {
        override fun getApplicationContext(): Context = this
    }

    private lateinit var mockContext: Context
    private lateinit var playlistStore: InMemoryPlaylistStore
    private lateinit var trackStore: InMemoryTrackStore
    private lateinit var scanner: AdvancedMediaScanner

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockContext = FakeTestContext()
        PermissionHelper.permissionChecker = { false }

        playlistStore = InMemoryPlaylistStore()
        trackStore = InMemoryTrackStore()
        scanner = AdvancedMediaScanner(trackStore)
    }

    @After
    fun tearDown() {
        PermissionHelper.permissionChecker = null
        Dispatchers.resetMain()
    }

    private fun sampleTrack(id: Long, title: String, artist: String, album: String, folder: String = "/Music"): Track {
        return Track(
            id = id,
            title = title,
            artist = artist,
            album = album,
            albumId = album.hashCode().toLong(),
            artistId = artist.hashCode().toLong(),
            durationMs = 210000L,
            trackNumber = id.toInt(),
            year = 2024,
            folderPath = folder,
            contentUriString = "content://media/external/audio/media/$id",
        )
    }

    @Test
    fun `repository serves cached tracks instantly on cold startup`() {
        // Pre-populate trackStore cache
        trackStore.upsertTracks(
            listOf(
                sampleTrack(101L, "Solaris", "Photay", "Onism"),
                sampleTrack(102L, "Aura", "Bicep", "Bicep"),
            ),
        )

        val repo = MediaStoreMusicRepository(
            context = mockContext,
            playlistStore = playlistStore,
            trackStore = trackStore,
            scanner = scanner,
            fallbackRepository = MockMusicRepository(playlistStore),
            scope = testScope,
        )

        assertTrue(repo.isLocalLoaded.value)
        assertEquals(2, repo.tracks().size)
        assertEquals("Aura", repo.tracks()[0].title)
        assertEquals("Solaris", repo.tracks()[1].title)
    }

    @Test
    fun `aggregation generates albums artists and folders accurately from cache`() {
        trackStore.upsertTracks(
            listOf(
                sampleTrack(1L, "Track 1", "Artist A", "Album X", "/Storage/Music"),
                sampleTrack(2L, "Track 2", "Artist A", "Album X", "/Storage/Music"),
                sampleTrack(3L, "Track 3", "Artist B", "Album Y", "/Storage/Downloads"),
            ),
        )

        val repo = MediaStoreMusicRepository(
            context = mockContext,
            playlistStore = playlistStore,
            trackStore = trackStore,
            scanner = scanner,
            fallbackRepository = MockMusicRepository(playlistStore),
            scope = testScope,
        )

        assertEquals(2, repo.albums().size)
        assertEquals(2, repo.artists().size)
        assertEquals(2, repo.folders().size)
    }

    @Test
    fun `search filters across cached tracks albums and artists`() {
        trackStore.upsertTracks(
            listOf(
                sampleTrack(1L, "Resonance", "HOME", "Odyssey"),
                sampleTrack(2L, "Voyager", "Daft Punk", "Discovery"),
            ),
        )

        val repo = MediaStoreMusicRepository(
            context = mockContext,
            playlistStore = playlistStore,
            trackStore = trackStore,
            scanner = scanner,
            fallbackRepository = MockMusicRepository(playlistStore),
            scope = testScope,
        )

        val results = repo.search("Daft")
        assertEquals(1, results.tracks.size)
        assertEquals("Voyager", results.tracks.first().title)
        assertEquals(1, results.artists.size)
        assertEquals("Daft Punk", results.artists.first().name)
    }

    @Test
    fun `fallback repository is used gracefully when cache is empty and permissions denied`() {
        val repo = MediaStoreMusicRepository(
            context = mockContext,
            playlistStore = playlistStore,
            trackStore = trackStore,
            scanner = scanner,
            fallbackRepository = MockMusicRepository(playlistStore),
            scope = testScope,
        )

        assertFalse(repo.isLocalLoaded.value)
        assertTrue(repo.tracks().isNotEmpty()) // Fallback sample tracks
    }
}
