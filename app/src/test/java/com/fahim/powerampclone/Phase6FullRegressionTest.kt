package com.fahim.powerampclone

import com.fahim.powerampclone.data.MockMusicRepository
import com.fahim.powerampclone.data.lyrics.InMemoryLyricOffsetStore
import com.fahim.powerampclone.data.lyrics.LrcParser
import com.fahim.powerampclone.data.playlist.InMemoryPlaylistStore
import com.fahim.powerampclone.model.RepeatMode
import com.fahim.powerampclone.model.Track
import com.fahim.powerampclone.playback.EqualizerController
import com.fahim.powerampclone.playback.InMemoryDspSettingsStore
import com.fahim.powerampclone.playback.PlaybackManager
import com.fahim.powerampclone.playback.PlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Phase 6 Master Full Regression Test Suite.
 *
 * Validates cross-subsystem integrity, boundary security, edge conditions,
 * and application identity migration under `com.fhf.miora`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Phase6FullRegressionTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeEngine: FakeAudioEngine
    private lateinit var repository: MockMusicRepository
    private lateinit var manager: PlaybackManager
    private lateinit var playlistStore: InMemoryPlaylistStore
    private lateinit var dspStore: InMemoryDspSettingsStore
    private lateinit var eqController: EqualizerController

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeEngine = FakeAudioEngine()
        repository = MockMusicRepository()
        manager = PlaybackManager(repository, fakeEngine)
        playlistStore = InMemoryPlaylistStore()
        dspStore = InMemoryDspSettingsStore()
        eqController = EqualizerController(dspStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =========================================================================
    // 1. Application Identity & Action Namespace Migration Verification
    // =========================================================================

    @Test
    fun testTargetApplicationIdActionNamespaces() {
        assertEquals("com.fhf.miora.ACTION_PLAY_PAUSE", PlaybackService.ACTION_PLAY_PAUSE)
        assertEquals("com.fhf.miora.ACTION_NEXT", PlaybackService.ACTION_NEXT)
        assertEquals("com.fhf.miora.ACTION_PREVIOUS", PlaybackService.ACTION_PREVIOUS)
        assertEquals("com.fhf.miora.ACTION_STOP", PlaybackService.ACTION_STOP)
    }

    // =========================================================================
    // 2. Playback Pipeline & Queue State Edge Cases
    // =========================================================================

    @Test
    fun testInitialPlaybackStateLoadsRepositoryTracks() {
        val state = manager.state.value
        assertEquals("Sky Dreams", state.currentTrack?.title)
        assertEquals(0, state.currentIndex)
        assertFalse(state.isPlaying)
        assertEquals(0L, state.positionMs)
        assertEquals(20, state.queue.size)
    }

    @Test
    fun testInitialPlaybackStateWithEmptyRepository() {
        val emptyRepo = object : com.fahim.powerampclone.data.MusicRepository by MockMusicRepository() {
            override fun tracks(): List<Track> = emptyList()
        }
        val emptyManager = PlaybackManager(emptyRepo, fakeEngine)
        val state = emptyManager.state.value
        assertNull(state.currentTrack)
        assertEquals(0, state.currentIndex)
        assertFalse(state.isPlaying)
        assertEquals(0L, state.positionMs)
        assertEquals(0L, state.durationMs)
        assertTrue(state.queue.isEmpty())
    }

    @Test
    fun testPlayTrackUpdatesCurrentIndexAndEngine() {
        val tracks = repository.tracks()
        val target = tracks[2]
        manager.playTrack(target, tracks)

        val state = manager.state.value
        assertEquals(target.id, state.currentTrack?.id)
        assertEquals(2, state.currentIndex)
        assertTrue(state.isPlaying)
        assertEquals(target, fakeEngine.playedTrack)
    }

    @Test
    fun testRepeatModeCycleBoundaryTransitions() {
        val tracks = repository.tracks().take(2)
        manager.playTrack(tracks[0], tracks)

        // Initial default is RepeatMode.OFF
        assertEquals(RepeatMode.OFF, manager.state.value.repeat)

        // OFF -> ALL
        manager.cycleRepeat()
        assertEquals(RepeatMode.ALL, manager.state.value.repeat)

        // ALL -> ONE
        manager.cycleRepeat()
        assertEquals(RepeatMode.ONE, manager.state.value.repeat)

        // ONE -> OFF
        manager.cycleRepeat()
        assertEquals(RepeatMode.OFF, manager.state.value.repeat)
    }

    @Test
    fun testPreviousTrackRewindVsPredecessor() {
        val tracks = repository.tracks().take(3)
        manager.playTrack(tracks[1], tracks) // index 1

        // When position > 3000ms, previous rewinds to start of current track
        manager.seekTo(4500L)
        manager.previous()
        assertEquals(1, manager.state.value.currentIndex)
        assertEquals(0L, manager.state.value.positionMs)

        // When position <= 3000ms, previous goes to track 0
        manager.seekTo(1000L)
        manager.previous()
        assertEquals(0, manager.state.value.currentIndex)
        assertEquals(tracks[0].id, manager.state.value.currentTrack?.id)
    }

    @Test
    fun testQueueRemovalAndClearance() {
        val tracks = repository.tracks().take(3)
        manager.playTrack(tracks[1], tracks) // index 1 is playing

        // Remove track at index 2 (after current)
        manager.removeQueueItem(2)
        assertEquals(2, manager.state.value.queue.size)
        assertEquals(1, manager.state.value.currentIndex)

        // Remove track at index 0 (before current) -> current index adjusts to 0
        manager.removeQueueItem(0)
        assertEquals(1, manager.state.value.queue.size)
        assertEquals(0, manager.state.value.currentIndex)
        assertEquals(tracks[1].id, manager.state.value.currentTrack?.id)

        // Clear queue: preserves currently playing track as per Poweramp contract
        manager.clearQueue()
        assertEquals(1, manager.state.value.queue.size)
        assertEquals(tracks[1].id, manager.state.value.currentTrack?.id)

        // setQueue with empty list is a safe no-op (preserves active queue integrity)
        manager.setQueue(emptyList())
        assertEquals(1, manager.state.value.queue.size)
        assertEquals(tracks[1].id, manager.state.value.currentTrack?.id)

        // setQueue with new list replaces queue
        manager.setQueue(listOf(tracks[0], tracks[2]))
        assertEquals(2, manager.state.value.queue.size)
        assertEquals(tracks[0].id, manager.state.value.queue[0].id)
        assertEquals(tracks[2].id, manager.state.value.queue[1].id)
    }

    @Test
    fun testQueueReorderBoundaries() {
        val tracks = repository.tracks().take(3)
        manager.playTrack(tracks[0], tracks)

        // Move track 0 up (boundary check: cannot move outside bounds)
        manager.moveQueueItem(0, -1)
        assertEquals(tracks[0].id, manager.state.value.queue[0].id)

        // Move track 2 down (boundary check: cannot move outside bounds)
        manager.moveQueueItem(2, 3)
        assertEquals(tracks[2].id, manager.state.value.queue[2].id)

        // Valid move: move index 0 down to index 1
        manager.moveQueueItem(0, 1)
        assertEquals(tracks[1].id, manager.state.value.queue[0].id)
        assertEquals(tracks[0].id, manager.state.value.queue[1].id)
    }

    // =========================================================================
    // 3. Library & Playlist Data Store Resilience
    // =========================================================================

    @Test
    fun testRepositoryNonExistentTrackLookupSafe() {
        val missingTrack = repository.trackById(999999L)
        assertNull(missingTrack)
    }

    @Test
    fun testPlaylistStoreCrudOperations() {
        val playlist = playlistStore.createPlaylist("Favorites")
        assertNotNull(playlist)
        val playlistId = playlist!!.id
        assertEquals("Favorites", playlist.name)
        assertTrue(playlist.trackIds.isEmpty())

        // Add tracks
        playlistStore.addTracksToPlaylist(playlistId, listOf(101L, 102L))
        val updated = playlistStore.getPlaylistById(playlistId)
        assertNotNull(updated)
        assertEquals(listOf(101L, 102L), updated?.trackIds)

        // Remove track
        playlistStore.removeTrackFromPlaylist(playlistId, 101L)
        val afterRemoval = playlistStore.getPlaylistById(playlistId)
        assertEquals(listOf(102L), afterRemoval?.trackIds)

        // Delete playlist
        val deleted = playlistStore.deletePlaylist(playlistId)
        assertTrue(deleted)
        assertNull(playlistStore.getPlaylistById(playlistId))
    }

    // =========================================================================
    // 4. Synced Lyrics & Multi-Tag Parsing
    // =========================================================================

    @Test
    fun testLrcParserWithMultiTagTimestampsAndOffsets() {
        val lrcContent = """
            [ti:Test Song]
            [ar:Test Artist]
            [offset:+500]
            [00:01.00]Line 1
            [00:05.00][00:15.00]Chorus line repeated
            [00:20.50]Final line
        """.trimIndent()

        val parsed = LrcParser.parseLrc(lrcContent)
        assertTrue(parsed.isNotEmpty())

        // Offset +500ms shifts timestamps
        // [00:01.00] = 1000ms + 500ms = 1500ms
        val line1 = parsed.find { it.text == "Line 1" }
        assertNotNull(line1)
        assertEquals(1500L, line1?.startMs)

        // Repeated timestamp should produce 2 entries
        val chorusLines = parsed.filter { it.text == "Chorus line repeated" }
        assertEquals(2, chorusLines.size)
        assertEquals(5500L, chorusLines[0].startMs)
        assertEquals(15500L, chorusLines[1].startMs)
    }

    @Test
    fun testLyricOffsetStoreAdjustments() {
        val offsetStore = InMemoryLyricOffsetStore()
        val trackId = 55L

        assertEquals(0L, offsetStore.getOffset(trackId))
        offsetStore.setOffset(trackId, -750L)
        assertEquals(-750L, offsetStore.getOffset(trackId))

        offsetStore.setOffset(trackId, 1200L)
        assertEquals(1200L, offsetStore.getOffset(trackId))

        offsetStore.clearOffset(trackId)
        assertEquals(0L, offsetStore.getOffset(trackId))
    }

    // =========================================================================
    // 5. Advanced DSP & Equalizer System Boundaries
    // =========================================================================

    @Test
    fun testEqualizerBandsClamping() {
        eqController.setEnabled(true)
        assertTrue(eqController.state.value.enabled)

        // Set band level within bounds (gainDb float: -12.0f .. +12.0f)
        eqController.setBandGain(0, 5.0f)
        assertEquals(5.0f, eqController.state.value.bandGains[0], 0.01f)

        // Clamping upper limit (+12.0f)
        eqController.setBandGain(0, 18.0f)
        assertEquals(12.0f, eqController.state.value.bandGains[0], 0.01f)

        // Clamping lower limit (-12.0f)
        eqController.setBandGain(0, -20.0f)
        assertEquals(-12.0f, eqController.state.value.bandGains[0], 0.01f)
    }

    @Test
    fun testBassBoostAndVirtualizerClamping() {
        eqController.setBassBoostEnabled(true)
        eqController.setBassBoostStrength(500)
        assertEquals(500, eqController.state.value.bassBoostStrength)

        // Beyond upper bound (clamp to 1000)
        eqController.setBassBoostStrength(2000)
        assertEquals(1000, eqController.state.value.bassBoostStrength)

        // Negative bound (clamp to 0)
        eqController.setBassBoostStrength(-300)
        assertEquals(0, eqController.state.value.bassBoostStrength)

        // Virtualizer
        eqController.setVirtualizerEnabled(true)
        eqController.setVirtualizerStrength(750)
        assertEquals(750, eqController.state.value.virtualizerStrength)

        eqController.setVirtualizerStrength(3000)
        assertEquals(1000, eqController.state.value.virtualizerStrength)
    }

    @Test
    fun testDspResetRestoresDefaults() {
        eqController.setEnabled(true)
        eqController.setBandGain(2, 8.0f)
        eqController.setBassBoostEnabled(true)
        eqController.setBassBoostStrength(900)
        eqController.setVirtualizerEnabled(true)
        eqController.setVirtualizerStrength(850)

        eqController.reset()

        val state = eqController.state.value
        assertEquals(0f, state.bandGains[2], 0.01f)
        assertEquals(0, state.bassBoostStrength)
        assertEquals(0, state.virtualizerStrength)
        assertFalse(state.bassBoostEnabled)
        assertFalse(state.virtualizerEnabled)
    }

    // =========================================================================
    // 6. MediaLibrary Hierarchy & Auto Root
    // =========================================================================

    @Test
    fun testMediaLibraryHierarchyRootAndCategories() {
        assertEquals("root", PlaybackService.ROOT_ID)
        val expectedCategories = setOf(
            PlaybackService.CATEGORY_SONGS,
            PlaybackService.CATEGORY_ALBUMS,
            PlaybackService.CATEGORY_ARTISTS,
            PlaybackService.CATEGORY_PLAYLISTS,
        )
        assertEquals(4, expectedCategories.size)
        assertTrue(expectedCategories.contains("category_songs"))
        assertTrue(expectedCategories.contains("category_albums"))
        assertTrue(expectedCategories.contains("category_artists"))
        assertTrue(expectedCategories.contains("category_playlists"))
    }
}
