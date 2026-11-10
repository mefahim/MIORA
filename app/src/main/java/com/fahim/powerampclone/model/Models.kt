package com.fahim.powerampclone.model

import androidx.compose.ui.graphics.Color

/** Domain models — pure Kotlin, no Android dependencies, ready for a real engine later. */
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val artistId: Long,
    val durationMs: Long,
    val trackNumber: Int,
    val year: Int,
    val genre: String,
    val folderPath: String,
    val isFavorite: Boolean = false,
    /** Deterministic seed colour for the original placeholder artwork. */
    val artworkSeed: Color,
    /** Simulated format metadata — labelled as sample data in the Audio Info dialog. */
    val format: String = "FLAC",
    val bitrateKbps: Int = 912,
    val sampleRateHz: Int = 44100,
    val bitDepth: Int = 16,
    val lyrics: List<LyricLine> = emptyList(),
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
