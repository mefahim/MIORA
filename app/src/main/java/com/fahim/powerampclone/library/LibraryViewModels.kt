package com.fahim.powerampclone.library

import androidx.lifecycle.ViewModel
import com.fahim.powerampclone.data.MockMusicRepository
import com.fahim.powerampclone.data.MusicRepository
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
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    val allTracks: List<Track> get() = repository.tracks()
    val albums get() = repository.albums()
    val artists get() = repository.artists()
    val playlists get() = repository.playlists()
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
    fun trackById(id: Long) = repository.trackById(id)
    fun albumById(id: Long) = repository.albumById(id)
    fun artistById(id: Long) = repository.artistById(id)
    fun playlistById(id: Long) = repository.playlistById(id)
    fun tracksByIds(ids: List<Long>) = ids.mapNotNull { repository.trackById(it) }
}

class SearchViewModel(
    private val repository: MusicRepository = MockMusicRepository(),
) : ViewModel() {
    data class UiState(
        val query: String = "",
        val history: List<String> = listOf("Sky Dreams", "Aurora Lane", "Midnight"),
        val results: MusicRepository.SearchResults? = null,
        val isSearching: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query, isSearching = query.isNotBlank(), results = if (query.isBlank()) null else repository.search(query)) }
    }

    fun submitSearch() {
        val q = _state.value.query.trim()
        if (q.isEmpty()) return
        _state.update {
            it.copy(history = (listOf(q) + it.history).distinct().take(8), isSearching = false)
        }
    }

    fun clearSearch() = _state.update { it.copy(query = "", results = null, isSearching = false) }
    fun clearHistory() = _state.update { it.copy(history = emptyList()) }
    fun useHistoryTerm(term: String) = onQueryChange(term)
}
