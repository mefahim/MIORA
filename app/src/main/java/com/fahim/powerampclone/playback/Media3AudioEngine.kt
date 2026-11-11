package com.fahim.powerampclone.playback

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import com.fahim.powerampclone.artwork.ArtworkResolver
import com.fahim.powerampclone.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Modern ExoPlayer and Media3 audio playback engine implementing [AudioEngine].
 *
 * Supports hardware equalizer binding, audio focus handling (ducking, transient loss,
 * auto-resumption), audio becoming noisy handling (headphone disconnect pause),
 * and transparent simulated fallback for virtual tracks and test environments.
 */
open class Media3AudioEngine(
    private val context: Context,
) : AudioEngine {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    override val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    override val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    override val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    override val equalizerController = EqualizerController()

    private var onCompletionListener: (() -> Unit)? = null
    private var exoPlayer: ExoPlayer? = null
    private var positionTrackerJob: Job? = null

    private var isRealPlayback = false
    private var isPrepared = false
    private var currentPlaybackToken = 0L

    override val player: Player?
        get() = exoPlayer

    override val audioSessionId: Int
        get() = exoPlayer?.audioSessionId ?: 0

    init {
        initExoPlayer()
    }

    private fun initExoPlayer() {
        try {
            exoPlayer?.release()
        } catch (_: Throwable) {}

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()

            val player = ExoPlayer.Builder(context)
                .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
                .setHandleAudioBecomingNoisy(true)
                .build()

            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            isPrepared = true
                            val realDuration = player.duration
                            if (realDuration > 0L) {
                                _durationMs.value = realDuration
                            }
                        }
                        Player.STATE_ENDED -> {
                            _isPlaying.value = false
                            stopPositionTracker()
                            onCompletionListener?.invoke()
                        }
                        Player.STATE_IDLE -> {
                            isPrepared = false
                        }
                        Player.STATE_BUFFERING -> {}
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    if (isPlaying) {
                        startPositionTracker()
                    } else {
                        stopPositionTracker()
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    _playbackError.value = "ExoPlayer error: ${error.errorCodeName} (${error.errorCode})"
                    _isPlaying.value = false
                    isPrepared = false
                    stopPositionTracker()
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int,
                ) {
                    _currentPositionMs.value = newPosition.positionMs
                }
            })

            player.addAnalyticsListener(object : AnalyticsListener {
                override fun onAudioSessionIdChanged(
                    eventTime: AnalyticsListener.EventTime,
                    audioSessionId: Int,
                ) {
                    if (audioSessionId > 0) {
                        equalizerController.attachAudioSession(audioSessionId)
                    }
                }
            })

            exoPlayer = player
            val initialSessionId = player.audioSessionId
            if (initialSessionId > 0) {
                equalizerController.attachAudioSession(initialSessionId)
            }
        } catch (_: Throwable) {
            // Gracefully fall back to virtual simulation when AudioTrack / Looper is unavailable (e.g. headless JVM)
            exoPlayer = null
        }
    }

    @Synchronized
    override fun play(track: Track) {
        val token = ++currentPlaybackToken
        stopPositionTracker()
        _playbackError.value = null
        _currentPositionMs.value = 0L
        _durationMs.value = track.durationMs
        isPrepared = false

        if (track.contentUriString.isNotBlank() && exoPlayer != null) {
            try {
                isRealPlayback = true
                val metadataBuilder = MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)

                val cachedBytes = getCachedArtworkBytes(track)
                if (cachedBytes != null) {
                    metadataBuilder.setArtworkData(cachedBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                } else if (!track.artworkUriString.isNullOrBlank()) {
                    try {
                        metadataBuilder.setArtworkUri(Uri.parse(track.artworkUriString))
                    } catch (_: Throwable) {}
                }

                val mediaItem = MediaItem.Builder()
                    .setUri(Uri.parse(track.contentUriString))
                    .setMediaId(track.id.toString())
                    .setMediaMetadata(metadataBuilder.build())
                    .build()

                exoPlayer?.run {
                    setMediaItem(mediaItem)
                    prepare()
                    play()
                }

                if (cachedBytes == null) {
                    loadArtworkAsync(track, token)
                }

                return
            } catch (e: Exception) {
                _playbackError.value = "Failed to load audio: ${e.message}"
                // Fall through to simulation mode below
            }
        }

        // Fallback or simulated playback for demo/mock tracks without physical media files
        isRealPlayback = false
        _isPlaying.value = true
        startPositionTracker()
    }

    override fun pause() {
        if (isRealPlayback && exoPlayer != null) {
            try {
                exoPlayer?.pause()
            } catch (_: Throwable) {}
        }
        _isPlaying.value = false
        stopPositionTracker()
    }

    override fun resume() {
        if (_isPlaying.value) return
        if (isRealPlayback && exoPlayer != null) {
            try {
                exoPlayer?.play()
            } catch (_: Throwable) {}
        }
        _isPlaying.value = true
        startPositionTracker()
    }

    override fun stop() {
        if (isRealPlayback && exoPlayer != null) {
            try {
                exoPlayer?.stop()
            } catch (_: Throwable) {}
        }
        _isPlaying.value = false
        _currentPositionMs.value = 0L
        isPrepared = false
        stopPositionTracker()
    }

    override fun seekTo(positionMs: Long) {
        val bounded = positionMs.coerceIn(0L, _durationMs.value.coerceAtLeast(0L))
        _currentPositionMs.value = bounded
        if (isRealPlayback && exoPlayer != null) {
            try {
                exoPlayer?.seekTo(bounded)
            } catch (_: Throwable) {}
        }
    }

    override fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        try {
            exoPlayer?.volume = clamped
        } catch (_: Throwable) {}
    }

    override fun setOnCompletionListener(listener: () -> Unit) {
        this.onCompletionListener = listener
    }

    override fun release() {
        stop()
        equalizerController.detach()
        try {
            exoPlayer?.release()
        } catch (_: Throwable) {}
        exoPlayer = null
    }

    private fun startPositionTracker() {
        positionTrackerJob?.cancel()
        positionTrackerJob = scope.launch {
            while (_isPlaying.value) {
                delay(250L)
                if (isRealPlayback && exoPlayer != null) {
                    try {
                        val pos = exoPlayer?.currentPosition ?: 0L
                        _currentPositionMs.value = pos.coerceIn(0L, _durationMs.value.coerceAtLeast(0L))
                    } catch (_: Throwable) {}
                } else if (!isRealPlayback) {
                    val next = _currentPositionMs.value + 250L
                    if (next >= _durationMs.value && _durationMs.value > 0L) {
                        _currentPositionMs.value = _durationMs.value
                        _isPlaying.value = false
                        onCompletionListener?.invoke()
                        break
                    } else {
                        _currentPositionMs.value = next
                    }
                }
            }
        }
    }

    private fun stopPositionTracker() {
        positionTrackerJob?.cancel()
        positionTrackerJob = null
    }

    private fun getCachedArtworkBytes(track: Track): ByteArray? {
        val cacheKey = when {
            track.id > 0L -> "track_${track.id}"
            track.albumId > 0L -> "album_${track.albumId}"
            else -> null
        } ?: return null
        val cached = ArtworkResolver.getCached(cacheKey) ?: return null
        if (cached.isRecycled) return null
        return try {
            val stream = ByteArrayOutputStream()
            cached.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            stream.toByteArray()
        } catch (_: Throwable) {
            null
        }
    }

    private fun loadArtworkAsync(track: Track, token: Long) {
        scope.launch(Dispatchers.IO) {
            val bitmap = try {
                ArtworkResolver.resolveArtwork(context, track, 320)
            } catch (_: Throwable) {
                null
            }
            if (bitmap != null && !bitmap.isRecycled && currentPlaybackToken == token) {
                val stream = ByteArrayOutputStream()
                val bytes = try {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                    stream.toByteArray()
                } catch (_: Throwable) {
                    null
                }
                if (bytes != null && currentPlaybackToken == token) {
                    withContext(Dispatchers.Main) {
                        if (currentPlaybackToken == token && exoPlayer != null) {
                            val player = exoPlayer ?: return@withContext
                            val currentItem = player.currentMediaItem
                            if (currentItem != null && currentItem.mediaId == track.id.toString()) {
                                val updatedMetadata = currentItem.mediaMetadata.buildUpon()
                                    .setArtworkData(bytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                                    .build()
                                player.playlistMetadata = updatedMetadata
                            }
                        }
                    }
                }
            }
        }
    }
}
