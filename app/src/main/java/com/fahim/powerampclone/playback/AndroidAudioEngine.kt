package com.fahim.powerampclone.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
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

class AndroidAudioEngine(
    private val context: Context,
) : AudioEngine {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    override val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    override val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    override val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    private var onCompletionListener: (() -> Unit)? = null
    private var mediaPlayer: MediaPlayer? = null
    private var positionTrackerJob: Job? = null

    private var isRealPlayback = false
    private var isPrepared = false
    private var isSeeking = false
    private var pausedDueToTransientFocusLoss = false
    private var isNoisyReceiverRegistered = false
    private var currentPlaybackToken = 0L

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                pausedDueToTransientFocusLoss = false
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (_isPlaying.value) {
                    pausedDueToTransientFocusLoss = true
                    pause()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mediaPlayer?.setVolume(0.2f, 0.2f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                mediaPlayer?.setVolume(1.0f, 1.0f)
                if (pausedDueToTransientFocusLoss) {
                    pausedDueToTransientFocusLoss = false
                    resume()
                }
            }
        }
    }

    private var audioFocusRequest: AudioFocusRequest? = null

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pause()
            }
        }
    }

    init {
        initMediaPlayer()
    }

    private fun initMediaPlayer() {
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {}

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(audioAttributes)
            setOnPreparedListener { mp ->
                if (currentPlaybackToken == 0L) return@setOnPreparedListener
                isPrepared = true
                val realDuration = mp.duration.toLong()
                if (realDuration > 0L) {
                    _durationMs.value = realDuration
                }
                if (requestAudioFocus()) {
                    mp.start()
                    _isPlaying.value = true
                    registerNoisyReceiver()
                    startPositionTracker()
                }
            }
            setOnCompletionListener {
                _isPlaying.value = false
                unregisterNoisyReceiver()
                onCompletionListener?.invoke()
            }
            setOnSeekCompleteListener {
                isSeeking = false
            }
            setOnErrorListener { _, what, extra ->
                _playbackError.value = "MediaPlayer error ($what, $extra)"
                _isPlaying.value = false
                isPrepared = false
                unregisterNoisyReceiver()
                try {
                    mediaPlayer?.reset()
                } catch (_: Exception) {
                    initMediaPlayer()
                }
                true
            }
        }
    }

    @Synchronized
    override fun play(track: Track) {
        val token = ++currentPlaybackToken
        positionTrackerJob?.cancel()
        _playbackError.value = null
        _currentPositionMs.value = 0L
        _durationMs.value = track.durationMs
        isPrepared = false
        isSeeking = false

        if (track.contentUriString.isNotBlank()) {
            try {
                isRealPlayback = true
                if (mediaPlayer == null) initMediaPlayer()
                val player = mediaPlayer ?: MediaPlayer().also { mediaPlayer = it }
                try {
                    player.reset()
                } catch (e: Exception) {
                    initMediaPlayer()
                }
                mediaPlayer?.apply {
                    setAudioAttributes(audioAttributes)
                    setDataSource(context, Uri.parse(track.contentUriString))
                    prepareAsync()
                }
                return
            } catch (e: Exception) {
                _playbackError.value = "Failed to load audio: ${e.message}"
                // Fall back to simulation mode below so playback stays responsive
            }
        }

        // Fallback or simulated playback for demo/sample tracks
        isRealPlayback = false
        if (requestAudioFocus()) {
            _isPlaying.value = true
            registerNoisyReceiver()
            startPositionTracker()
        }
    }

    override fun pause() {
        if (isRealPlayback && isPrepared) {
            try {
                mediaPlayer?.pause()
            } catch (_: Exception) {}
        }
        _isPlaying.value = false
        unregisterNoisyReceiver()
        positionTrackerJob?.cancel()
    }

    override fun resume() {
        if (_isPlaying.value) return
        if (requestAudioFocus()) {
            if (isRealPlayback && isPrepared) {
                try {
                    mediaPlayer?.start()
                } catch (_: Exception) {}
            }
            _isPlaying.value = true
            registerNoisyReceiver()
            startPositionTracker()
        }
    }

    override fun stop() {
        if (isRealPlayback && isPrepared) {
            try {
                mediaPlayer?.stop()
            } catch (_: Exception) {}
        }
        _isPlaying.value = false
        _currentPositionMs.value = 0L
        isPrepared = false
        abandonAudioFocus()
        unregisterNoisyReceiver()
        positionTrackerJob?.cancel()
    }

    override fun seekTo(positionMs: Long) {
        val bounded = positionMs.coerceIn(0L, _durationMs.value.coerceAtLeast(0L))
        _currentPositionMs.value = bounded
        if (isRealPlayback && isPrepared) {
            isSeeking = true
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mediaPlayer?.seekTo(bounded, MediaPlayer.SEEK_CLOSEST)
                } else {
                    mediaPlayer?.seekTo(bounded.toInt())
                }
            } catch (_: Exception) {
                isSeeking = false
            }
        }
    }

    override fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        try {
            mediaPlayer?.setVolume(clamped, clamped)
        } catch (_: Exception) {}
    }

    override fun setOnCompletionListener(listener: () -> Unit) {
        this.onCompletionListener = listener
    }

    override fun release() {
        stop()
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    private fun startPositionTracker() {
        positionTrackerJob?.cancel()
        positionTrackerJob = scope.launch {
            while (_isPlaying.value) {
                delay(250L)
                if (isRealPlayback && isPrepared) {
                    if (!isSeeking) {
                        try {
                            mediaPlayer?.let { mp ->
                                if (mp.isPlaying) {
                                    val pos = mp.currentPosition.toLong()
                                    _currentPositionMs.value = pos.coerceIn(0L, _durationMs.value)
                                }
                            }
                        } catch (_: Exception) {}
                    }
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

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = req
            audioManager.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN,
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    private fun registerNoisyReceiver() {
        if (!isNoisyReceiverRegistered) {
            try {
                context.registerReceiver(
                    noisyReceiver,
                    IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
                )
                isNoisyReceiverRegistered = true
            } catch (_: Exception) {}
        }
    }

    private fun unregisterNoisyReceiver() {
        if (isNoisyReceiverRegistered) {
            try {
                context.unregisterReceiver(noisyReceiver)
            } catch (_: Exception) {}
            isNoisyReceiverRegistered = false
        }
    }
}
