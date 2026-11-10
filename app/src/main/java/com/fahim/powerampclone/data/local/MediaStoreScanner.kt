package com.fahim.powerampclone.data.local

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.fahim.powerampclone.model.Track
import com.fahim.powerampclone.model.seedColorForString
import java.io.File

object MediaStoreScanner {

    private val ALBUM_ART_URI = Uri.parse("content://media/external/audio/albumart")

    fun scanTracks(context: Context): List<Track> {
        if (!PermissionHelper.hasMediaPermission(context)) {
            return emptyList()
        }

        val tracks = mutableListOf<Track>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE,
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 5000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor: Cursor? = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder,
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val artistIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val trackCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val mimeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val title = c.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "Unknown Track"
                    val artist = c.getString(artistCol)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown Artist"
                    val album = c.getString(albumCol)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown Album"
                    val albumId = c.getLong(albumIdCol)
                    val artistId = c.getLong(artistIdCol)
                    val durationMs = c.getLong(durationCol)
                    val trackNumber = c.getInt(trackCol).let { if (it >= 1000) it % 1000 else it }
                    val year = c.getInt(yearCol)
                    val data = c.getString(dataCol) ?: ""
                    val mimeType = c.getString(mimeCol) ?: ""

                    val folderPath = try {
                        val file = File(data)
                        file.parent ?: "/Music"
                    } catch (_: Exception) {
                        "/Music"
                    }

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id,
                    ).toString()

                    val artworkUri = if (albumId > 0) {
                        ContentUris.withAppendedId(ALBUM_ART_URI, albumId).toString()
                    } else null

                    val format = parseFormat(data, mimeType)

                    tracks.add(
                        Track(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            albumId = albumId,
                            artistId = artistId,
                            durationMs = durationMs,
                            trackNumber = trackNumber,
                            year = if (year > 0) year else 2024,
                            genre = "Music",
                            folderPath = folderPath,
                            isFavorite = false,
                            artworkSeed = seedColorForString(album),
                            format = format,
                            contentUriString = contentUri,
                            artworkUriString = artworkUri,
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            // Log/handle permission or database query failures gracefully
        }

        return tracks
    }

    private fun parseFormat(filePath: String, mimeType: String): String {
        val ext = filePath.substringAfterLast('.', "").uppercase()
        return when {
            ext.isNotEmpty() && ext.length in 3..4 -> ext
            mimeType.contains("flac", ignoreCase = true) -> "FLAC"
            mimeType.contains("mp3", ignoreCase = true) || mimeType.contains("mpeg", ignoreCase = true) -> "MP3"
            mimeType.contains("ogg", ignoreCase = true) -> "OGG"
            mimeType.contains("wav", ignoreCase = true) -> "WAV"
            mimeType.contains("aac", ignoreCase = true) || mimeType.contains("mp4", ignoreCase = true) -> "M4A"
            else -> "AUDIO"
        }
    }
}
