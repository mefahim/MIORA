package com.fahim.powerampclone.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fahim.powerampclone.data.MockMusicRepository
import com.fahim.powerampclone.data.MusicRepository
import com.fahim.powerampclone.model.RepeatMode
import com.fahim.powerampclone.model.Track
import com.fahim.powerampclone.playback.EqualizerController
import com.fahim.powerampclone.playback.EqualizerState
import com.fahim.powerampclone.playback.PlaybackManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Playback UI state matching Poweramp / MIORA player contracts.
 */
data class PlayerUiState(
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.OFF,
    val favorites: Set<Long> = emptySet(),
    val queueOrder: List<Long> = emptyList(),
) {
    val currentTrack: Track? get() = queue.getOrNull(currentIndex)
    val currentDurationMs: Long get() = if (durationMs > 0L) durationMs else (currentTrack?.durationMs ?: 0L)
    val upNext: List<Track> get() = if (queue.isEmpty()) emptyList() else queue.drop(currentIndex + 1)
}

class PlayerViewModel(
    private val repository: MusicRepository = MockMusicRepository(),
    private val playbackManager: PlaybackManager? = null,
) : ViewModel() {
    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = playbackManager?.state ?: _state.asStateFlow()
    private var ticker: Job? = null

    val equalizerController: EqualizerController = playbackManager?.equalizerController ?: EqualizerController()
    val equalizerState: StateFlow<EqualizerState> = equalizerController.state

    fun setEqualizerBandGain(bandIndex: Int, gainDb: Float) {
        equalizerController.setBandGain(bandIndex, gainDb)
    }

    fun setEqualizerPreset(presetName: String) {
        equalizerController.setPreset(presetName)
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        equalizerController.setEnabled(enabled)
    }

    fun resetEqualizer() {
        equalizerController.reset()
    }

    init {
        if (playbackManager == null) {
            val tracks = repository.tracks()
            _state.update {
                it.copy(
                    queue = tracks,
                    queueOrder = tracks.map { t -> t.id },
                    favorites = tracks.filter { t -> t.isFavorite }.map { t -> t.id }.toSet(),
                    durationMs = tracks.firstOrNull()?.durationMs ?: 0L,
                )
            }
        }
    }

    fun playTrack(track: Track, queue: List<Track>? = null) {
        if (playbackManager != null) {
            playbackManager.playTrack(track, queue)
            return
        }
        val effectiveQueue = when {
            queue != null && queue.isNotEmpty() -> queue
            _state.value.queue.any { it.id == track.id } -> _state.value.queue
            else -> listOf(track) + _state.value.queue
        }
        val index = effectiveQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        _state.update {
            it.copy(
                queue = effectiveQueue,
                queueOrder = effectiveQueue.map { t -> t.id },
                currentIndex = index,
                positionMs = 0L,
                durationMs = track.durationMs,
                isPlaying = true,
            )
        }
        startTicker()
    }

    fun togglePlayPause() {
        if (playbackManager != null) {
            playbackManager.togglePlayPause()
            return
        }
        val playing = !_state.value.isPlaying
        _state.update { it.copy(isPlaying = playing) }
        if (playing) startTicker() else ticker?.cancel()
    }

    fun next() {
        if (playbackManager != null) {
            playbackManager.next()
            return
        }
        val s = _state.value
        if (s.queue.isEmpty()) return
        val nextIndex = if (s.shuffle) {
            val candidates = s.queue.indices.filter { it != s.currentIndex }
            if (candidates.isNotEmpty()) candidates.random() else 0
        } else {
            (s.currentIndex + 1) % s.queue.size
        }
        val nextTrack = s.queue[nextIndex]
        _state.update {
            it.copy(
                currentIndex = nextIndex,
                positionMs = 0L,
                durationMs = nextTrack.durationMs,
                isPlaying = true,
            )
        }
        startTicker()
    }

    fun previous() {
        if (playbackManager != null) {
            playbackManager.previous()
            return
        }
        val s = _state.value
        if (s.queue.isEmpty()) return
        if (s.positionMs > 3000L) { seekTo(0L); return }
        val prevIndex = if (s.currentIndex == 0) s.queue.size - 1 else s.currentIndex - 1
        val prevTrack = s.queue[prevIndex]
        _state.update {
            it.copy(
                currentIndex = prevIndex,
                positionMs = 0L,
                durationMs = prevTrack.durationMs,
                isPlaying = true,
            )
        }
        startTicker()
    }

    fun seekTo(positionMs: Long) {
        if (playbackManager != null) {
            playbackManager.seekTo(positionMs)
            return
        }
        val dur = _state.value.currentDurationMs
        val clamped = if (dur > 0L) positionMs.coerceIn(0L, dur) else positionMs.coerceAtLeast(0L)
        _state.update { it.copy(positionMs = clamped) }
    }

    fun toggleShuffle() {
        if (playbackManager != null) {
            playbackManager.toggleShuffle()
            return
        }
        _state.update { it.copy(shuffle = !it.shuffle) }
    }

    fun cycleRepeat() {
        if (playbackManager != null) {
            playbackManager.cycleRepeat()
            return
        }
        _state.update {
            it.copy(repeat = when (it.repeat) { RepeatMode.OFF -> RepeatMode.ALL; RepeatMode.ALL -> RepeatMode.ONE; RepeatMode.ONE -> RepeatMode.OFF })
        }
    }

    fun toggleFavorite(trackId: Long) {
        if (playbackManager != null) {
            playbackManager.toggleFavorite(trackId)
            return
        }
        val isFav = repository.toggleFavorite(trackId)
        _state.update {
            val favs = if (isFav) it.favorites + trackId else it.favorites - trackId
            it.copy(favorites = favs)
        }
    }

    /** Move a queue item from [fromIndex] to [toIndex] (both indices into the full queue). */
    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        if (playbackManager != null) {
            playbackManager.moveQueueItem(fromIndex, toIndex)
            return
        }
        val s = _state.value
        if (fromIndex !in s.queue.indices || toIndex !in s.queue.indices || fromIndex == toIndex) return
        val currentTrackId = s.currentTrack?.id
        val mutable = s.queue.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        val newIndex = if (currentTrackId != null) {
            mutable.indexOfFirst { it.id == currentTrackId }.takeIf { it >= 0 } ?: 0
        } else {
            0
        }
        _state.update { it.copy(queue = mutable, queueOrder = mutable.map { t -> t.id }, currentIndex = newIndex) }
    }

    fun removeQueueItem(index: Int) {
        if (playbackManager != null) {
            playbackManager.removeQueueItem(index)
            return
        }
        val s = _state.value
        if (index !in s.queue.indices || s.queue.size <= 1) return
        val wasCurrent = (index == s.currentIndex)
        val currentId = s.currentTrack?.id
        val mutable = s.queue.toMutableList()
        mutable.removeAt(index)
        val newIndex = if (currentId != null && !wasCurrent) {
            mutable.indexOfFirst { it.id == currentId }.takeIf { it >= 0 } ?: 0
        } else {
            index.coerceAtMost(mutable.size - 1)
        }
        _state.update {
            it.copy(
                queue = mutable,
                queueOrder = mutable.map { t -> t.id },
                currentIndex = newIndex,
                positionMs = if (wasCurrent) 0L else s.positionMs,
                durationMs = if (wasCurrent) mutable[newIndex].durationMs else s.durationMs,
            )
        }
    }

    fun addToQueue(track: Track) {
        if (playbackManager != null) {
            playbackManager.addToQueue(track)
            return
        }
        _state.update { s ->
            val updated = s.queue + track
            s.copy(queue = updated, queueOrder = updated.map { it.id })
        }
    }

    fun playNext(track: Track) {
        if (playbackManager != null) {
            playbackManager.playNext(track)
            return
        }
        _state.update { s ->
            val insertIndex = (s.currentIndex + 1).coerceAtMost(s.queue.size)
            val mutable = s.queue.toMutableList()
            mutable.add(insertIndex, track)
            s.copy(queue = mutable, queueOrder = mutable.map { it.id })
        }
    }

    fun clearQueue() {
        if (playbackManager != null) {
            playbackManager.clearQueue()
            return
        }
        val s = _state.value
        val current = s.currentTrack
        val kept = if (current != null) listOf(current) else emptyList()
        _state.update { it.copy(queue = kept, queueOrder = kept.map { t -> t.id }, currentIndex = 0) }
    }

    fun shuffleQueue() {
        if (playbackManager != null) {
            playbackManager.shuffleQueue()
            return
        }
        val s = _state.value
        val current = s.currentTrack ?: return
        val rest = (s.queue - current).shuffled()
        val shuffled = listOf(current) + rest
        _state.update { it.copy(queue = shuffled, queueOrder = shuffled.map { t -> t.id }, currentIndex = 0) }
    }

    fun setQueue(tracks: List<Track>) {
        if (playbackManager != null) {
            playbackManager.setQueue(tracks)
            return
        }
        if (tracks.isEmpty()) return
        _state.update { current ->
            val currentId = current.currentTrack?.id
            val newIndex = if (currentId != null) tracks.indexOfFirst { it.id == currentId }.takeIf { it >= 0 } ?: 0 else 0
            current.copy(
                queue = tracks,
                queueOrder = tracks.map { it.id },
                currentIndex = newIndex,
                durationMs = tracks[newIndex].durationMs,
            )
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (true) {
                delay(500L)
                val s = _state.value
                if (!s.isPlaying) break
                val dur = s.currentDurationMs
                if (dur <= 0L) break
                val nextPos = s.positionMs + 500L
                if (nextPos >= dur) {
                    when (s.repeat) {
                        RepeatMode.ONE -> _state.update { it.copy(positionMs = 0L) }
                        RepeatMode.ALL -> next()
                        RepeatMode.OFF -> {
                            if (s.currentIndex >= s.queue.size - 1) _state.update { it.copy(isPlaying = false, positionMs = dur) } else next()
                        }
                    }
                } else {
                    _state.update { it.copy(positionMs = nextPos) }
                }
            }
        }
    }
}
