package com.fahim.powerampclone.model

import androidx.compose.ui.graphics.Color
import com.fahim.powerampclone.design.hslToColor
import kotlin.math.abs

/** Domain models — pure Kotlin, serializable, with real audio uri support. */
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long = 0L,
    val artistId: Long = 0L,
    val durationMs: Long = 180000L,
    val trackNumber: Int = 1,
    val year: Int = 2024,
    val genre: String = "Music",
    val folderPath: String = "/Music",
    val isFavorite: Boolean = false,
    /** Deterministic seed colour for the original placeholder artwork. */
    val artworkSeed: Color = seedColorForString(album),
    /** Real or simulated format metadata. */
    val format: String = "FLAC",
    val bitrateKbps: Int = 912,
    val sampleRateHz: Int = 44100,
    val bitDepth: Int = 16,
    val lyrics: List<LyricLine> = emptyList(),
    /** Real content URI string for Android media playback (e.g. content://media/external/audio/media/123) */
    val contentUriString: String = "",
    /** Album art URI string if present in MediaStore or local cache */
    val artworkUriString: String? = null,
)

data class LyricLine(val startMs: Long, val text: String)

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long,
    val year: Int,
    val artworkSeed: Color,
    val trackIds: List<Long>,
    val artworkUriString: String? = null,
)

data class Artist(
    val id: Long,
    val name: String,
    val artworkSeed: Color,
    val albumIds: List<Long>,
    val trackIds: List<Long>,
)

data class Playlist(
    val id: Long,
    val name: String,
    val artworkSeed: Color,
    val trackIds: List<Long>,
    val isSmart: Boolean = false,
)

data class Genre(val name: String, val trackIds: List<Long>)
data class MusicFolder(val path: String, val trackIds: List<Long>)

enum class RepeatMode { OFF, ALL, ONE }
enum class SortOrder { TITLE, ARTIST, DURATION, RECENTLY_ADDED, TRACK_NUMBER }

fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

/** Generates a deterministic, vibrant HSL seed color for any text key (e.g. title or album) */
fun seedColorForString(key: String): Color {
    val hash = key.hashCode()
    val hue = (abs(hash) % 360).toFloat()
    return hslToColor(hue, 68f, 42f)
}
