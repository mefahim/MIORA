package com.fahim.powerampclone.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.fahim.powerampclone.MainActivity
import com.fahim.powerampclone.R
import com.fahim.powerampclone.data.MockMusicRepository
import com.fahim.powerampclone.data.MusicRepository
import com.fahim.powerampclone.data.local.MediaStoreMusicRepository
import com.fahim.powerampclone.model.Track
import com.fahim.powerampclone.widget.MioraAppWidgetProvider
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Core MediaLibraryService for MIORA.
 *
 * Capabilities:
 * - Extends [MediaLibraryService] to expose the music library hierarchy for Android Auto and MediaBrowser callers.
 * - Bridges ExoPlayer with [PlaybackManager] via [ForwardingPlayer].
 * - Synchronizes real-time playback state and track metadata with [MioraAppWidgetProvider].
 * - Supports browsing Tracks, Albums, Artists, and Playlists.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {

    private var mediaLibrarySession: MediaLibrarySession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var stateObserverJob: Job? = null
    private var cachedRepository: MusicRepository? = null

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        createNotificationChannel()
        initMediaSession()
        observePlaybackStateForWidgets()
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

    private fun getOrCreateRepository(): MusicRepository {
        val existing = cachedRepository
        if (existing != null) return existing

        val repo = try {
            MediaStoreMusicRepository(applicationContext)
        } catch (_: Throwable) {
            MockMusicRepository()
        }
        cachedRepository = repo
        return repo
    }

    private fun getOrCreatePlaybackManager(): PlaybackManager {
        return PlaybackManager.currentInstance()
            ?: PlaybackManager.getInstance(applicationContext, getOrCreateRepository())
    }

    private fun observePlaybackStateForWidgets() {
        val manager = getOrCreatePlaybackManager()
        stateObserverJob?.cancel()
        stateObserverJob = serviceScope.launch {
            manager.state.collect { state ->
                MioraAppWidgetProvider.updateWidgets(
                    context = applicationContext,
                    track = state.currentTrack,
                    isPlaying = state.isPlaying,
                    artwork = null,
                )
            }
        }
    }

    private fun initMediaSession() {
        if (mediaLibrarySession != null) return

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

        val session = MediaLibrarySession.Builder(this, forwardingPlayer, MediaLibraryCallback())
            .setSessionActivity(sessionActivityIntent)
            .setId("MioraMediaLibrarySession")
            .build()

        mediaLibrarySession = session
        addSession(session)
    }

    private inner class MediaLibraryCallback : MediaLibrarySession.Callback {
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

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("MIORA Library")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build(),
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val repository = getOrCreateRepository()
            val items: List<MediaItem> = when (parentId) {
                ROOT_ID -> {
                    listOf(
                        createCategoryItem(CATEGORY_SONGS, "Tracks"),
                        createCategoryItem(CATEGORY_ALBUMS, "Albums"),
                        createCategoryItem(CATEGORY_ARTISTS, "Artists"),
                        createCategoryItem(CATEGORY_PLAYLISTS, "Playlists"),
                    )
                }
                CATEGORY_SONGS -> {
                    repository.tracks().map { mapTrackToMediaItem(it) }
                }
                CATEGORY_ALBUMS -> {
                    repository.albums().map { album ->
                        MediaItem.Builder()
                            .setMediaId("album_${album.id}")
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(album.title)
                                    .setArtist(album.artist)
                                    .setIsBrowsable(true)
                                    .setIsPlayable(false)
                                    .build(),
                            )
                            .build()
                    }
                }
                CATEGORY_ARTISTS -> {
                    repository.artists().map { artist ->
                        MediaItem.Builder()
                            .setMediaId("artist_${artist.id}")
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(artist.name)
                                    .setIsBrowsable(true)
                                    .setIsPlayable(false)
                                    .build(),
                            )
                            .build()
                    }
                }
                CATEGORY_PLAYLISTS -> {
                    repository.playlists().map { playlist ->
                        MediaItem.Builder()
                            .setMediaId("playlist_${playlist.id}")
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(playlist.name)
                                    .setIsBrowsable(true)
                                    .setIsPlayable(false)
                                    .build(),
                            )
                            .build()
                    }
                }
                else -> {
                    when {
                        parentId.startsWith("album_") -> {
                            val albumId = parentId.removePrefix("album_").toLongOrNull()
                            val album = albumId?.let { repository.albumById(it) }
                            album?.trackIds?.mapNotNull { repository.trackById(it) }?.map { mapTrackToMediaItem(it) } ?: emptyList()
                        }
                        parentId.startsWith("artist_") -> {
                            val artistId = parentId.removePrefix("artist_").toLongOrNull()
                            val artist = artistId?.let { repository.artistById(it) }
                            artist?.trackIds?.mapNotNull { repository.trackById(it) }?.map { mapTrackToMediaItem(it) } ?: emptyList()
                        }
                        parentId.startsWith("playlist_") -> {
                            val playlistId = parentId.removePrefix("playlist_").toLongOrNull()
                            val playlist = playlistId?.let { repository.playlistById(it) }
                            playlist?.trackIds?.mapNotNull { repository.trackById(it) }?.map { mapTrackToMediaItem(it) } ?: emptyList()
                        }
                        else -> emptyList()
                    }
                }
            }
            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val repository = getOrCreateRepository()
            val trackId = mediaId.removePrefix("track_").toLongOrNull()
            val track = trackId?.let { repository.trackById(it) }
            return if (track != null) {
                Futures.immediateFuture(LibraryResult.ofItem(mapTrackToMediaItem(track), null))
            } else {
                Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
            }
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> {
            val repository = getOrCreateRepository()
            val firstItem = mediaItems.firstOrNull()
            if (firstItem != null) {
                val trackId = firstItem.mediaId.removePrefix("track_").toLongOrNull()
                val track = trackId?.let { repository.trackById(it) }
                if (track != null) {
                    getOrCreatePlaybackManager().playTrack(track)
                }
            }
            return Futures.immediateFuture(mediaItems)
        }
    }

    private fun createCategoryItem(id: String, title: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build(),
            )
            .build()
    }

    private fun mapTrackToMediaItem(track: Track): MediaItem {
        val uriString = track.contentUriString.ifEmpty { "content://media/external/audio/media/${track.id}" }
        return MediaItem.Builder()
            .setMediaId("track_${track.id}")
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(Uri.parse(uriString))
                    .build(),
            )
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build(),
            )
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        if (mediaLibrarySession == null) {
            initMediaSession()
        }
        return mediaLibrarySession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = getOrCreatePlaybackManager()
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> manager.togglePlayPause()
            ACTION_NEXT -> manager.next()
            ACTION_PREVIOUS -> manager.previous()
            ACTION_STOP -> {
                manager.pause()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        val currentState = manager.state.value
        MioraAppWidgetProvider.updateWidgets(
            context = applicationContext,
            track = currentState.currentTrack,
            isPlaying = currentState.isPlaying,
            artwork = null,
        )
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        isServiceRunning = false
        stateObserverJob?.cancel()
        serviceScope.cancel()
        mediaLibrarySession?.run {
            removeSession(this)
            release()
            mediaLibrarySession = null
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

        const val ROOT_ID = "root"
        const val CATEGORY_SONGS = "category_songs"
        const val CATEGORY_ALBUMS = "category_albums"
        const val CATEGORY_ARTISTS = "category_artists"
        const val CATEGORY_PLAYLISTS = "category_playlists"

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
                // Media3's MediaLibraryService automatically handles foreground transition when playback begins.
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
