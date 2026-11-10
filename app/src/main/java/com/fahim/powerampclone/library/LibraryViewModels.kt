package com.fahim.powerampclone.library

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.fahim.powerampclone.data.MockMusicRepository
import com.fahim.powerampclone.data.MusicRepository
import com.fahim.powerampclone.data.local.MediaStoreMusicRepository
import com.fahim.powerampclone.model.Playlist
import com.fahim.powerampclone.model.SortOrder
import com.fahim.powerampclone.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Library list state: sorting + multi-select, shared by Songs/Albums/Artists screens. */
class LibraryViewModel(
    private val repository: MusicRepository = MockMusicRepository(),
) : ViewModel() {
    data class UiState(
        val sortOrder: SortOrder = SortOrder.TITLE,
        val selectedIds: Set<Long> = emptySet(),
        val selectionMode: Boolean = false,
        val isPermissionGranted: Boolean = true,
        val isLocalLoaded: Boolean = false,
        val playlistRevision: Long = 0L,
    )

    private val _state = MutableStateFlow(
        UiState(
            isPermissionGranted = (repository as? MediaStoreMusicRepository)?.isPermissionGranted?.value ?: true,
            isLocalLoaded = (repository as? MediaStoreMusicRepository)?.isLocalLoaded?.value ?: false,
        ),
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    val allTracks: List<Track> get() = repository.tracks()
    val albums get() = repository.albums()
    val artists get() = repository.artists()
    val playlists: List<Playlist> get() = repository.playlists()
    val userPlaylists: List<Playlist> get() = playlists.filter { !it.isSmart }
    val genres get() = repository.genres()
    val folders get() = repository.folders()
    val recentlyPlayed get() = repository.recentlyPlayed()

    fun sortedTracks(): List<Track> = when (_state.value.sortOrder) {
        SortOrder.TITLE -> allTracks.sortedBy { it.title.lowercase() }
        SortOrder.ARTIST -> allTracks.sortedBy { it.artist.lowercase() }
        SortOrder.DURATION -> allTracks.sortedByDescending { it.durationMs }
        SortOrder.RECENTLY_ADDED -> allTracks.sortedByDescending { it.id }
        SortOrder.TRACK_NUMBER -> allTracks.sortedBy { it.trackNumber }
    }

    fun setSortOrder(order: SortOrder) = _state.update { it.copy(sortOrder = order) }

    fun toggleSelect(id: Long) {
        _state.update {
            val selected = if (id in it.selectedIds) it.selectedIds - id else it.selectedIds + id
            it.copy(selectedIds = selected, selectionMode = selected.isNotEmpty())
        }
    }

    fun clearSelection() = _state.update { it.copy(selectedIds = emptySet(), selectionMode = false) }

    fun refresh() {
        (repository as? MediaStoreMusicRepository)?.reload()
        _state.update {
            it.copy(
                isPermissionGranted = (repository as? MediaStoreMusicRepository)?.isPermissionGranted?.value ?: true,
                isLocalLoaded = (repository as? MediaStoreMusicRepository)?.isLocalLoaded?.value ?: false,
                playlistRevision = it.playlistRevision + 1,
            )
        }
    }

    fun trackById(id: Long) = repository.trackById(id)
    fun albumById(id: Long) = repository.albumById(id)
    fun artistById(id: Long) = repository.artistById(id)
    fun playlistById(id: Long) = repository.playlistById(id)
    fun tracksByIds(ids: List<Long>) = ids.mapNotNull { repository.trackById(it) }

    // Playlist CRUD
    fun createPlaylist(name: String, artworkSeed: Color? = null): Playlist? {
        val created = repository.createPlaylist(name, artworkSeed)
        if (created != null) {
            _state.update { it.copy(playlistRevision = it.playlistRevision + 1) }
        }
        return created
    }

    fun renamePlaylist(id: Long, newName: String): Boolean {
        val success = repository.renamePlaylist(id, newName)
        if (success) {
            _state.update { it.copy(playlistRevision = it.playlistRevision + 1) }
        }
        return success
    }

    fun deletePlaylist(id: Long): Boolean {
        val success = repository.deletePlaylist(id)
        if (success) {
            _state.update { it.copy(playlistRevision = it.playlistRevision + 1) }
        }
        return success
    }

    fun addTracksToPlaylist(playlistId: Long, trackIds: List<Long>) {
        repository.addTracksToPlaylist(playlistId, trackIds)
        _state.update { it.copy(playlistRevision = it.playlistRevision + 1) }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long, position: Int? = null) {
        repository.removeTrackFromPlaylist(playlistId, trackId, position)
        _state.update { it.copy(playlistRevision = it.playlistRevision + 1) }
    }

    fun reorderPlaylistTracks(playlistId: Long, fromIndex: Int, toIndex: Int) {
        repository.reorderPlaylistTracks(playlistId, fromIndex, toIndex)
        _state.update { it.copy(playlistRevision = it.playlistRevision + 1) }
    }

    fun tracksForPlaylist(playlist: Playlist): List<Track> {
        return playlist.trackIds.mapNotNull { repository.trackById(it) }
    }

    fun durationForPlaylist(playlist: Playlist): Long {
        return tracksForPlaylist(playlist).sumOf { it.durationMs }
    }

    fun toggleFavorite(trackId: Long): Boolean {
        val fav = repository.toggleFavorite(trackId)
        _state.update { it.copy(playlistRevision = it.playlistRevision + 1) }
        return fav
    }
}

class SearchViewModel(
    private val repository: MusicRepository = MockMusicRepository(),
) : ViewModel() {
    data class UiState(
        val query: String = "",
        val results: MusicRepository.SearchResults? = null,
        val history: List<String> = emptyList(),
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun onQueryChange(newQuery: String) {
        _state.update {
            it.copy(
                query = newQuery,
                results = if (newQuery.isBlank()) null else repository.search(newQuery),
            )
        }
    }

    fun submitSearch() {
        val q = _state.value.query.trim()
        if (q.isNotEmpty()) {
            _state.update {
                val updated = listOf(q) + (it.history - q)
                it.copy(history = updated.take(10))
            }
        }
    }

    fun clearHistory() = _state.update { it.copy(history = emptyList()) }
}
