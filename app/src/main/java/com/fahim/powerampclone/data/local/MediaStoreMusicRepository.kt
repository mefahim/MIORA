package com.fahim.powerampclone.data.local

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.fahim.powerampclone.data.MockMusicRepository
import com.fahim.powerampclone.data.MusicRepository
import com.fahim.powerampclone.data.playlist.PlaylistStore
import com.fahim.powerampclone.data.playlist.SQLitePlaylistStore
import com.fahim.powerampclone.model.Album
import com.fahim.powerampclone.model.Artist
import com.fahim.powerampclone.model.Genre
import com.fahim.powerampclone.model.MusicFolder
import com.fahim.powerampclone.model.Playlist
import com.fahim.powerampclone.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Real MediaStore-backed music repository.
 * Reads local audio files when permission is granted; falls back to deterministic sample
 * data when storage permission is denied or no local audio files are found on the device.
 */
class MediaStoreMusicRepository(
    private val context: Context,
    val playlistStore: PlaylistStore = SQLitePlaylistStore(context),
    private val fallbackRepository: MusicRepository = MockMusicRepository(playlistStore),
) : MusicRepository {

    private val _isPermissionGranted = MutableStateFlow(PermissionHelper.hasMediaPermission(context))
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    private val _isLocalLoaded = MutableStateFlow(false)
    val isLocalLoaded: StateFlow<Boolean> = _isLocalLoaded.asStateFlow()

    @Volatile
    private var cachedLibrary: AggregatedLibrary? = null

    init {
        loadLibrary()
    }

    @Synchronized
    fun reload() {
        val granted = PermissionHelper.hasMediaPermission(context)
        _isPermissionGranted.value = granted
        loadLibrary()
    }

    private fun loadLibrary() {
        val granted = PermissionHelper.hasMediaPermission(context)
        _isPermissionGranted.value = granted

        if (granted) {
            val scanned = MediaStoreScanner.scanTracks(context)
            if (scanned.isNotEmpty()) {
                cachedLibrary = LocalMediaAggregator.aggregate(scanned)
                _isLocalLoaded.value = true
                return
            }
        }

        // Fallback to sample library if permission not granted or 0 local tracks found
        cachedLibrary = null
        _isLocalLoaded.value = false
    }

    override fun tracks(): List<Track> {
        val raw = cachedLibrary?.tracks ?: fallbackRepository.tracks()
        val favs = playlistStore.getFavoriteTrackIds()
        return raw.map { it.copy(isFavorite = favs.contains(it.id)) }
    }

    override fun albums(): List<Album> =
        cachedLibrary?.albums ?: fallbackRepository.albums()

    override fun artists(): List<Artist> =
        cachedLibrary?.artists ?: fallbackRepository.artists()

    override fun playlists(): List<Playlist> =
        playlistStore.getAllPlaylists()

    override fun genres(): List<Genre> =
        cachedLibrary?.genres ?: fallbackRepository.genres()

    override fun folders(): List<MusicFolder> =
        cachedLibrary?.folders ?: fallbackRepository.folders()

    override fun recentlyPlayed(): List<Track> {
        val ids = playlistStore.getRecentlyPlayedTrackIds(50)
        val resolved = ids.mapNotNull { trackById(it) }
        return if (resolved.isNotEmpty()) {
            resolved
        } else if (_isLocalLoaded.value) {
            tracks().take(6)
        } else {
            fallbackRepository.recentlyPlayed()
        }
    }

    override fun trackById(id: Long): Track? {
        val t = (cachedLibrary?.tracks ?: fallbackRepository.tracks()).firstOrNull { it.id == id } ?: return null
        return t.copy(isFavorite = playlistStore.getFavoriteTrackIds().contains(id))
    }

    override fun albumById(id: Long): Album? =
        albums().firstOrNull { it.id == id }

    override fun artistById(id: Long): Artist? =
        artists().firstOrNull { it.id == id }

    override fun playlistById(id: Long): Playlist? =
        playlistStore.getPlaylistById(id)

    override fun createPlaylist(name: String, artworkSeed: Color?): Playlist? =
        playlistStore.createPlaylist(name, artworkSeed)

    override fun renamePlaylist(id: Long, newName: String): Boolean =
        playlistStore.renamePlaylist(id, newName)

    override fun deletePlaylist(id: Long): Boolean =
        playlistStore.deletePlaylist(id)

    override fun addTracksToPlaylist(playlistId: Long, trackIds: List<Long>) =
        playlistStore.addTracksToPlaylist(playlistId, trackIds)

    override fun removeTrackFromPlaylist(playlistId: Long, trackId: Long, position: Int?) =
        playlistStore.removeTrackFromPlaylist(playlistId, trackId, position)

    override fun reorderPlaylistTracks(playlistId: Long, fromIndex: Int, toIndex: Int) =
        playlistStore.reorderTracks(playlistId, fromIndex, toIndex)

    override fun isFavorite(trackId: Long): Boolean =
        playlistStore.getFavoriteTrackIds().contains(trackId)

    override fun toggleFavorite(trackId: Long): Boolean {
        val fav = isFavorite(trackId)
        playlistStore.setFavorite(trackId, !fav)
        return !fav
    }

    override fun recordPlayback(trackId: Long) =
        playlistStore.recordPlayback(trackId)

    override fun search(query: String): MusicRepository.SearchResults {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return MusicRepository.SearchResults(emptyList(), emptyList(), emptyList(), emptyList())
        return MusicRepository.SearchResults(
            tracks = tracks().filter { it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) || it.album.lowercase().contains(q) },
            albums = albums().filter { it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) },
            artists = artists().filter { it.name.lowercase().contains(q) },
            playlists = playlists().filter { it.name.lowercase().contains(q) },
        )
    }
}
