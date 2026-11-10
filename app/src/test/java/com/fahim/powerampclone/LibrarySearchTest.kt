package com.fahim.powerampclone

import com.fahim.powerampclone.data.MockMusicRepository
import com.fahim.powerampclone.library.LibraryViewModel
import com.fahim.powerampclone.library.SearchViewModel
import com.fahim.powerampclone.model.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibrarySearchTest {
    @Test fun `songs sort by title`() {
        val vm = LibraryViewModel(MockMusicRepository())
        vm.setSortOrder(SortOrder.TITLE)
        val titles = vm.sortedTracks().map { it.title }
        assertEquals(titles.sorted(), titles)
    }

    @Test fun `songs sort by duration descending`() {
        val vm = LibraryViewModel(MockMusicRepository())
        vm.setSortOrder(SortOrder.DURATION)
        val durations = vm.sortedTracks().map { it.durationMs }
        assertEquals(durations.sortedDescending(), durations)
    }

    @Test fun `selection mode toggles`() {
        val vm = LibraryViewModel(MockMusicRepository())
        vm.toggleSelect(1L)
        assertTrue(vm.state.value.selectionMode)
        assertEquals(setOf(1L), vm.state.value.selectedIds)
        vm.clearSelection()
        assertTrue(vm.state.value.selectedIds.isEmpty())
    }

    @Test fun `search finds tracks artists and albums`() {
        val vm = SearchViewModel(MockMusicRepository())
        vm.onQueryChange("aurora")
        val results = vm.state.value.results!!
        assertTrue(results.tracks.isNotEmpty())
        assertTrue(results.artists.any { it.name == "Aurora Lane" })
        assertTrue(results.albums.any { it.title == "Dreamscape" })
    }

    @Test fun `search with no match returns empty groups`() {
        val vm = SearchViewModel(MockMusicRepository())
        vm.onQueryChange("zzzzqqq")
        val results = vm.state.value.results!!
        assertTrue(results.tracks.isEmpty() && results.albums.isEmpty() && results.artists.isEmpty())
    }

    @Test fun `search history records submitted terms`() {
        val vm = SearchViewModel(MockMusicRepository())
        vm.onQueryChange("Nova Echo")
        vm.submitSearch()
        assertEquals("Nova Echo", vm.state.value.history.first())
    }
}
