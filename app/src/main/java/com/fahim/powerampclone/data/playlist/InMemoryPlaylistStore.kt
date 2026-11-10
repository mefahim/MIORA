package com.fahim.powerampclone.data.playlist

import androidx.compose.ui.graphics.Color
import com.fahim.powerampclone.model.Playlist
import com.fahim.powerampclone.model.seedColorForString

/**
 * Thread-safe in-memory playlist store for JVM unit tests and fast in-memory previews.
 */
class InMemoryPlaylistStore : PlaylistStore {

    private var nextPlaylistId = 1L
    private val playlists = mutableMapOf<Long, Playlist>()
    private val playlistTrackMap = mutableMapOf<Long, MutableList<Long>>()
    private val favorites = LinkedHashSet<Long>()
    private val playbackHistory = mutableListOf<Pair<Long, Long>>() // trackId to timestamp

    companion object {
        const val SMART_FAVORITES_ID = -100L
        const val SMART_RECENTLY_PLAYED_ID = -101L
    }

    init {
        // Seed default initial starter playlists
        seedInitialPlaylists()
    }

    private fun seedInitialPlaylists() {
        val starter = listOf(
            Triple("Chill Vibes", Color(0xFF06B6D4), listOf(1L, 4L, 8L, 12L, 15L, 18L, 19L)),
            Triple("Focus Flow", Color(0xFF10B981), listOf(1L, 7L, 8L, 9L, 15L, 19L)),
            Triple("Dreamscape", Color(0xFF3B82F6), listOf(1L, 3L, 7L, 8L, 9L, 11L, 19L)),
        )

        starter.forEach { (name, color, tracks) ->
            val id = nextPlaylistId++
            playlists[id] = Playlist(id = id, name = name, artworkSeed = color, trackIds = tracks, isSmart = false)
            playlistTrackMap[id] = tracks.toMutableList()
        }

        listOf(1L, 3L, 7L, 13L).forEach { favorites.add(it) }
        listOf(1L, 2L, 3L, 4L, 5L, 6L).forEachIndexed { i, id ->
            playbackHistory.add(id to (System.currentTimeMillis() - ((6 - i) * 60_000L)))
        }
    }

    @Synchronized
    override fun getAllPlaylists(): List<Playlist> {
        val result = mutableListOf<Playlist>()
        result.add(
            Playlist(
                id = SMART_FAVORITES_ID,
                name = "Favourites",
                artworkSeed = Color(0xFFEF4444),
                trackIds = getFavoriteTrackIds().toList(),
                isSmart = true,
            )
        )
        result.add(
            Playlist(
                id = SMART_RECENTLY_PLAYED_ID,
                name = "Recently Played",
                artworkSeed = Color(0xFFF59E0B),
                trackIds = getRecentlyPlayedTrackIds(50),
                isSmart = true,
            )
        )

        val userPlaylists = playlists.values
            .sortedBy { it.name.lowercase() }
            .map { p ->
                val currentTracks = playlistTrackMap[p.id] ?: emptyList()
                p.copy(trackIds = currentTracks)
            }
        result.addAll(userPlaylists)
        return result
    }

    @Synchronized
    override fun getPlaylistById(id: Long): Playlist? {
        if (id == SMART_FAVORITES_ID) {
            return Playlist(
                id = SMART_FAVORITES_ID,
                name = "Favourites",
                artworkSeed = Color(0xFFEF4444),
                trackIds = getFavoriteTrackIds().toList(),
                isSmart = true,
            )
        }
        if (id == SMART_RECENTLY_PLAYED_ID) {
            return Playlist(
                id = SMART_RECENTLY_PLAYED_ID,
                name = "Recently Played",
                artworkSeed = Color(0xFFF59E0B),
                trackIds = getRecentlyPlayedTrackIds(50),
                isSmart = true,
            )
        }
        val p = playlists[id] ?: return null
        return p.copy(trackIds = playlistTrackMap[id] ?: emptyList())
    }

    @Synchronized
    override fun createPlaylist(name: String, artworkSeed: Color?): Playlist? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        if (playlists.values.any { it.name.equals(trimmed, ignoreCase = true) }) return null

        val id = nextPlaylistId++
        val seed = artworkSeed ?: seedColorForString(trimmed)
        val created = Playlist(id = id, name = trimmed, artworkSeed = seed, trackIds = emptyList(), isSmart = false)
        playlists[id] = created
        playlistTrackMap[id] = mutableListOf()
        return created
    }

    @Synchronized
    override fun renamePlaylist(id: Long, newName: String): Boolean {
        if (id < 0L) return false
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return false
        if (playlists.values.any { it.id != id && it.name.equals(trimmed, ignoreCase = true) }) return false

        val existing = playlists[id] ?: return false
        playlists[id] = existing.copy(name = trimmed)
        return true
    }

    @Synchronized
    override fun deletePlaylist(id: Long): Boolean {
        if (id < 0L) return false
        playlistTrackMap.remove(id)
        return playlists.remove(id) != null
    }

    @Synchronized
    override fun addTracksToPlaylist(playlistId: Long, trackIds: List<Long>) {
        if (trackIds.isEmpty()) return
        if (playlistId == SMART_FAVORITES_ID) {
            trackIds.forEach { setFavorite(it, true) }
            return
        }
        if (playlistId < 0L) return

        val list = playlistTrackMap.getOrPut(playlistId) { mutableListOf() }
        list.addAll(trackIds)
    }

    @Synchronized
    override fun removeTrackFromPlaylist(playlistId: Long, trackId: Long, position: Int?) {
        if (playlistId == SMART_FAVORITES_ID) {
            setFavorite(trackId, false)
            return
        }
        if (playlistId < 0L) return

        val list = playlistTrackMap[playlistId] ?: return
        if (position != null && position in list.indices) {
            if (list[position] == trackId) {
                list.removeAt(position)
            } else {
                list.remove(trackId)
            }
        } else {
            list.remove(trackId)
        }
    }

    @Synchronized
    override fun reorderTracks(playlistId: Long, fromIndex: Int, toIndex: Int) {
        if (playlistId < 0L || fromIndex == toIndex) return
        val list = playlistTrackMap[playlistId] ?: return
        if (fromIndex in list.indices && toIndex in list.indices) {
            val moved = list.removeAt(fromIndex)
            list.add(toIndex, moved)
        }
    }

    @Synchronized
    override fun getFavoriteTrackIds(): Set<Long> = LinkedHashSet(favorites)

    @Synchronized
    override fun setFavorite(trackId: Long, isFavorite: Boolean) {
        if (isFavorite) {
            favorites.add(trackId)
        } else {
            favorites.remove(trackId)
        }
    }

    @Synchronized
    override fun getRecentlyPlayedTrackIds(limit: Int): List<Long> {
        val seen = mutableSetOf<Long>()
        val result = mutableListOf<Long>()
        // Traverse history newest first
        for (i in playbackHistory.indices.reversed()) {
            val trackId = playbackHistory[i].first
            if (seen.add(trackId)) {
                result.add(trackId)
                if (result.size >= limit) break
            }
        }
        return result
    }

    @Synchronized
    override fun recordPlayback(trackId: Long, timestamp: Long) {
        playbackHistory.add(trackId to timestamp)
        if (playbackHistory.size > 200) {
            playbackHistory.removeAt(0)
        }
    }
}
