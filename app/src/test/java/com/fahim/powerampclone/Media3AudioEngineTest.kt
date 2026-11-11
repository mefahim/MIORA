package com.fahim.powerampclone

import com.fahim.powerampclone.data.MockMusicRepository
import com.fahim.powerampclone.model.Track
import com.fahim.powerampclone.playback.AudioEngine
import com.fahim.powerampclone.playback.EqualizerController
import com.fahim.powerampclone.playback.PlaybackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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
 * Unit and integration tests for Media3 playback engine migration, state machines,
 * AudioEngine contract extensions, and ForwardingPlayer command delegation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Media3AudioEngineTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Test fake implementing extended AudioEngine contract including Media3 primitives.
     */
    private class TestMedia3Engine : AudioEngine {
        private val _isPlaying = MutableStateFlow(false)
        override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

        private val _currentPositionMs = MutableStateFlow(0L)
        override val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

        private val _durationMs = MutableStateFlow(0L)
        override val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

        private val _playbackError = MutableStateFlow<String?>(null)
        override val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

        override val equalizerController = EqualizerController()
        override val audioSessionId: Int = 42

        private var completionListener: (() -> Unit)? = null
        var currentVolume: Float = 1.0f
        var released = false

        override fun play(track: Track) {
            _playbackError.value = null
            _currentPositionMs.value = 0L
            _durationMs.value = track.durationMs
            _isPlaying.value = true
        }

        override fun pause() {
            _isPlaying.value = false
        }

        override fun resume() {
            _isPlaying.value = true
        }

        override fun stop() {
            _isPlaying.value = false
            _currentPositionMs.value = 0L
        }

        override fun seekTo(positionMs: Long) {
            _currentPositionMs.value = positionMs.coerceIn(0L, _durationMs.value.coerceAtLeast(0L))
        }

        override fun setVolume(volume: Float) {
            this.currentVolume = volume.coerceIn(0f, 1f)
        }

        override fun release() {
            stop()
            equalizerController.detach()
            released = true
        }

        override fun setOnCompletionListener(listener: () -> Unit) {
            this.completionListener = listener
        }

        fun triggerCompletion() {
            _isPlaying.value = false
            completionListener?.invoke()
        }

        fun triggerError(message: String) {
            _playbackError.value = message
            _isPlaying.value = false
        }
    }

    @Test
    fun audioEngineDefaults_provideSafeNullPlayerAndZeroSessionId() {
        val minimalEngine = object : AudioEngine {
            override val isPlaying = MutableStateFlow(false)
            override val currentPositionMs = MutableStateFlow(0L)
            override val durationMs = MutableStateFlow(0L)
            override val playbackError = MutableStateFlow<String?>(null)

            override fun play(track: Track) {}
            override fun pause() {}
            override fun resume() {}
            override fun stop() {}
            override fun seekTo(positionMs: Long) {}
            override fun setVolume(volume: Float) {}
            override fun release() {}
            override fun setOnCompletionListener(listener: () -> Unit) {}
        }

        assertNull(minimalEngine.player)
        assertEquals(0, minimalEngine.audioSessionId)
        assertNull(minimalEngine.equalizerController)
    }

    @Test
    fun playbackManager_exposesAudioEngineInstanceAndPlayer() {
        val repo = MockMusicRepository()
        val engine = TestMedia3Engine()
        val manager = PlaybackManager(repo, engine)

        assertEquals(engine, manager.audioEngineInstance)
        assertNull(manager.player)
        assertNotNull(manager.equalizerController)
    }

    @Test
    fun playbackManager_pauseAndResume_controlEngineCorrectly() = runTest {
        val repo = MockMusicRepository()
        val engine = TestMedia3Engine()
        val manager = PlaybackManager(repo, engine)

        val track = repo.tracks().first()
        manager.playTrack(track)
        advanceUntilIdle()

        assertTrue(manager.state.value.isPlaying)
        assertTrue(engine.isPlaying.value)

        manager.pause()
        advanceUntilIdle()
        assertFalse(manager.state.value.isPlaying)
        assertFalse(engine.isPlaying.value)

        manager.resume()
        advanceUntilIdle()
        assertTrue(manager.state.value.isPlaying)
        assertTrue(engine.isPlaying.value)
    }

    @Test
    fun playbackManager_seekTo_boundsPositionCorrectly() = runTest {
        val repo = MockMusicRepository()
        val engine = TestMedia3Engine()
        val manager = PlaybackManager(repo, engine)

        val track = repo.tracks().first() // duration 225000 ms
        manager.playTrack(track)
        advanceUntilIdle()

        manager.seekTo(50000L)
        advanceUntilIdle()
        assertEquals(50000L, manager.state.value.positionMs)

        // Seeking beyond duration clamps to track duration
        manager.seekTo(9999999L)
        advanceUntilIdle()
        assertEquals(track.durationMs, manager.state.value.positionMs)

        // Negative seek clamps to 0
        manager.seekTo(-500L)
        advanceUntilIdle()
        assertEquals(0L, manager.state.value.positionMs)
    }

    @Test
    fun equalizer_attachesToAudioSessionIdFromEngine() {
        val engine = TestMedia3Engine()
        assertEquals(42, engine.audioSessionId)

        engine.equalizerController.attachAudioSession(engine.audioSessionId)
        // In headless JVM without audiofx hardware, isHardwareAttached safely evaluates to false without throwing
        assertNotNull(engine.equalizerController.state.value)
        assertEquals("Flat", engine.equalizerController.state.value.preset)

        engine.release()
        assertTrue(engine.released)
        assertFalse(engine.equalizerController.state.value.isHardwareAttached)
    }

    @Test
    fun testMedia3Engine_handlesVolumeClamping() {
        val engine = TestMedia3Engine()

        engine.setVolume(0.5f)
        assertEquals(0.5f, engine.currentVolume, 0.001f)

        engine.setVolume(2.0f)
        assertEquals(1.0f, engine.currentVolume, 0.001f)

        engine.setVolume(-0.5f)
        assertEquals(0.0f, engine.currentVolume, 0.001f)
    }

    @Test
    fun testMedia3Engine_handlesErrorState() {
        val engine = TestMedia3Engine()
        val track = MockMusicRepository().tracks().first()

        engine.play(track)
        assertTrue(engine.isPlaying.value)
        assertNull(engine.playbackError.value)

        engine.triggerError("Decoder initialization failed")
        assertFalse(engine.isPlaying.value)
        assertEquals("Decoder initialization failed", engine.playbackError.value)
    }

    @Test
    fun testMedia3Engine_handlesTrackCompletion() = runTest {
        val repo = MockMusicRepository()
        val engine = TestMedia3Engine()
        val manager = PlaybackManager(repo, engine)

        val tracks = repo.tracks()
        manager.playTrack(tracks[0], tracks)
        advanceUntilIdle()

        assertEquals(0, manager.state.value.currentIndex)

        // Trigger track completion -> should advance to index 1 (RepeatMode.OFF)
        engine.triggerCompletion()
        advanceUntilIdle()

        assertEquals(1, manager.state.value.currentIndex)
        assertEquals(tracks[1].id, manager.state.value.currentTrack?.id)
    }
}
