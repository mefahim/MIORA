package com.fahim.powerampclone.data

import androidx.compose.ui.graphics.Color
import com.fahim.powerampclone.model.Album
import com.fahim.powerampclone.model.Artist
import com.fahim.powerampclone.model.Genre
import com.fahim.powerampclone.model.LyricLine
import com.fahim.powerampclone.model.MusicFolder
import com.fahim.powerampclone.model.Playlist
import com.fahim.powerampclone.model.Track

/**
 * Deterministic sample library. All titles/artists are original sample data
 * created for development — no proprietary assets, no real audio files.
 * Artwork is generated in-app from [Track.artworkSeed] (see AlbumArt).
 */
object MockData {
    private fun lyrics(vararg lines: Pair<Long, String>) = lines.map { LyricLine(it.first, it.second) }

    val tracks: List<Track> = listOf(
        Track(1, "Sky Dreams", "Aurora Lane", "Dreamscape", 1, 1, 232_000, 1, 2024, "Ambient", "/Music/Aurora Lane/Dreamscape", true, Color(0xFF3B82F6), lyrics = lyrics(0L to "Walking where the morning breaks", 8000L to "Golden light across the hills", 16000L to "We are made of sky dreams", 24000L to "Drifting on a quiet wind")),
        Track(2, "Better Tomorrow", "The Horizon", "Better Tomorrow", 2, 2, 198_000, 1, 2024, "Pop", "/Music/The Horizon/Better Tomorrow", false, Color(0xFFF59E0B), format = "MP3", bitrateKbps = 320, sampleRateHz = 44100, bitDepth = 16),
        Track(3, "Midnight Lights", "Nova Echo", "Midnight Lights", 3, 3, 214_000, 1, 2023, "Electronic", "/Music/Nova Echo/Midnight Lights", true, Color(0xFF8B5CF6), format = "FLAC", bitrateKbps = 1024, sampleRateHz = 48000, bitDepth = 24),
        Track(4, "Ocean Breeze", "Blue Horizon", "Ocean Breeze", 4, 4, 187_000, 1, 2024, "Chill", "/Music/Blue Horizon/Ocean Breeze", false, Color(0xFF06B6D4)),
        Track(5, "Feel the Beat", "Pulse Drive", "Feel the Beat", 5, 5, 176_000, 1, 2023, "Dance", "/Music/Pulse Drive/Feel the Beat", false, Color(0xFFEF4444), format = "MP3", bitrateKbps = 320),
        Track(6, "City Lights", "Urban Theory", "City Lights", 6, 6, 203_000, 1, 2024, "Electronic", "/Music/Urban Theory/City Lights", false, Color(0xFF6366F1)),
        Track(7, "Infinite Moment", "Dream State", "Infinite Moment", 7, 7, 241_000, 1, 2023, "Ambient", "/Music/Dream State/Infinite Moment", true, Color(0xFF10B981)),
        Track(8, "Golden Hour", "Aurora Lane", "Dreamscape", 1, 1, 209_000, 2, 2024, "Ambient", "/Music/Aurora Lane/Dreamscape", false, Color(0xFF3B82F6)),
        Track(9, "Northern Sky", "Aurora Lane", "Dreamscape", 1, 1, 226_000, 3, 2024, "Ambient", "/Music/Aurora Lane/Dreamscape", false, Color(0xFF3B82F6)),
        Track(10, "Rising Sun", "The Horizon", "Better Tomorrow", 2, 2, 192_000, 2, 2024, "Pop", "/Music/The Horizon/Better Tomorrow", false, Color(0xFFF59E0B)),
        Track(11, "Neon Streets", "Nova Echo", "Midnight Lights", 3, 3, 205_000, 2, 2023, "Electronic", "/Music/Nova Echo/Midnight Lights", false, Color(0xFF8B5CF6)),
        Track(12, "Tide Song", "Blue Horizon", "Ocean Breeze", 4, 4, 174_000, 2, 2024, "Chill", "/Music/Blue Horizon/Ocean Breeze", false, Color(0xFF06B6D4)),
        Track(13, "Night Drive", "Pulse Drive", "Feel the Beat", 5, 5, 189_000, 2, 2023, "Dance", "/Music/Pulse Drive/Feel the Beat", true, Color(0xFFEF4444)),
        Track(14, "Metro Lines", "Urban Theory", "City Lights", 6, 6, 211_000, 2, 2024, "Electronic", "/Music/Urban Theory/City Lights", false, Color(0xFF6366F1)),
        Track(15, "Slow Orbit", "Dream State", "Infinite Moment", 7, 7, 238_000, 2, 2023, "Ambient", "/Music/Dream State/Infinite Moment", false, Color(0xFF10B981)),
        Track(16, "Paper Planes", "The Horizon", "Better Tomorrow", 2, 2, 183_000, 3, 2024, "Pop", "/Music/The Horizon/Better Tomorrow", false, Color(0xFFF59E0B)),
        Track(17, "Starlight Run", "Nova Echo", "Midnight Lights", 3, 3, 197_000, 3, 2023, "Electronic", "/Music/Nova Echo/Midnight Lights", false, Color(0xFF8B5CF6)),
        Track(18, "Salt Air", "Blue Horizon", "Ocean Breeze", 4, 4, 169_000, 3, 2024, "Chill", "/Music/Blue Horizon/Ocean Breeze", false, Color(0xFF06B6D4)),
        Track(19, "Afterglow", "Aurora Lane", "Dreamscape", 1, 1, 219_000, 4, 2024, "Ambient", "/Music/Aurora Lane/Dreamscape", false, Color(0xFF3B82F6)),
        Track(20, "Uptown Static", "Urban Theory", "City Lights", 6, 6, 194_000, 3, 2024, "Electronic", "/Music/Urban Theory/City Lights", false, Color(0xFF6366F1)),
    )

    val albums: List<Album> = listOf(
        Album(1, "Dreamscape", "Aurora Lane", 1, 2024, Color(0xFF3B82F6), listOf(1, 8, 9, 19)),
        Album(2, "Better Tomorrow", "The Horizon", 2, 2024, Color(0xFFF59E0B), listOf(2, 10, 16)),
        Album(3, "Midnight Lights", "Nova Echo", 3, 2023, Color(0xFF8B5CF6), listOf(3, 11, 17)),
        Album(4, "Ocean Breeze", "Blue Horizon", 4, 2024, Color(0xFF06B6D4), listOf(4, 12, 18)),
        Album(5, "Feel the Beat", "Pulse Drive", 5, 2023, Color(0xFFEF4444), listOf(5, 13)),
        Album(6, "City Lights", "Urban Theory", 6, 2024, Color(0xFF6366F1), listOf(6, 14, 20)),
        Album(7, "Infinite Moment", "Dream State", 7, 2023, Color(0xFF10B981), listOf(7, 15)),
    )

    val artists: List<Artist> = listOf(
        Artist(1, "Aurora Lane", Color(0xFF3B82F6), listOf(1), listOf(1, 8, 9, 19)),
        Artist(2, "The Horizon", Color(0xFFF59E0B), listOf(2), listOf(2, 10, 16)),
        Artist(3, "Nova Echo", Color(0xFF8B5CF6), listOf(3), listOf(3, 11, 17)),
        Artist(4, "Blue Horizon", Color(0xFF06B6D4), listOf(4), listOf(4, 12, 18)),
        Artist(5, "Pulse Drive", Color(0xFFEF4444), listOf(5), listOf(5, 13)),
        Artist(6, "Urban Theory", Color(0xFF6366F1), listOf(6), listOf(6, 14, 20)),
        Artist(7, "Dream State", Color(0xFF10B981), listOf(7), listOf(7, 15)),
    )

    val playlists: List<Playlist> = listOf(
        Playlist(1, "Chill Vibes", Color(0xFF06B6D4), listOf(1, 4, 8, 12, 15, 18, 19)),
        Playlist(2, "Focus Flow", Color(0xFF10B981), listOf(1, 7, 8, 9, 15, 19)),
        Playlist(3, "Dreamscape", Color(0xFF3B82F6), listOf(1, 3, 7, 8, 9, 11, 19)),
        Playlist(4, "Recently Played", Color(0xFFF59E0B), listOf(1, 2, 3, 4, 5, 6), isSmart = true),
        Playlist(5, "Favourites", Color(0xFFEF4444), listOf(1, 3, 7, 13), isSmart = true),
    )

    val genres: List<Genre> = listOf(
        Genre("Ambient", listOf(1, 7, 8, 9, 15, 19)),
        Genre("Pop", listOf(2, 10, 16)),
        Genre("Electronic", listOf(3, 6, 11, 14, 17, 20)),
        Genre("Chill", listOf(4, 12, 18)),
        Genre("Dance", listOf(5, 13)),
    )

    val folders: List<MusicFolder> = listOf(
        MusicFolder("/Music/Aurora Lane/Dreamscape", listOf(1, 8, 9, 19)),
        MusicFolder("/Music/The Horizon/Better Tomorrow", listOf(2, 10, 16)),
        MusicFolder("/Music/Nova Echo/Midnight Lights", listOf(3, 11, 17)),
        MusicFolder("/Music/Blue Horizon/Ocean Breeze", listOf(4, 12, 18)),
        MusicFolder("/Music/Pulse Drive/Feel the Beat", listOf(5, 13)),
        MusicFolder("/Music/Urban Theory/City Lights", listOf(6, 14, 20)),
        MusicFolder("/Music/Dream State/Infinite Moment", listOf(7, 15)),
    )

    val recentlyPlayedIds: List<Long> = listOf(1, 2, 3, 4, 5, 6)
}

/** Repository contract — a real MediaStore/Room implementation can replace the mock without touching UI. */
interface MusicRepository {
    fun tracks(): List<Track>
    fun albums(): List<Album>
    fun artists(): List<Artist>
    fun playlists(): List<Playlist>
    fun genres(): List<Genre>
    fun folders(): List<MusicFolder>
    fun recentlyPlayed(): List<Track>
    fun trackById(id: Long): Track?
    fun albumById(id: Long): Album?
    fun artistById(id: Long): Artist?
    fun playlistById(id: Long): Playlist?
    fun search(query: String): SearchResults
    data class SearchResults(val tracks: List<Track>, val albums: List<Album>, val artists: List<Artist>, val playlists: List<Playlist>)
}

class MockMusicRepository : MusicRepository {
    override fun tracks() = MockData.tracks
    override fun albums() = MockData.albums
    override fun artists() = MockData.artists
    override fun playlists() = MockData.playlists
    override fun genres() = MockData.genres
    override fun folders() = MockData.folders
    override fun recentlyPlayed() = MockData.recentlyPlayedIds.mapNotNull { trackById(it) }
    override fun trackById(id: Long) = MockData.tracks.firstOrNull { it.id == id }
    override fun albumById(id: Long) = MockData.albums.firstOrNull { it.id == id }
    override fun artistById(id: Long) = MockData.artists.firstOrNull { it.id == id }
    override fun playlistById(id: Long) = MockData.playlists.firstOrNull { it.id == id }
    override fun search(query: String): MusicRepository.SearchResults {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return MusicRepository.SearchResults(emptyList(), emptyList(), emptyList(), emptyList())
        return MusicRepository.SearchResults(
            tracks = MockData.tracks.filter { it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) || it.album.lowercase().contains(q) },
            albums = MockData.albums.filter { it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) },
            artists = MockData.artists.filter { it.name.lowercase().contains(q) },
            playlists = MockData.playlists.filter { it.name.lowercase().contains(q) },
        )
    }
}
