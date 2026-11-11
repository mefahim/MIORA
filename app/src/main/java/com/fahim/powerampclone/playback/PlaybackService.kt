package com.fahim.powerampclone.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.fahim.powerampclone.MainActivity
import com.fahim.powerampclone.R
import com.fahim.powerampclone.data.local.MediaStoreMusicRepository

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        createNotificationChannel()
        initMediaSession()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.notification_channel_desc)
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
        if (mediaSession != null) return

        val manager = getOrCreatePlaybackManager()
        val underlyingPlayer = manager.player ?: try {
            ExoPlayer.Builder(applicationContext).build()
        } catch (_: Throwable) {
            null
        }

        if (underlyingPlayer == null) return

        val forwardingPlayer = object : ForwardingPlayer(underlyingPlayer) {
            override fun seekToNext() {
                getOrCreatePlaybackManager().next()
            }

            override fun seekToPrevious() {
                getOrCreatePlaybackManager().previous()
            }

            override fun seekToNextMediaItem() {
                getOrCreatePlaybackManager().next()
            }

            override fun seekToPreviousMediaItem() {
                getOrCreatePlaybackManager().previous()
            }

            override fun play() {
                getOrCreatePlaybackManager().resume()
            }

            override fun pause() {
                getOrCreatePlaybackManager().pause()
            }

            override fun stop() {
                getOrCreatePlaybackManager().pause()
            }

            override fun seekTo(positionMs: Long) {
                getOrCreatePlaybackManager().seekTo(positionMs)
            }

            override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
                getOrCreatePlaybackManager().seekTo(positionMs)
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return when (command) {
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                    Player.COMMAND_PLAY_PAUSE,
                    Player.COMMAND_STOP,
                    Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM -> true
                    else -> super.isCommandAvailable(command)
                }
            }

            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .add(Player.COMMAND_PLAY_PAUSE)
                    .add(Player.COMMAND_STOP)
                    .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                    .build()
            }
        }

        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notificationProvider = DefaultMediaNotificationProvider.Builder(applicationContext)
            .setChannelId(CHANNEL_ID)
            .setChannelName(R.string.notification_channel_name)
            .setNotificationId(NOTIFICATION_ID)
            .build().apply {
                setSmallIcon(R.drawable.ic_notification_music)
            }
        setMediaNotificationProvider(notificationProvider)

        val session = MediaSession.Builder(this, forwardingPlayer)
            .setSessionActivity(sessionActivityIntent)
            .setId("MioraMediaSession")
            .setCallback(MediaSessionCallback())
            .build()

        mediaSession = session
        addSession(session)
    }

    private inner class MediaSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon().build()
            val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .add(Player.COMMAND_PLAY_PAUSE)
                .add(Player.COMMAND_STOP)
                .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setAvailablePlayerCommands(playerCommands)
                .build()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        if (mediaSession == null) {
            initMediaSession()
        }
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> getOrCreatePlaybackManager().togglePlayPause()
            ACTION_NEXT -> getOrCreatePlaybackManager().next()
            ACTION_PREVIOUS -> getOrCreatePlaybackManager().previous()
            ACTION_STOP -> {
                getOrCreatePlaybackManager().pause()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        isServiceRunning = false
        mediaSession?.run {
            removeSession(this)
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "miora_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY_PAUSE = "com.fahim.powerampclone.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.fahim.powerampclone.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.fahim.powerampclone.ACTION_PREVIOUS"
        const val ACTION_STOP = "com.fahim.powerampclone.ACTION_STOP"

        @Volatile
        private var isServiceRunning = false

        fun isRunning(): Boolean = isServiceRunning

        fun start(context: Context) {
            if (isServiceRunning) return
            try {
                val intent = Intent(context, PlaybackService::class.java)
                context.startService(intent)
            } catch (_: Throwable) {
                // Ignore if background restrictions prevent startService.
                // Media3's MediaSessionService automatically handles foreground transition when playback begins.
            }
        }

        fun stop(context: Context) {
            isServiceRunning = false
            try {
                context.stopService(Intent(context, PlaybackService::class.java))
            } catch (_: Throwable) {}
        }
    }
}
