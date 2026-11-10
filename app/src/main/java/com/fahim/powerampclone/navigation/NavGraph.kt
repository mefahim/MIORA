package com.fahim.powerampclone.navigation

/**
 * Navigation routes for MIORA.
 * Covers primary destinations (Home, Library, Search, Queue, Settings, Player)
 * and category/detail/sheet screens.
 */
object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val SONGS = "songs"
    const val ALBUMS = "albums"
    const val ARTISTS = "artists"
    const val PLAYLISTS = "playlists"
    const val FOLDERS = "folders"
    const val GENRES = "genres"
    const val SEARCH = "search"
    const val QUEUE = "queue"
    const val PLAYER = "player"
    const val SETTINGS = "settings"
    const val EQUALIZER = "equalizer"
    const val LYRICS = "lyrics"
    const val AUDIO_INFO = "audio_info"
    const val ALBUM_DETAIL = "album/{albumId}"
    const val ARTIST_DETAIL = "artist/{artistId}"
    const val PLAYLIST_DETAIL = "playlist/{playlistId}"

    fun album(id: Long) = "album/$id"
    fun artist(id: Long) = "artist/$id"
    fun playlist(id: Long) = "playlist/$id"
}
