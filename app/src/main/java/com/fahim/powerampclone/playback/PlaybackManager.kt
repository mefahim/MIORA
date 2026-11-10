package com.fahim.powerampclone.playback

import android.content.Context
import com.fahim.powerampclone.data.MusicRepository
import com.fahim.powerampclone.model.RepeatMode
import com.fahim.powerampclone.model.Track
import com.fahim.powerampclone.player.PlayerUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlaybackManager(
    private val repository: MusicRepository,
    private val audioEngine: AudioEngine,
    private val context: Context? = null,
) {
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    init {
        val initialTracks = repository.tracks()
        _state.update {
            it.copy(
                queue = initialTracks,
                queueOrder = initialTracks.map { t -> t.id },
                favorites = initialTracks.filter { t -> t.isFavorite }.map { t -> t.id }.toSet(),
            )
        }

        // Listen for track completion from audio engine
        audioEngine.setOnCompletionListener {
            handleTrackCompletion()
        }

        // Sync position from engine
        scope.launch {
            audioEngine.currentPositionMs.collect { pos ->
                _state.update { it.copy(positionMs = pos) }
            }
        }

        // Sync playing state from engine
        scope.launch {
            audioEngine.isPlaying.collect { playing ->
                _state.update { it.copy(isPlaying = playing) }
            }
        }
    }

    private fun handleTrackCompletion() {
        val s = _state.value
        when (s.repeat) {
            RepeatMode.ONE -> {
                s.currentTrack?.let { audioEngine.play(it) }
            }
            RepeatMode.ALL -> {
                next()
            }
            RepeatMode.OFF -> {
                if (s.currentIndex < s.queue.size - 1) {
                    next()
                } else {
                    _state.update { it.copy(isPlaying = false) }
                }
            }
        }
    }

    fun playTrack(track: Track, queue: List<Track>? = null) {
        val newQueue = queue ?: _state.value.queue
        val index = newQueue.indexOfFirst { it.id == track.id }.takeIf { it >= 0 } ?: 0
        _state.update {
            it.copy(
                queue = newQueue,
                queueOrder = newQueue.map { t -> t.id },
                currentIndex = index,
                positionMs = 0L,
                isPlaying = true,
            )
        }
        audioEngine.play(track)
        context?.let { PlaybackService.start(it) }
    }

    fun togglePlayPause() {
        val current = _state.value
        val track = current.currentTrack ?: return

        if (current.isPlaying) {
            _state.update { it.copy(isPlaying = false) }
            audioEngine.pause()
        } else {
            _state.update { it.copy(isPlaying = true) }
            if (audioEngine.currentPositionMs.value > 0L) {
                audioEngine.resume()
            } else {
                audioEngine.play(track)
            }
            context?.let { PlaybackService.start(it) }
        }
    }

    fun next() {
        val s = _state.value
        if (s.queue.isEmpty()) return
        val nextIndex = if (s.shuffle) {
            val candidates = s.queue.indices.filter { it != s.currentIndex }
            if (candidates.isNotEmpty()) candidates.random() else 0
        } else {
            (s.currentIndex + 1) % s.queue.size
        }

        val nextTrack = s.queue[nextIndex]
        _state.update { it.copy(currentIndex = nextIndex, positionMs = 0L, isPlaying = true) }
        audioEngine.play(nextTrack)
        context?.let { PlaybackService.start(it) }
    }

    fun previous() {
        val s = _state.value
        if (s.queue.isEmpty()) return
        if (s.positionMs > 3000L) {
            seekTo(0L)
            return
        }
        val prevIndex = if (s.currentIndex == 0) s.queue.size - 1 else s.currentIndex - 1
        val prevTrack = s.queue[prevIndex]
        _state.update { it.copy(currentIndex = prevIndex, positionMs = 0L, isPlaying = true) }
        audioEngine.play(prevTrack)
        context?.let { PlaybackService.start(it) }
    }

    fun seekTo(positionMs: Long) {
        val track = _state.value.currentTrack ?: return
        val clamped = positionMs.coerceIn(0L, track.durationMs)
        _state.update { it.copy(positionMs = clamped) }
        audioEngine.seekTo(clamped)
    }

    fun toggleShuffle() {
        _state.update { it.copy(shuffle = !it.shuffle) }
    }

    fun cycleRepeat() {
        _state.update {
            it.copy(
                repeat = when (it.repeat) {
                    RepeatMode.OFF -> RepeatMode.ALL
                    RepeatMode.ALL -> RepeatMode.ONE
                    RepeatMode.ONE -> RepeatMode.OFF
                },
            )
        }
    }

    fun toggleFavorite(trackId: Long) {
        _state.update {
            val favs = if (trackId in it.favorites) it.favorites - trackId else it.favorites + trackId
            it.copy(favorites = favs)
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val s = _state.value
        if (fromIndex !in s.queue.indices || toIndex !in s.queue.indices || fromIndex == toIndex) return
        val mutable = s.queue.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        var newIndex = s.currentIndex
        if (fromIndex == s.currentIndex) newIndex = toIndex
        else if (fromIndex < s.currentIndex && toIndex >= s.currentIndex) newIndex -= 1
        else if (fromIndex > s.currentIndex && toIndex <= s.currentIndex) newIndex += 1
        _state.update { it.copy(queue = mutable, queueOrder = mutable.map { t -> t.id }, currentIndex = newIndex) }
    }

    fun removeQueueItem(index: Int) {
        val s = _state.value
        if (index !in s.queue.indices || s.queue.size <= 1) return
        val mutable = s.queue.toMutableList()
        mutable.removeAt(index)
        var newIndex = s.currentIndex
        if (index < s.currentIndex) newIndex -= 1
        else if (index == s.currentIndex) newIndex = newIndex.coerceAtMost(mutable.size - 1)
        _state.update { it.copy(queue = mutable, queueOrder = mutable.map { t -> t.id }, currentIndex = newIndex) }
    }

    fun clearQueue() {
        val s = _state.value
        val current = s.currentTrack
        val kept = if (current != null) listOf(current) else emptyList()
        _state.update { it.copy(queue = kept, queueOrder = kept.map { t -> t.id }, currentIndex = 0) }
    }

    fun shuffleQueue() {
        val s = _state.value
        val current = s.currentTrack ?: return
        val rest = (s.queue - current).shuffled()
        val shuffled = listOf(current) + rest
        _state.update { it.copy(queue = shuffled, queueOrder = shuffled.map { t -> t.id }, currentIndex = 0) }
    }

    fun setQueue(newTracks: List<Track>) {
        if (newTracks.isEmpty()) return
        _state.update { current ->
            val currentId = current.currentTrack?.id
            val newIndex = if (currentId != null) newTracks.indexOfFirst { it.id == currentId }.takeIf { it >= 0 } ?: 0 else 0
            current.copy(
                queue = newTracks,
                queueOrder = newTracks.map { it.id },
                currentIndex = newIndex,
            )
        }
    }

    companion object {
        @Volatile
        private var instance: PlaybackManager? = null

        fun getInstance(context: Context, repository: MusicRepository): PlaybackManager {
            return instance ?: synchronized(this) {
                instance ?: PlaybackManager(
                    repository = repository,
                    audioEngine = AndroidAudioEngine(context.applicationContext),
                    context = context.applicationContext,
                ).also { instance = it }
            }
        }

        fun currentInstance(): PlaybackManager? = instance
    }
}
