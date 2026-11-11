package com.fahim.powerampclone.playback

import androidx.media3.common.Player
import com.fahim.powerampclone.model.Track
import kotlinx.coroutines.flow.StateFlow

/**
 * Core audio playback engine abstraction.
 */
interface AudioEngine {
    val isPlaying: StateFlow<Boolean>
    val currentPositionMs: StateFlow<Long>
    val durationMs: StateFlow<Long>
    val playbackError: StateFlow<String?>
    val equalizerController: EqualizerController? get() = null
    val player: Player? get() = null
    val audioSessionId: Int get() = 0

    fun play(track: Track)
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(positionMs: Long)
    fun setVolume(volume: Float)
    fun release()
    fun setOnCompletionListener(listener: () -> Unit)
}
