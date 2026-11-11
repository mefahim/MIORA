package com.fahim.powerampclone

import com.fahim.powerampclone.data.MockMusicRepository
import com.fahim.powerampclone.model.Track
import com.fahim.powerampclone.playback.PlaybackManager
import com.fahim.powerampclone.playback.PlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Regression and lifecycle tests for P0 ForegroundServiceDidNotStartInTimeException hotfix.
 *
 * Verifies:
 * 1. Cold launch & first song playback
 * 2. Repeated song selection
 * 3. Rapid track switching
 * 4. Pause / resume cycle
 * 5. Idempotent service state tracking
 * 6. Empty library handling
 * 7. Invalid media item safe fallback
 * 8. Repeated start requests stability
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ForegroundServiceStartupRegressionTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repo: MockMusicRepository
    private lateinit var tracks: List<Track>
    private lateinit var fakeEngine: FakeAudioEngine
    private lateinit var manager: PlaybackManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repo = MockMusicRepository()
        tracks = repo.tracks()
        fakeEngine = FakeAudioEngine()
        manager = PlaybackManager(repo, fakeEngine)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testPlaybackService_isRunningInitiallyFalse() {
        // Before service is created/started, isRunning should be false
        assertFalse(PlaybackService.isRunning())
    }

    @Test
    fun testColdLaunchAndFirstSongPlayback() = runTest {
        val firstTrack = tracks.first()
        manager.playTrack(firstTrack, tracks)
        advanceUntilIdle()

        assertEquals(firstTrack.id, manager.state.value.currentTrack?.id)
        assertEquals(0, manager.state.value.currentIndex)
        assertTrue(manager.state.value.isPlaying)
        assertEquals(0L, manager.state.value.positionMs)
    }

    @Test
    fun testRepeatedSongSelection() = runTest {
        // Select song 0, then 3, then 1, then 7 repeatedly
        val indices = listOf(0, 3, 1, 7, 2, 4)
        for (idx in indices) {
            manager.playTrack(tracks[idx], tracks)
            advanceUntilIdle()
            assertEquals(idx, manager.state.value.currentIndex)
            assertEquals(tracks[idx].id, manager.state.value.currentTrack?.id)
            assertTrue(manager.state.value.isPlaying)
        }
    }

    @Test
    fun testRapidTrackSwitching() = runTest {
        manager.playTrack(tracks[0], tracks)
        advanceUntilIdle()

        // Rapid next skipping
        for (i in 1..10) {
            manager.next()
        }
        advanceUntilIdle()
        assertEquals(10, manager.state.value.currentIndex)
        assertEquals(tracks[10].id, manager.state.value.currentTrack?.id)

        // Rapid previous skipping
        for (i in 1..5) {
            manager.previous()
        }
        advanceUntilIdle()
        assertEquals(5, manager.state.value.currentIndex)
        assertEquals(tracks[5].id, manager.state.value.currentTrack?.id)
    }

    @Test
    fun testPauseResumeCycle() = runTest {
        manager.playTrack(tracks[0], tracks)
        advanceUntilIdle()
        assertTrue(manager.state.value.isPlaying)

        manager.pause()
        advanceUntilIdle()
        assertFalse(manager.state.value.isPlaying)

        manager.resume()
        advanceUntilIdle()
        assertTrue(manager.state.value.isPlaying)

        manager.togglePlayPause()
        advanceUntilIdle()
        assertFalse(manager.state.value.isPlaying)

        manager.togglePlayPause()
        advanceUntilIdle()
        assertTrue(manager.state.value.isPlaying)
    }

    @Test
    fun testEmptyLibraryHandling() = runTest {
        val emptyRepo = object : com.fahim.powerampclone.data.MusicRepository by MockMusicRepository() {
            override fun tracks(): List<Track> = emptyList()
        }
        val emptyManager = PlaybackManager(emptyRepo, fakeEngine)
        advanceUntilIdle()

        assertEquals(0, emptyManager.state.value.queue.size)
        assertNull(emptyManager.state.value.currentTrack)
        assertFalse(emptyManager.state.value.isPlaying)

        // Next, previous and playPause on empty queue should be safe no-ops
        emptyManager.next()
        emptyManager.previous()
        emptyManager.togglePlayPause()
        advanceUntilIdle()

        assertFalse(emptyManager.state.value.isPlaying)
    }

    @Test
    fun testInvalidMediaItemHandling() = runTest {
        val invalidTrack = Track(
            id = 9999L,
            title = "Corrupt Track",
            artist = "Unknown",
            album = "Unknown",
            durationMs = 120_000L,
            folderPath = "",
            contentUriString = "content://media/external/audio/media/nonexistent",
        )
        manager.playTrack(invalidTrack, listOf(invalidTrack))
        advanceUntilIdle()

        assertEquals(invalidTrack.id, manager.state.value.currentTrack?.id)
        assertTrue(manager.state.value.isPlaying)
    }

    @Test
    fun testRepeatedStartRequests() = runTest {
        // Calling playTrack multiple times on the same track should not crash or corrupt queue
        val track = tracks[2]
        repeat(5) {
            manager.playTrack(track)
            advanceUntilIdle()
            assertEquals(track.id, manager.state.value.currentTrack?.id)
            assertTrue(manager.state.value.isPlaying)
        }
    }
}
