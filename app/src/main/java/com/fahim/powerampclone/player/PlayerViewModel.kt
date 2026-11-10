package com.fahim.powerampclone.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fahim.powerampclone.data.MockMusicRepository
import com.fahim.powerampclone.data.MusicRepository
import com.fahim.powerampclone.model.RepeatMode
import com.fahim.powerampclone.model.Track
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Simulated playback state. No audio is produced in Phase 2 — the ticker only
 * advances [PlayerUiState.positionMs] so every control behaves like a real player.
 * A future audio engine replaces [tick] with MediaSession callbacks; the UI contract stays identical.
 */
data class PlayerUiState(
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.OFF,
    val favorites: Set<Long> = emptySet(),
    val queueOrder: List<Long> = emptyList(),
) {
    val currentTrack: Track? get() = queue.getOrNull(currentIndex)
    val upNext: List<Track> get() = if (queue.isEmpty()) emptyList() else queue.drop(currentIndex + 1)
}

class PlayerViewModel(
    private val repository: MusicRepository = MockMusicRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()
    private var ticker: Job? = null

    init {
        val tracks = repository.tracks()
        _state.update {
            it.copy(
                queue = tracks,
                queueOrder = tracks.map { t -> t.id },
                favorites = tracks.filter { t -> t.isFavorite }.map { t -> t.id }.toSet(),
            )
        }
    }

    fun playTrack(track: Track, queue: List<Track>? = null) {
        val newQueue = queue ?: _state.value.queue
        val index = newQueue.indexOfFirst { it.id == track.id }.takeIf { it >= 0 } ?: 0
        _state.update { it.copy(queue = newQueue, queueOrder = newQueue.map { t -> t.id }, currentIndex = index, positionMs = 0L, isPlaying = true) }
        startTicker()
    }

    fun togglePlayPause() {
        val playing = !_state.value.isPlaying
        _state.update { it.copy(isPlaying = playing) }
        if (playing) startTicker() else ticker?.cancel()
    }

    fun next() {
        val s = _state.value
        if (s.queue.isEmpty()) return
        val nextIndex = if (s.shuffle) s.queue.indices.filter { it != s.currentIndex }.random() else (s.currentIndex + 1) % s.queue.size
        _state.update { it.copy(currentIndex = nextIndex, positionMs = 0L, isPlaying = true) }
        startTicker()
    }

    fun previous() {
        val s = _state.value
        if (s.queue.isEmpty()) return
        if (s.positionMs > 3000L) { seekTo(0L); return }
        val prevIndex = if (s.currentIndex == 0) s.queue.size - 1 else s.currentIndex - 1
        _state.update { it.copy(currentIndex = prevIndex, positionMs = 0L, isPlaying = true) }
        startTicker()
    }

    fun seekTo(positionMs: Long) {
        val track = _state.value.currentTrack ?: return
        _state.update { it.copy(positionMs = positionMs.coerceIn(0L, track.durationMs)) }
    }

    fun toggleShuffle() = _state.update { it.copy(shuffle = !it.shuffle) }

    fun cycleRepeat() {
        _state.update {
            it.copy(repeat = when (it.repeat) { RepeatMode.OFF -> RepeatMode.ALL; RepeatMode.ALL -> RepeatMode.ONE; RepeatMode.ONE -> RepeatMode.OFF })
        }
    }

    fun toggleFavorite(trackId: Long) {
        _state.update {
            val favs = if (trackId in it.favorites) it.favorites - trackId else it.favorites + trackId
            it.copy(favorites = favs)
        }
    }

    /** Move a queue item from [fromIndex] to [toIndex] (both indices into the full queue). */
    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val s = _state.value
        if (fromIndex !in s.queue.indices || toIndex !in s.queue.indices || fromIndex == toIndex) return
        val mutable = s.queue.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        // Keep the current track playing: adjust currentIndex to follow the moved item.
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
        _state.update { it.copy(queue = mutable, queueOrder = mutable.map { t -> t.id }, currentIndex = newIndex, positionMs = if (index == s.currentIndex) 0L else s.positionMs) }
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

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (true) {
                delay(500L)
                val s = _state.value
                if (!s.isPlaying) break
                val track = s.currentTrack ?: break
                val nextPos = s.positionMs + 500L
                if (nextPos >= track.durationMs) {
                    when (s.repeat) {
                        RepeatMode.ONE -> _state.update { it.copy(positionMs = 0L) }
                        RepeatMode.ALL -> next()
                        RepeatMode.OFF -> {
                            if (s.currentIndex >= s.queue.size - 1) _state.update { it.copy(isPlaying = false, positionMs = track.durationMs) } else next()
                        }
                    }
                } else {
                    _state.update { it.copy(positionMs = nextPos) }
                }
            }
        }
    }
}
