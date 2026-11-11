package com.fahim.powerampclone.data.db

import com.fahim.powerampclone.model.Track

/**
 * In-memory thread-safe implementation of [TrackStore] for testing and transient caching.
 */
class InMemoryTrackStore : TrackStore {

    private val tracksMap = LinkedHashMap<Long, Track>()
    private var lastScanTimestamp: Long = 0L

    @Synchronized
    override fun getAllTracks(): List<Track> {
        return tracksMap.values.sortedBy { it.title.lowercase() }
    }

    @Synchronized
    override fun getTrackById(id: Long): Track? {
        return tracksMap[id]
    }

    @Synchronized
    override fun getTrackByPath(path: String): Track? {
        return tracksMap.values.firstOrNull { it.folderPath == path }
    }

    @Synchronized
    override fun getTrackByUri(uri: String): Track? {
        return tracksMap.values.firstOrNull { it.contentUriString == uri }
    }

    @Synchronized
    override fun getTrackByStableHash(hash: String): Track? {
        return tracksMap.values.firstOrNull { computeStableHash(it) == hash }
    }

    @Synchronized
    override fun upsertTracks(tracks: List<Track>): Int {
        var count = 0
        for (track in tracks) {
            tracksMap[track.id] = track
            count++
        }
        return count
    }

    @Synchronized
    override fun deleteTracks(trackIds: Collection<Long>): Int {
        var count = 0
        for (id in trackIds) {
            if (tracksMap.remove(id) != null) {
                count++
            }
        }
        return count
    }

    @Synchronized
    override fun deleteTracksNotIn(activeIds: Collection<Long>): Int {
        val activeSet = activeIds.toSet()
        val toRemove = tracksMap.keys.filter { it !in activeSet }
        toRemove.forEach { tracksMap.remove(it) }
        return toRemove.size
    }

    @Synchronized
    override fun searchTracks(query: String, limit: Int): List<Track> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return tracksMap.values
            .filter {
                it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q) ||
                it.folderPath.lowercase().contains(q)
            }
            .sortedBy { it.title.lowercase() }
            .take(limit)
    }

    @Synchronized
    override fun getTrackCount(): Int {
        return tracksMap.size
    }

    @Synchronized
    override fun getLastScanTimestamp(): Long {
        return lastScanTimestamp
    }

    @Synchronized
    override fun setLastScanTimestamp(timestampMs: Long) {
        lastScanTimestamp = timestampMs
    }

    @Synchronized
    override fun clearAllTracks() {
        tracksMap.clear()
        lastScanTimestamp = 0L
    }

    private fun computeStableHash(track: Track): String {
        return "${track.contentUriString}|${track.folderPath}|${track.title}|${track.album}|${track.durationMs}".hashCode().toString()
    }
}
