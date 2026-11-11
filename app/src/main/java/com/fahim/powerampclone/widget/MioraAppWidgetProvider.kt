package com.fahim.powerampclone.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import com.fahim.powerampclone.MainActivity
import com.fahim.powerampclone.R
import com.fahim.powerampclone.model.Track
import com.fahim.powerampclone.playback.PlaybackService

/**
 * Native Android Home Screen AppWidget Provider for MIORA.
 *
 * Displays:
 * - Current track title and artist.
 * - Album artwork thumbnail.
 * - Play/Pause, Next, and Previous transport controls.
 * - Synchronized with MediaSession / PlaybackService state.
 */
class MioraAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        for (appWidgetId in appWidgetIds) {
            updateSingleWidget(context, appWidgetManager, appWidgetId, lastTrack, lastIsPlaying, lastArtwork)
        }
    }

    companion object {
        private var lastTrack: Track? = null
        private var lastIsPlaying: Boolean = false
        private var lastArtwork: Bitmap? = null

        fun updateWidgets(
            context: Context,
            track: Track?,
            isPlaying: Boolean,
            artwork: Bitmap? = null,
        ) {
            lastTrack = track
            lastIsPlaying = isPlaying
            lastArtwork = artwork

            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, MioraAppWidgetProvider::class.java)
            val appWidgetIds = try {
                appWidgetManager.getAppWidgetIds(componentName)
            } catch (_: Throwable) {
                intArrayOf()
            }

            if (appWidgetIds.isEmpty()) return

            for (appWidgetId in appWidgetIds) {
                updateSingleWidget(context, appWidgetManager, appWidgetId, track, isPlaying, artwork)
            }
        }

        private fun updateSingleWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            track: Track?,
            isPlaying: Boolean,
            artwork: Bitmap?,
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_miora_player)

            // Track info
            if (track != null) {
                views.setTextViewText(R.id.widget_track_title, track.title)
                views.setTextViewText(R.id.widget_track_artist, "${track.artist} • ${track.album}")
            } else {
                views.setTextViewText(R.id.widget_track_title, context.getString(R.string.app_name))
                views.setTextViewText(R.id.widget_track_artist, "No track playing")
            }

            // Play/Pause icon
            val playPauseIcon = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
            views.setImageViewResource(R.id.widget_btn_play_pause, playPauseIcon)

            // Album artwork
            if (artwork != null && !artwork.isRecycled) {
                views.setImageViewBitmap(R.id.widget_album_art, artwork)
            } else {
                views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_widget_art_placeholder)
            }

            // Open app click intent
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

            // Play/Pause transport click intent
            val playPauseIntent = Intent(context, PlaybackService::class.java).apply {
                action = PlaybackService.ACTION_PLAY_PAUSE
            }
            val playPausePendingIntent = PendingIntent.getService(
                context,
                101,
                playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_btn_play_pause, playPausePendingIntent)

            // Next transport click intent
            val nextIntent = Intent(context, PlaybackService::class.java).apply {
                action = PlaybackService.ACTION_NEXT
            }
            val nextPendingIntent = PendingIntent.getService(
                context,
                102,
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_btn_next, nextPendingIntent)

            // Previous transport click intent
            val prevIntent = Intent(context, PlaybackService::class.java).apply {
                action = PlaybackService.ACTION_PREVIOUS
            }
            val prevPendingIntent = PendingIntent.getService(
                context,
                103,
                prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_btn_previous, prevPendingIntent)

            try {
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (_: Throwable) {}
        }
    }
}
