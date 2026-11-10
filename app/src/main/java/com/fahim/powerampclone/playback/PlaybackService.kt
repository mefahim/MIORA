package com.fahim.powerampclone.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import android.util.Size
import com.fahim.powerampclone.MainActivity
import com.fahim.powerampclone.R
import com.fahim.powerampclone.artwork.ArtworkResolver
import com.fahim.powerampclone.data.local.MediaStoreMusicRepository
import com.fahim.powerampclone.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.abs

class PlaybackService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var stateObserverJob: Job? = null
    private lateinit var mediaSession: MediaSession
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        initMediaSession()
        observePlaybackManager()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MIORA Playback",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Music playback controls and information"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getOrCreatePlaybackManager(): PlaybackManager {
        return PlaybackManager.currentInstance()
            ?: PlaybackManager.getInstance(applicationContext, MediaStoreMusicRepository(applicationContext))
    }

    private fun initMediaSession() {
        mediaSession = MediaSession(this, "MioraMediaSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    getOrCreatePlaybackManager().togglePlayPause()
                }

                override fun onPause() {
                    getOrCreatePlaybackManager().togglePlayPause()
                }

                override fun onSkipToNext() {
                    getOrCreatePlaybackManager().next()
                }

                override fun onSkipToPrevious() {
                    getOrCreatePlaybackManager().previous()
                }

                override fun onSeekTo(pos: Long) {
                    getOrCreatePlaybackManager().seekTo(pos)
                }

                override fun onStop() {
                    getOrCreatePlaybackManager().togglePlayPause()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            })
            isActive = true
        }
    }

    private fun observePlaybackManager() {
        stateObserverJob?.cancel()
        val manager = getOrCreatePlaybackManager()
        stateObserverJob = scope.launch {
            manager.state.collect { state ->
                val track = state.currentTrack
                val duration = if (state.durationMs > 0L) state.durationMs else (track?.durationMs ?: 0L)
                updateMediaSessionState(state.isPlaying, state.positionMs, duration)
                if (track != null) {
                    val artworkBitmap = getTrackArtworkBitmap(track)
                    updateMediaSessionMetadata(track, duration, artworkBitmap)
                    val notification = buildNotification(track, state.isPlaying, artworkBitmap)
                    if (state.isPlaying) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                        } else {
                            startForeground(NOTIFICATION_ID, notification)
                        }
                    } else {
                        notificationManager.notify(NOTIFICATION_ID, notification)
                    }
                }
            }
        }
    }

    private fun updateMediaSessionState(isPlaying: Boolean, positionMs: Long, durationMs: Long) {
        val stateBuilder = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                    PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_PLAY_PAUSE or
                    PlaybackState.ACTION_SKIP_TO_NEXT or
                    PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackState.ACTION_SEEK_TO or
                    PlaybackState.ACTION_STOP,
            )
            .setState(
                if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                positionMs,
                1.0f,
            )
        mediaSession.setPlaybackState(stateBuilder.build())
    }

    private fun updateMediaSessionMetadata(track: Track, durationMs: Long, artwork: Bitmap?) {
        val metadataBuilder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, track.title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, track.album)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, durationMs)

        if (artwork != null) {
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artwork)
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, artwork)
        }
        mediaSession.setMetadata(metadataBuilder.build())
    }

    private fun getTrackArtworkBitmap(track: Track): Bitmap {
        val resolved = ArtworkResolver.resolveArtwork(this, track, targetSize = 320)
        if (resolved != null) return resolved
        return createFallbackArtworkBitmap(track)
    }

    private fun createFallbackArtworkBitmap(track: Track): Bitmap {
        val size = 256
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val h = abs(track.artworkSeed.hashCode()) % 360
        val hsv = floatArrayOf(h.toFloat(), 0.65f, 0.35f)
        paint.color = Color.HSVToColor(hsv)
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        paint.color = Color.argb(45, 255, 255, 255)
        canvas.drawCircle((size / 2).toFloat(), (size / 2).toFloat(), (size * 0.35f), paint)

        paint.color = Color.WHITE
        paint.textSize = 80f
        paint.textAlign = Paint.Align.CENTER
        val letter = track.title.trim().take(1).uppercase().ifEmpty { "M" }
        val yPos = (size / 2 - (paint.descent() + paint.ascent()) / 2)
        canvas.drawText(letter, (size / 2).toFloat(), yPos, paint)

        return bitmap
    }

    private fun buildNotification(track: Track, isPlaying: Boolean, artwork: Bitmap): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val prevIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, PlaybackService::class.java).apply { action = ACTION_PREVIOUS },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val playPauseIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, PlaybackService::class.java).apply { action = ACTION_PLAY_PAUSE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val nextIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, PlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val playPauseIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setContentTitle(track.title)
            .setContentText("${track.artist} • ${track.album}")
            .setSmallIcon(R.drawable.ic_notification_music)
            .setLargeIcon(artwork)
            .setColor(0xFF0F766E.toInt())
            .setColorized(true)
            .setContentIntent(contentIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2),
            )
            .addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_media_previous),
                    "Previous",
                    prevIntent,
                ).build(),
            )
            .addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(this, playPauseIcon),
                    if (isPlaying) "Pause" else "Play",
                    playPauseIntent,
                ).build(),
            )
            .addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_media_next),
                    "Next",
                    nextIntent,
                ).build(),
            )
            .setOngoing(isPlaying)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> getOrCreatePlaybackManager().togglePlayPause()
            ACTION_NEXT -> getOrCreatePlaybackManager().next()
            ACTION_PREVIOUS -> getOrCreatePlaybackManager().previous()
            ACTION_STOP -> {
                getOrCreatePlaybackManager().togglePlayPause()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stateObserverJob?.cancel()
        mediaSession.isActive = false
        mediaSession.release()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "miora_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY_PAUSE = "com.fahim.powerampclone.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.fahim.powerampclone.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.fahim.powerampclone.ACTION_PREVIOUS"
        const val ACTION_STOP = "com.fahim.powerampclone.ACTION_STOP"

        fun start(context: Context) {
            val intent = Intent(context, PlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PlaybackService::class.java))
        }
    }
}
