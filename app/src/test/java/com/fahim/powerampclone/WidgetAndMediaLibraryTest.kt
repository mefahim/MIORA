package com.fahim.powerampclone

import com.fahim.powerampclone.data.MockMusicRepository
import com.fahim.powerampclone.model.Track
import com.fahim.powerampclone.playback.PlaybackService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WidgetAndMediaLibraryTest {

    private lateinit var repository: MockMusicRepository

    @Before
    fun setUp() {
        repository = MockMusicRepository()
    }

    @Test
    fun testRootCategories() {
        val rootCategories = listOf(
            PlaybackService.CATEGORY_SONGS,
            PlaybackService.CATEGORY_ALBUMS,
            PlaybackService.CATEGORY_ARTISTS,
            PlaybackService.CATEGORY_PLAYLISTS,
        )

        assertEquals(4, rootCategories.size)
        assertTrue(rootCategories.contains(PlaybackService.CATEGORY_SONGS))
        assertTrue(rootCategories.contains(PlaybackService.CATEGORY_ALBUMS))
        assertTrue(rootCategories.contains(PlaybackService.CATEGORY_ARTISTS))
        assertTrue(rootCategories.contains(PlaybackService.CATEGORY_PLAYLISTS))
    }

    @Test
    fun testSongHierarchyMapping() {
        val tracks = repository.tracks()
        assertTrue(tracks.isNotEmpty())

        val firstTrack = tracks.first()
        val mediaId = "track_${firstTrack.id}"
        assertEquals("track_1", mediaId)

        val resolvedTrack = repository.trackById(firstTrack.id)
        assertNotNull(resolvedTrack)
        assertEquals(firstTrack.title, resolvedTrack?.title)
    }

    @Test
    fun testAlbumChildrenResolution() {
        val albums = repository.albums()
        assertTrue(albums.isNotEmpty())

        val firstAlbum = albums.first()
        val containedTracks = firstAlbum.trackIds.mapNotNull { repository.trackById(it) }

        assertTrue(containedTracks.isNotEmpty())
        containedTracks.forEach { track ->
            assertEquals(firstAlbum.id, track.albumId)
        }
    }

    @Test
    fun testArtistChildrenResolution() {
        val artists = repository.artists()
        assertTrue(artists.isNotEmpty())

        val firstArtist = artists.first()
        val containedTracks = firstArtist.trackIds.mapNotNull { repository.trackById(it) }

        assertTrue(containedTracks.isNotEmpty())
        containedTracks.forEach { track ->
            assertEquals(firstArtist.id, track.artistId)
        }
    }

    @Test
    fun testPlaylistChildrenResolution() {
        val playlists = repository.playlists()
        assertTrue(playlists.isNotEmpty())

        val firstPlaylist = playlists.first()
        val containedTracks = firstPlaylist.trackIds.mapNotNull { repository.trackById(it) }
        assertEquals(firstPlaylist.trackIds.size, containedTracks.size)
    }

    @Test
    fun testWidgetPresentationState() {
        // Paused state with active track
        val sampleTrack = Track(
            id = 42L,
            title = "Midnight Horizon",
            artist = "Aether",
            album = "Cosmic Dreams",
        )

        fun computeWidgetPresentation(track: Track?, isPlaying: Boolean): Triple<String, String, Int> {
            val title = track?.title ?: "MIORA Player"
            val subtitle = if (track != null) "${track.artist} • ${track.album}" else "No track playing"
            val playPauseIcon = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
            return Triple(title, subtitle, playPauseIcon)
        }

        val (titlePlaying, subPlaying, iconPlaying) = computeWidgetPresentation(sampleTrack, isPlaying = true)
        assertEquals("Midnight Horizon", titlePlaying)
        assertEquals("Aether • Cosmic Dreams", subPlaying)
        assertEquals(R.drawable.ic_widget_pause, iconPlaying)

        val (titlePaused, _, iconPaused) = computeWidgetPresentation(sampleTrack, isPlaying = false)
        assertEquals("Midnight Horizon", titlePaused)
        assertEquals(R.drawable.ic_widget_play, iconPaused)

        val (titleNull, subNull, iconNull) = computeWidgetPresentation(null, isPlaying = false)
        assertEquals("MIORA Player", titleNull)
        assertEquals("No track playing", subNull)
        assertEquals(R.drawable.ic_widget_play, iconNull)
    }
}
