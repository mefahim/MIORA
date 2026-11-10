package com.fahim.powerampclone.data.local

import com.fahim.powerampclone.model.Album
import com.fahim.powerampclone.model.Artist
import com.fahim.powerampclone.model.Genre
import com.fahim.powerampclone.model.MusicFolder
import com.fahim.powerampclone.model.Track
import com.fahim.powerampclone.model.seedColorForString

data class AggregatedLibrary(
    val tracks: List<Track>,
    val albums: List<Album>,
    val artists: List<Artist>,
    val folders: List<MusicFolder>,
    val genres: List<Genre>,
)

object LocalMediaAggregator {
    /**
     * Groups a flat list of scanned audio tracks into canonical Album, Artist,
     * Folder, and Genre structures without losing any track relationships.
     */
    fun aggregate(tracks: List<Track>): AggregatedLibrary {
        if (tracks.isEmpty()) {
            return AggregatedLibrary(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        }

        // Group into Albums
        val albums = tracks.groupBy { if (it.albumId > 0) it.albumId else it.album.hashCode().toLong() }
            .map { (albumId, albumTracks) ->
                val first = albumTracks.first()
                val albumTitle = first.album.ifBlank { "Unknown Album" }
                val albumArtist = first.artist.ifBlank { "Unknown Artist" }
                val maxYear = albumTracks.maxOfOrNull { it.year } ?: 0
                Album(
                    id = albumId,
                    title = albumTitle,
                    artist = albumArtist,
                    artistId = first.artistId,
                    year = maxYear,
                    artworkSeed = seedColorForString(albumTitle),
                    trackIds = albumTracks.map { it.id },
                    artworkUriString = first.artworkUriString,
                )
            }.sortedBy { it.title.lowercase() }

        // Group into Artists
        val artists = tracks.groupBy { if (it.artistId > 0) it.artistId else it.artist.hashCode().toLong() }
            .map { (artistId, artistTracks) ->
                val artistName = artistTracks.first().artist.ifBlank { "Unknown Artist" }
                val albumIds = artistTracks.map { it.albumId }.distinct()
                Artist(
                    id = artistId,
                    name = artistName,
                    artworkSeed = seedColorForString(artistName),
                    albumIds = albumIds,
                    trackIds = artistTracks.map { it.id },
                )
            }.sortedBy { it.name.lowercase() }

        // Group into Folders
        val folders = tracks.groupBy { it.folderPath.ifBlank { "/Music" } }
            .map { (path, folderTracks) ->
                MusicFolder(
                    path = path,
                    trackIds = folderTracks.map { it.id },
                )
            }.sortedBy { it.path.lowercase() }

        // Group into Genres
        val genres = tracks.groupBy { it.genre.ifBlank { "Unknown" } }
            .map { (name, genreTracks) ->
                Genre(
                    name = name,
                    trackIds = genreTracks.map { it.id },
                )
            }.sortedBy { it.name.lowercase() }

        return AggregatedLibrary(
            tracks = tracks,
            albums = albums,
            artists = artists,
            folders = folders,
            genres = genres,
        )
    }
}
