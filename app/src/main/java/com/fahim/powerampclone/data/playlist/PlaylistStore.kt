package com.fahim.powerampclone.data.playlist

import androidx.compose.ui.graphics.Color
import com.fahim.powerampclone.model.Playlist

interface PlaylistStore {
    /** Returns all playlists with their ordered track IDs. */
    fun getAllPlaylists(): List<Playlist>

    /** Returns a specific playlist by ID. */
    fun getPlaylistById(id: Long): Playlist?

    /** Creates a new playlist with the given name. Returns null if name is invalid or duplicate. */
    fun createPlaylist(name: String, artworkSeed: Color? = null): Playlist?

    /** Renames an existing playlist. Returns false if duplicate or invalid. */
    fun renamePlaylist(id: Long, newName: String): Boolean

    /** Deletes an existing playlist. Smart playlists cannot be deleted. */
    fun deletePlaylist(id: Long): Boolean

    /** Appends tracks to a playlist. */
    fun addTracksToPlaylist(playlistId: Long, trackIds: List<Long>)

    /** Removes a track from a playlist by trackId (optionally matching index/position). */
    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long, position: Int? = null)

    /** Reorders tracks in a playlist from fromIndex to toIndex. */
    fun reorderTracks(playlistId: Long, fromIndex: Int, toIndex: Int)

    /** Returns persistent favorite track IDs. */
    fun getFavoriteTrackIds(): Set<Long>

    /** Toggles or sets favorite status for a track. */
    fun setFavorite(trackId: Long, isFavorite: Boolean)

    /** Returns ordered recently played track IDs (most recent first, up to limit). */
    fun getRecentlyPlayedTrackIds(limit: Int = 50): List<Long>

    /** Records a track playback event in persistent history. */
    fun recordPlayback(trackId: Long, timestamp: Long = System.currentTimeMillis())
}
