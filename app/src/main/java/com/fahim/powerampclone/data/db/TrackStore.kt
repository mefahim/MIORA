package com.fahim.powerampclone.data.db

import com.fahim.powerampclone.model.Track

/**
 * Contract for persistent audio track caching, retrieval, and library metadata storage.
 */
interface TrackStore {
    /** Returns all cached tracks sorted by title. */
    fun getAllTracks(): List<Track>

    /** Finds a track by its unique ID. */
    fun getTrackById(id: Long): Track?

    /** Finds a track by its absolute storage file path. */
    fun getTrackByPath(path: String): Track?

    /** Finds a track by its content URI string. */
    fun getTrackByUri(uri: String): Track?

    /** Finds a track by its stable content hash. */
    fun getTrackByStableHash(hash: String): Track?

    /**
     * Batch inserts or updates tracks inside an atomic database transaction.
     * Returns the number of affected rows.
     */
    fun upsertTracks(tracks: List<Track>): Int

    /** Deletes specific tracks by ID. */
    fun deleteTracks(trackIds: Collection<Long>): Int

    /** Purges tracks whose IDs are not in the provided active set (used during scan cleanup). */
    fun deleteTracksNotIn(activeIds: Collection<Long>): Int

    /** Database-backed fuzzy search across title, artist, album, and folder path. */
    fun searchTracks(query: String, limit: Int = 100): List<Track>

    /** Returns total number of cached tracks in the database. */
    fun getTrackCount(): Int

    /** Returns timestamp of the last successful scanner run in epoch milliseconds (0 if never scanned). */
    fun getLastScanTimestamp(): Long

    /** Updates the timestamp of the last successful scanner run. */
    fun setLastScanTimestamp(timestampMs: Long)

    /** Wipes all cached tracks (does not affect playlists, favorites, or history). */
    fun clearAllTracks()
}
