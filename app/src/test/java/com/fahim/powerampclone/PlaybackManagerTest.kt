package com.fahim.powerampclone

import com.fahim.powerampclone.data.MockMusicRepository
import com.fahim.powerampclone.model.RepeatMode
import com.fahim.powerampclone.model.Track
import com.fahim.powerampclone.playback.AudioEngine
import com.fahim.powerampclone.playback.PlaybackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeAudioEngine : AudioEngine {
    val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying

    val _currentPositionMs = MutableStateFlow(0L)
    override val currentPositionMs: StateFlow<Long> = _currentPositionMs

    val _durationMs = MutableStateFlow(0L)
    override val durationMs: StateFlow<Long> = _durationMs

    val _playbackError = MutableStateFlow<String?>(null)
    override val playbackError: StateFlow<String?> = _playbackError

    var playedTrack: Track? = null
    var paused = false
    var completionListener: (() -> Unit)? = null

    override fun play(track: Track) {
        playedTrack = track
        _durationMs.value = track.durationMs
        _currentPositionMs.value = 0L
        _isPlaying.value = true
        paused = false
    }

    override fun pause() {
        _isPlaying.value = false
        paused = true
    }

    override fun resume() {
        _isPlaying.value = true
        paused = false
    }

    override fun stop() {
        _isPlaying.value = false
        _currentPositionMs.value = 0L
    }

    override fun seekTo(positionMs: Long) {
        _currentPositionMs.value = positionMs
    }

    override fun setVolume(volume: Float) {}

    override fun release() {
        stop()
    }

    override fun setOnCompletionListener(listener: () -> Unit) {
        completionListener = listener
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackManagerTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeEngine: FakeAudioEngine
    private lateinit var manager: PlaybackManager
    private val repo = MockMusicRepository()

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

    @Test
    fun `initial state loads repository tracks`() {
        assertEquals(20, manager.state.value.queue.size)
        assertEquals("Sky Dreams", manager.state.value.currentTrack?.title)
        assertFalse(manager.state.value.isPlaying)
    }

    @Test
    fun `playTrack delegates to audioEngine and updates state`() {
        val track = repo.tracks()[3]
        manager.playTrack(track)

        assertEquals("Ocean Breeze", manager.state.value.currentTrack?.title)
        assertTrue(manager.state.value.isPlaying)
        assertEquals(track, fakeEngine.playedTrack)
    }

    @Test
    fun `togglePlayPause pauses and resumes engine`() {
        manager.playTrack(repo.tracks()[0])
        assertTrue(manager.state.value.isPlaying)

        manager.togglePlayPause()
        assertTrue(fakeEngine.paused)

        manager.togglePlayPause()
        assertFalse(fakeEngine.paused)
    }

    @Test
    fun `next advances to next track and plays it`() {
        manager.next()
        assertEquals("Better Tomorrow", manager.state.value.currentTrack?.title)
        assertEquals("Better Tomorrow", fakeEngine.playedTrack?.title)
    }

    @Test
    fun `previous restarts current track if elapsed over 3s`() {
        manager.playTrack(repo.tracks()[1])
        fakeEngine.seekTo(4500L)
        manager.seekTo(4500L)

        manager.previous()
        // Should seek to 0 on same track
        assertEquals(1, manager.state.value.currentIndex)
        assertEquals(0L, manager.state.value.positionMs)
    }

    @Test
    fun `cycleRepeat cycles through OFF, ALL, and ONE`() {
        assertEquals(RepeatMode.OFF, manager.state.value.repeat)
        manager.cycleRepeat()
        assertEquals(RepeatMode.ALL, manager.state.value.repeat)
        manager.cycleRepeat()
        assertEquals(RepeatMode.ONE, manager.state.value.repeat)
        manager.cycleRepeat()
        assertEquals(RepeatMode.OFF, manager.state.value.repeat)
    }

    @Test
    fun `track completion in RepeatMode ONE replays same track`() {
        manager.cycleRepeat() // ALL
        manager.cycleRepeat() // ONE
        manager.playTrack(repo.tracks()[2])

        fakeEngine.completionListener?.invoke()

        assertEquals(2, manager.state.value.currentIndex)
        assertEquals(repo.tracks()[2], fakeEngine.playedTrack)
    }

    @Test
    fun `moveQueueItem reorders items and follows current playing index`() {
        val firstTrack = manager.state.value.queue[0]
        manager.moveQueueItem(0, 3)

        assertEquals(3, manager.state.value.currentIndex)
        assertEquals(firstTrack.id, manager.state.value.queue[3].id)
    }

    @Test
    fun `toggleFavorite updates favorites set`() {
        val trackId = repo.tracks()[0].id
        val wasFav = trackId in manager.state.value.favorites

        manager.toggleFavorite(trackId)
        if (wasFav) {
            assertFalse(trackId in manager.state.value.favorites)
        } else {
            assertTrue(trackId in manager.state.value.favorites)
        }
    }
}
