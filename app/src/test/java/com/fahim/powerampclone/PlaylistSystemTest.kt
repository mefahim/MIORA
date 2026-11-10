package com.fahim.powerampclone

import androidx.compose.ui.graphics.Color
import com.fahim.powerampclone.data.MockData
import com.fahim.powerampclone.data.MockMusicRepository
import com.fahim.powerampclone.data.playlist.InMemoryPlaylistStore
import com.fahim.powerampclone.library.LibraryViewModel
import com.fahim.powerampclone.model.Playlist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlaylistSystemTest {

    private lateinit var store: InMemoryPlaylistStore
    private lateinit var repository: MockMusicRepository
    private lateinit var viewModel: LibraryViewModel

    @Before
    fun setUp() {
        store = InMemoryPlaylistStore()
        repository = MockMusicRepository(store)
        viewModel = LibraryViewModel(repository)
    }

    @Test
    fun `initial playlists contain seeded starter playlists and smart playlists`() {
        val playlists = viewModel.playlists
        assertTrue(playlists.size >= 5)

        val favs = playlists.firstOrNull { it.id == InMemoryPlaylistStore.SMART_FAVORITES_ID }
        assertNotNull(favs)
        assertTrue(favs!!.isSmart)
        assertEquals("Favourites", favs.name)

        val recent = playlists.firstOrNull { it.id == InMemoryPlaylistStore.SMART_RECENTLY_PLAYED_ID }
        assertNotNull(recent)
        assertTrue(recent!!.isSmart)
        assertEquals("Recently Played", recent.name)
    }

    @Test
    fun `create playlist with valid name succeeds`() {
        val created = viewModel.createPlaylist("Midnight Drive", Color(0xFF6366F1))
        assertNotNull(created)
        assertEquals("Midnight Drive", created!!.name)
        assertFalse(created.isSmart)
        assertTrue(created.trackIds.isEmpty())

        val found = viewModel.playlistById(created.id)
        assertNotNull(found)
        assertEquals("Midnight Drive", found!!.name)
    }

    @Test
    fun `create playlist with blank name fails`() {
        val blank = viewModel.createPlaylist("   ")
        assertNull(blank)
    }

    @Test
    fun `create playlist with duplicate name fails`() {
        val first = viewModel.createPlaylist("Chill Vibes 2")
        assertNotNull(first)

        val duplicate = viewModel.createPlaylist("chill vibes 2") // case-insensitive check
        assertNull(duplicate)
    }

    @Test
    fun `rename playlist succeeds for user playlist`() {
        val created = viewModel.createPlaylist("Old Name")!!
        val renamed = viewModel.renamePlaylist(created.id, "New Name")
        assertTrue(renamed)

        val updated = viewModel.playlistById(created.id)
        assertEquals("New Name", updated?.name)
    }

    @Test
    fun `rename smart playlist fails`() {
        val renamed = viewModel.renamePlaylist(InMemoryPlaylistStore.SMART_FAVORITES_ID, "Hacked")
        assertFalse(renamed)
    }

    @Test
    fun `delete playlist removes user playlist and its tracks`() {
        val created = viewModel.createPlaylist("Temporary")!!
        viewModel.addTracksToPlaylist(created.id, listOf(1L, 2L, 3L))

        val deleted = viewModel.deletePlaylist(created.id)
        assertTrue(deleted)

        val lookup = viewModel.playlistById(created.id)
        assertNull(lookup)
    }

    @Test
    fun `delete smart playlist is rejected`() {
        val deleted = viewModel.deletePlaylist(InMemoryPlaylistStore.SMART_FAVORITES_ID)
        assertFalse(deleted)
    }

    @Test
    fun `add and remove tracks in playlist`() {
        val playlist = viewModel.createPlaylist("Workout")!!

        viewModel.addTracksToPlaylist(playlist.id, listOf(5L, 6L, 13L))
        var updated = viewModel.playlistById(playlist.id)!!
        assertEquals(listOf(5L, 6L, 13L), updated.trackIds)

        viewModel.removeTrackFromPlaylist(playlist.id, 6L)
        updated = viewModel.playlistById(playlist.id)!!
        assertEquals(listOf(5L, 13L), updated.trackIds)
    }

    @Test
    fun `reorder tracks in playlist shifts positions correctly`() {
        val playlist = viewModel.createPlaylist("Road Trip")!!
        viewModel.addTracksToPlaylist(playlist.id, listOf(1L, 2L, 3L, 4L))

        // Move item at index 0 (1L) to index 2
        viewModel.reorderPlaylistTracks(playlist.id, fromIndex = 0, toIndex = 2)

        val updated = viewModel.playlistById(playlist.id)!!
        assertEquals(listOf(2L, 3L, 1L, 4L), updated.trackIds)
    }

    @Test
    fun `favorites smart playlist dynamically reflects favorite state`() {
        // Toggle favorite for track 20
        assertFalse(repository.isFavorite(20L))
        viewModel.toggleFavorite(20L)
        assertTrue(repository.isFavorite(20L))

        val favPlaylist = viewModel.playlistById(InMemoryPlaylistStore.SMART_FAVORITES_ID)!!
        assertTrue(favPlaylist.trackIds.contains(20L))

        // Toggle again to remove
        viewModel.toggleFavorite(20L)
        assertFalse(repository.isFavorite(20L))
        val updatedFavPlaylist = viewModel.playlistById(InMemoryPlaylistStore.SMART_FAVORITES_ID)!!
        assertFalse(updatedFavPlaylist.trackIds.contains(20L))
    }

    @Test
    fun `recently played smart playlist reflects playback history ordered newest first`() {
        repository.recordPlayback(10L)
        repository.recordPlayback(14L)
        repository.recordPlayback(18L)

        val recentPlaylist = viewModel.playlistById(InMemoryPlaylistStore.SMART_RECENTLY_PLAYED_ID)!!
        val recentIds = recentPlaylist.trackIds

        // Most recent (18L) must be at index 0
        assertEquals(18L, recentIds[0])
        assertEquals(14L, recentIds[1])
        assertEquals(10L, recentIds[2])
    }

    @Test
    fun `tracksForPlaylist gracefully skips missing track IDs`() {
        val playlist = Playlist(
            id = 999L,
            name = "Test Resilience",
            artworkSeed = Color.Blue,
            trackIds = listOf(1L, 999999L, 2L), // 999999L does not exist in library
        )

        val resolved = viewModel.tracksForPlaylist(playlist)
        // Missing track 999999L is omitted cleanly without crashing or corrupting
        assertEquals(2, resolved.size)
        assertEquals(1L, resolved[0].id)
        assertEquals(2L, resolved[1].id)
    }

    @Test
    fun `durationForPlaylist accurately sums track durations`() {
        val playlist = Playlist(
            id = 999L,
            name = "Short List",
            artworkSeed = Color.Cyan,
            trackIds = listOf(1L, 2L),
        )
        val track1 = MockData.tracks.first { it.id == 1L }
        val track2 = MockData.tracks.first { it.id == 2L }
        val expectedDuration = track1.durationMs + track2.durationMs

        val calculated = viewModel.durationForPlaylist(playlist)
        assertEquals(expectedDuration, calculated)
    }
}
