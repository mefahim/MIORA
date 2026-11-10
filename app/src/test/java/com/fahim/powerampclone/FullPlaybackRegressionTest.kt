package com.fahim.powerampclone

import com.fahim.powerampclone.data.MockData
import com.fahim.powerampclone.data.MockMusicRepository
import com.fahim.powerampclone.data.local.LocalMediaAggregator
import com.fahim.powerampclone.model.RepeatMode
import com.fahim.powerampclone.model.Track
import com.fahim.powerampclone.playback.PlaybackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FullPlaybackRegressionTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeEngine: FakeAudioEngine
    private lateinit var manager: PlaybackManager
    private val repo = MockMusicRepository()
    private val tracks = repo.tracks()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeEngine = FakeAudioEngine()
        manager = PlaybackManager(repo, fakeEngine)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =========================================================================
    // 1. Queue and Track Selection
    // =========================================================================

    @Test
    fun `selecting first track sets exact track and index 0`() {
        val first = tracks.first()
        manager.playTrack(first, tracks)

        assertEquals(0, manager.state.value.currentIndex)
        assertEquals(first.id, manager.state.value.currentTrack?.id)
        assertEquals(first.title, manager.state.value.currentTrack?.title)
        assertTrue(manager.state.value.isPlaying)
        assertEquals(0L, manager.state.value.positionMs)
    }

    @Test
    fun `selecting middle track sets exact track and index without resetting to 0`() {
        val middleIndex = tracks.size / 2 // index 10
        val middle = tracks[middleIndex]
        manager.playTrack(middle, tracks)

        assertEquals(middleIndex, manager.state.value.currentIndex)
        assertEquals(middle.id, manager.state.value.currentTrack?.id)
        assertEquals(middle.title, manager.state.value.currentTrack?.title)
        assertTrue(manager.state.value.isPlaying)
    }

    @Test
    fun `selecting last track sets exact track and last index`() {
        val lastIndex = tracks.size - 1
        val last = tracks[lastIndex]
        manager.playTrack(last, tracks)

        assertEquals(lastIndex, manager.state.value.currentIndex)
        assertEquals(last.id, manager.state.value.currentTrack?.id)
        assertEquals(last.title, manager.state.value.currentTrack?.title)
    }

    @Test
    fun `selecting track from sorted list updates active queue to that sorted list`() {
        val sortedByTitle = tracks.sortedBy { it.title.lowercase() }
        val target = sortedByTitle[5]

        manager.playTrack(target, sortedByTitle)

        assertEquals(5, manager.state.value.currentIndex)
        assertEquals(target.id, manager.state.value.currentTrack?.id)
        assertEquals(sortedByTitle.map { it.id }, manager.state.value.queue.map { it.id })

        // Next follows the sorted list!
        manager.next()
        assertEquals(6, manager.state.value.currentIndex)
        assertEquals(sortedByTitle[6].id, manager.state.value.currentTrack?.id)
    }

    @Test
    fun `next and previous sequentially traverse active queue`() {
        manager.playTrack(tracks[2], tracks)
        assertEquals(2, manager.state.value.currentIndex)

        manager.next()
        assertEquals(3, manager.state.value.currentIndex)
        assertEquals(tracks[3].id, manager.state.value.currentTrack?.id)

        manager.next()
        assertEquals(4, manager.state.value.currentIndex)
        assertEquals(tracks[4].id, manager.state.value.currentTrack?.id)

        manager.previous()
        assertEquals(3, manager.state.value.currentIndex)
        assertEquals(tracks[3].id, manager.state.value.currentTrack?.id)

        manager.previous()
        assertEquals(2, manager.state.value.currentIndex)
        assertEquals(tracks[2].id, manager.state.value.currentTrack?.id)
    }

    @Test
    fun `rapid repeated next and previous calls maintain correct index and track identity`() {
        manager.playTrack(tracks[0], tracks)

        // Rapid next 5 times
        repeat(5) { manager.next() }
        assertEquals(5, manager.state.value.currentIndex)
        assertEquals(tracks[5].id, manager.state.value.currentTrack?.id)

        // Rapid prev 3 times
        repeat(3) { manager.previous() }
        assertEquals(2, manager.state.value.currentIndex)
        assertEquals(tracks[2].id, manager.state.value.currentTrack?.id)
    }

    @Test
    fun `previous at index 0 wraps to last track when under 3s`() {
        manager.playTrack(tracks[0], tracks)
        assertEquals(0, manager.state.value.currentIndex)

        manager.previous()
        assertEquals(tracks.size - 1, manager.state.value.currentIndex)
        assertEquals(tracks.last().id, manager.state.value.currentTrack?.id)
    }

    @Test
    fun `repeat modes behave correctly on natural track completion`() {
        // 1. Repeat ONE: re-triggers same track
        manager.cycleRepeat() // ALL
        manager.cycleRepeat() // ONE
        manager.playTrack(tracks[4], tracks)
        fakeEngine.completionListener?.invoke()
        assertEquals(4, manager.state.value.currentIndex)
        assertEquals(tracks[4].id, fakeEngine.playedTrack?.id)

        // 2. Repeat ALL: wraps from end to beginning
        manager.cycleRepeat() // OFF
        manager.cycleRepeat() // ALL
        manager.playTrack(tracks.last(), tracks)
        fakeEngine.completionListener?.invoke()
        assertEquals(0, manager.state.value.currentIndex)
        assertEquals(tracks[0].id, manager.state.value.currentTrack?.id)

        // 3. Repeat OFF: stops at end of queue
        manager.cycleRepeat() // ONE
        manager.cycleRepeat() // OFF
        manager.playTrack(tracks.last(), tracks)
        fakeEngine.completionListener?.invoke()
        assertFalse(manager.state.value.isPlaying)
    }

    @Test
    fun `shuffle mode picks random track from queue without losing track identity`() {
        manager.playTrack(tracks[0], tracks)
        manager.toggleShuffle()
        assertTrue(manager.state.value.shuffle)

        manager.next()
        val current = manager.state.value.currentTrack
        assertNotNull(current)
        assertTrue(tracks.any { it.id == current?.id })
    }

    @Test
    fun `moveQueueItem preserves current playing track identity`() {
        val playingTrack = tracks[2]
        manager.playTrack(playingTrack, tracks)
        assertEquals(2, manager.state.value.currentIndex)

        // Move item at 2 to 5
        manager.moveQueueItem(2, 5)

        assertEquals(5, manager.state.value.currentIndex)
        assertEquals(playingTrack.id, manager.state.value.currentTrack?.id)
        assertEquals(playingTrack.id, manager.state.value.queue[5].id)
    }

    @Test
    fun `removing current track advances to next item or stops if queue empty`() {
        val queue = listOf(tracks[0], tracks[1], tracks[2])
        manager.playTrack(queue[1], queue)
        assertEquals(1, manager.state.value.currentIndex)

        // Remove track 1
        manager.removeQueueItem(1)

        // Index remains 1 (which is now tracks[2])
        assertEquals(1, manager.state.value.currentIndex)
        assertEquals(tracks[2].id, manager.state.value.currentTrack?.id)
    }

    // =========================================================================
    // 2. Playback State & Progress Synchronization
    // =========================================================================

    @Test
    fun `playTrack resets position to 0 and updates duration`() {
        manager.playTrack(tracks[0])
        manager.seekTo(50000L)
        assertEquals(50000L, manager.state.value.positionMs)

        // Switch to track 1
        manager.playTrack(tracks[1])
        assertEquals(0L, manager.state.value.positionMs)
        assertEquals(tracks[1].durationMs, manager.state.value.currentDurationMs)
    }

    @Test
    fun `seek clamps to duration and updates position`() {
        val track = tracks[0] // 232_000 ms
        manager.playTrack(track)

        manager.seekTo(100000L)
        assertEquals(100000L, manager.state.value.positionMs)

        manager.seekTo(99999999L)
        assertEquals(track.durationMs, manager.state.value.positionMs)

        manager.seekTo(-500L)
        assertEquals(0L, manager.state.value.positionMs)
    }

    @Test
    fun `real duration update from engine updates state and track in queue`() {
        val track = tracks[0].copy(durationMs = 1000L)
        manager.playTrack(track)

        // Simulate engine reporting true media duration from hardware decoder
        fakeEngine._durationMs.value = 245000L
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(245000L, manager.state.value.durationMs)
        assertEquals(245000L, manager.state.value.currentDurationMs)
        assertEquals(245000L, manager.state.value.currentTrack?.durationMs)
    }

    @Test
    fun `playback error halts playing state safely`() {
        manager.playTrack(tracks[0])
        assertTrue(manager.state.value.isPlaying)

        fakeEngine._playbackError.value = "Decoder error"
        fakeEngine._isPlaying.value = false
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(manager.state.value.isPlaying)
    }

    // =========================================================================
    // 3. Library & Media Aggregator Regression
    // =========================================================================

    @Test
    fun `aggregator deduplicates and handles tracks cleanly`() {
        val t1 = Track(1L, "Song", "Artist", "Album", 10L, 20L, 180000L)
        val t2 = Track(2L, "Song", "Artist", "Album", 10L, 20L, 200000L) // same title, different track

        val aggregated = LocalMediaAggregator.aggregate(listOf(t1, t2))
        assertEquals(2, aggregated.tracks.size)
        assertEquals(1, aggregated.albums.size)
        assertEquals(listOf(1L, 2L), aggregated.albums.first().trackIds)
    }

    @Test
    fun `empty library returns safe empty state without crashing`() {
        val emptyAgg = LocalMediaAggregator.aggregate(emptyList())
        assertTrue(emptyAgg.tracks.isEmpty())
        assertTrue(emptyAgg.albums.isEmpty())
        assertTrue(emptyAgg.artists.isEmpty())
    }
}
