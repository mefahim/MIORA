package com.fahim.powerampclone.navigation

/** Navigation routes. The 66 declared activities from research/05 are classified here:
 *  8 user-facing screens become destinations; 22 dialogs become sheets/dialogs;
 *  widget config, licensing and third-party endpoints are out of Phase 2 scope. */
object Routes {
    const val HOME = "home"
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
