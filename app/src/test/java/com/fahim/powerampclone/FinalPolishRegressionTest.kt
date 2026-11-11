package com.fahim.powerampclone

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.graphics.Color
import com.fahim.powerampclone.data.MockMusicRepository
import com.fahim.powerampclone.data.MusicRepository
import com.fahim.powerampclone.data.db.InMemoryTrackStore
import com.fahim.powerampclone.data.local.MediaStoreMusicRepository
import com.fahim.powerampclone.data.local.PermissionHelper
import com.fahim.powerampclone.data.playlist.InMemoryPlaylistStore
import com.fahim.powerampclone.data.playlist.SQLitePlaylistStore
import com.fahim.powerampclone.data.scanner.AdvancedMediaScanner
import com.fahim.powerampclone.design.AccentChoice
import com.fahim.powerampclone.design.accentSeed
import com.fahim.powerampclone.library.LibraryViewModel
import com.fahim.powerampclone.model.Playlist
import com.fahim.powerampclone.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FinalPolishRegressionTest {

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
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockContext = FakeTestContext()
        PermissionHelper.permissionChecker = { true }
        playlistStore = InMemoryPlaylistStore()
        trackStore = InMemoryTrackStore()
        scanner = AdvancedMediaScanner(trackStore)
    }

    @After
    fun tearDown() {
        PermissionHelper.permissionChecker = null
        Dispatchers.resetMain()
    }

    private fun testTrack(id: Long, title: String): Track {
        return Track(
            id = id,
            title = title,
            artist = "Artist $id",
            album = "Album $id",
            durationMs = 180000L,
            artworkSeed = Color.Blue,
            contentUriString = "content://media/audio/$id",
        )
    }

    @Test
    fun testEmptyRepositoryReturnsZeroTracksAndNoFallbackMockTracks() {
        val repo = MediaStoreMusicRepository(
            context = mockContext,
            playlistStore = playlistStore,
            trackStore = trackStore,
            scanner = scanner,
            fallbackRepository = null, // Production default
            scope = testScope,
        )

        assertEquals(0, repo.tracks().size)
        assertEquals(0, repo.albums().size)
        assertEquals(0, repo.artists().size)
        assertEquals(0, repo.genres().size)
        assertEquals(0, repo.folders().size)
        assertEquals(0, repo.recentlyPlayed().size)
    }

    @Test
    fun testPlaylistTrackCountConsistencySingleSourceOfTruth() {
        val validTrack1 = testTrack(1L, "Song 1")
        val validTrack2 = testTrack(2L, "Song 2")
        trackStore.upsertTracks(listOf(validTrack1, validTrack2))

        val createdPlaylist = playlistStore.createPlaylist("Favorites", Color.Red)
        assertNotNull(createdPlaylist)
        // Add valid track 1, non-existent track 999, and valid track 2
        playlistStore.addTracksToPlaylist(createdPlaylist!!.id, listOf(1L, 999L, 2L))

        val repo = MediaStoreMusicRepository(
            context = mockContext,
            playlistStore = playlistStore,
            trackStore = trackStore,
            scanner = scanner,
            fallbackRepository = null,
            scope = testScope,
        )

        val vm = LibraryViewModel(repo)
        val targetPlaylist = vm.playlists.firstOrNull { it.id == createdPlaylist!!.id }
        assertNotNull(targetPlaylist)
        val detailTracks = vm.tracksForPlaylist(targetPlaylist!!)

        // List count and detail count MUST match exactly
        assertEquals(2, targetPlaylist.trackIds.size)
        assertEquals(2, detailTracks.size)
        assertEquals(listOf(1L, 2L), targetPlaylist.trackIds)
        assertEquals(listOf(1L, 2L), detailTracks.map { it.id })
    }

    @Test
    fun testSmartPlaylistFiltersStaleHistoryIds() {
        val validTrack = testTrack(10L, "Track 10")
        trackStore.upsertTracks(listOf(validTrack))

        // Record playback for valid track 10 and deleted track 99
        playlistStore.recordPlayback(10L, 1000L)
        playlistStore.recordPlayback(99L, 2000L)

        val repo = MediaStoreMusicRepository(
            context = mockContext,
            playlistStore = playlistStore,
            trackStore = trackStore,
            scanner = scanner,
            fallbackRepository = null,
            scope = testScope,
        )

        val vm = LibraryViewModel(repo)
        val smartPlaylists = vm.playlists.filter { it.isSmart }
        val recentPlaylist = smartPlaylists.firstOrNull { it.id == SQLitePlaylistStore.SMART_RECENTLY_PLAYED_ID }

        assertNotNull(recentPlaylist)
        // Stale ID 99 was filtered out by MediaStoreMusicRepository
        assertEquals(1, recentPlaylist!!.trackIds.size)
        assertEquals(listOf(10L), recentPlaylist.trackIds)
        assertEquals(1, vm.tracksForPlaylist(recentPlaylist).size)
    }

    @Test
    fun testAccentChoiceSeedsProduceDistinctManualThemes() {
        val emberSeed = accentSeed(AccentChoice.EMBER)
        val oceanSeed = accentSeed(AccentChoice.OCEAN)
        val forestSeed = accentSeed(AccentChoice.FOREST)
        val roseSeed = accentSeed(AccentChoice.ROSE)
        val graphiteSeed = accentSeed(AccentChoice.GRAPHITE)

        // Verify distinct seeds
        val seeds = setOf(emberSeed, oceanSeed, forestSeed, roseSeed, graphiteSeed)
        assertEquals(5, seeds.size)

        // Verify known tokens
        assertEquals(Color(0xFFC2410C), emberSeed)
        assertEquals(Color(0xFF0F766E), oceanSeed)
        assertEquals(Color(0xFF2E7D32), forestSeed)
        assertEquals(Color(0xFFBE123C), roseSeed)
        assertEquals(Color(0xFF64748B), graphiteSeed)
    }

    @Test
    fun testSearchRespectsLiveRepositoryState() {
        val t1 = testTrack(1L, "Midnight City")
        val t2 = testTrack(2L, "Starboy")
        trackStore.upsertTracks(listOf(t1, t2))

        val repo = MediaStoreMusicRepository(
            context = mockContext,
            playlistStore = playlistStore,
            trackStore = trackStore,
            scanner = scanner,
            fallbackRepository = null,
            scope = testScope,
        )

        val results = repo.search("City")
        assertEquals(1, results.tracks.size)
        assertEquals("Midnight City", results.tracks.first().title)

        val emptyResults = repo.search("NonexistentArtist")
        assertTrue(emptyResults.tracks.isEmpty())
    }
}
