package com.fahim.powerampclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fahim.powerampclone.data.SettingsStore
import com.fahim.powerampclone.design.PowerampCloneTheme
import com.fahim.powerampclone.library.LibraryViewModel
import com.fahim.powerampclone.library.SearchViewModel
import com.fahim.powerampclone.library.SettingsViewModel
import com.fahim.powerampclone.navigation.Routes
import com.fahim.powerampclone.player.PlayerViewModel
import com.fahim.powerampclone.ui.AlbumsScreen
import com.fahim.powerampclone.ui.ArtistsScreen
import com.fahim.powerampclone.ui.AudioInfoScreen
import com.fahim.powerampclone.ui.DetailScreen
import com.fahim.powerampclone.ui.EqualizerScreen
import com.fahim.powerampclone.ui.FoldersScreen
import com.fahim.powerampclone.ui.GenresScreen
import com.fahim.powerampclone.ui.HomeScreen
import com.fahim.powerampclone.ui.LyricsScreen
import com.fahim.powerampclone.ui.MiniPlayer
import com.fahim.powerampclone.ui.NowPlayingScreen
import com.fahim.powerampclone.ui.PlaylistsScreen
import com.fahim.powerampclone.ui.QueueScreen
import com.fahim.powerampclone.ui.SearchScreen
import com.fahim.powerampclone.ui.SettingsScreen
import com.fahim.powerampclone.ui.SongsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { PowerampCloneApp() }
    }
}

private class SettingsViewModelFactory(private val store: SettingsStore) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(store) as T
}

private data class BottomNavItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.HOME, "Home", Icons.Filled.Home),
    BottomNavItem(Routes.SEARCH, "Search", Icons.Filled.Search),
    BottomNavItem(Routes.QUEUE, "Queue", Icons.Filled.QueueMusic),
    BottomNavItem(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
)

@Composable
fun PowerampCloneApp() {
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = viewModel()
    val libraryViewModel: LibraryViewModel = viewModel()
    val searchViewModel: SearchViewModel = viewModel()
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsStore = remember { SettingsStore(context.applicationContext) }
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(settingsStore))
    val settingsState by settingsViewModel.state.collectAsState()
    val playerState by playerViewModel.state.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Artwork-derived accent: the Now Playing screen tints the theme from the current track.
    val artworkSeed = if (currentRoute == Routes.PLAYER) playerState.currentTrack?.artworkSeed else null

    PowerampCloneTheme(
        themeMode = settingsState.themeMode,
        dynamicColor = settingsState.dynamicColor,
        accent = settingsState.accent,
        artworkSeed = artworkSeed,
    ) {
        Scaffold(
            bottomBar = {
                if (currentRoute in bottomNavItems.map { it.route }) {
                    Column {
                        MiniPlayer(playerViewModel = playerViewModel, onExpand = { navController.navigate(Routes.PLAYER) })
                        NavigationBar {
                            bottomNavItems.forEach { item ->
                                NavigationBarItem(
                                    selected = currentRoute == item.route,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) },
                                )
                            }
                        }
                    }
                }
            },
        ) { padding ->
            AppNavHost(
                navController = navController,
                playerViewModel = playerViewModel,
                libraryViewModel = libraryViewModel,
                searchViewModel = searchViewModel,
                settingsViewModel = settingsViewModel,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    playerViewModel: PlayerViewModel,
    libraryViewModel: LibraryViewModel,
    searchViewModel: SearchViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val navigate: (String) -> Unit = { navController.navigate(it) }
    val back: () -> Unit = { navController.popBackStack() }
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
        enterTransition = { slideInHorizontally { it / 3 } + fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { slideOutHorizontally { it / 3 } + fadeOut() },
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                libraryViewModel = libraryViewModel,
                playerViewModel = playerViewModel,
                onNavigate = navigate,
                onOpenPlayer = { navigate(Routes.PLAYER) },
                onSearch = { navigate(Routes.SEARCH) },
            )
        }
        composable(Routes.SONGS) { SongsScreen(libraryViewModel, playerViewModel, back) { navigate(Routes.PLAYER) } }
        composable(Routes.ALBUMS) { AlbumsScreen(libraryViewModel, playerViewModel, back) { navigate(Routes.album(it)) } }
        composable(Routes.ARTISTS) { ArtistsScreen(libraryViewModel, back) { navigate(Routes.artist(it)) } }
        composable(Routes.PLAYLISTS) { PlaylistsScreen(libraryViewModel, back) { navigate(Routes.playlist(it)) } }
        composable(Routes.FOLDERS) { FoldersScreen(libraryViewModel, playerViewModel, back) { navigate(Routes.PLAYER) } }
        composable(Routes.GENRES) { GenresScreen(libraryViewModel, playerViewModel, back) { navigate(Routes.PLAYER) } }
        composable(Routes.SEARCH) {
            SearchScreen(searchViewModel, playerViewModel, back, { navigate(Routes.album(it)) }, { navigate(Routes.artist(it)) }, { navigate(Routes.playlist(it)) }) { navigate(Routes.PLAYER) }
        }
        composable(Routes.QUEUE) { QueueScreen(playerViewModel, back) { navigate(Routes.PLAYER) } }
        composable(Routes.PLAYER) { NowPlayingScreen(playerViewModel, back, navigate) }
        composable(Routes.SETTINGS) { SettingsScreen(settingsViewModel) { navigate(Routes.EQUALIZER) } }
        composable(Routes.EQUALIZER) { EqualizerScreen(back) }
        composable(Routes.LYRICS) { LyricsScreen(playerViewModel, back) }
        composable(Routes.AUDIO_INFO) { AudioInfoScreen(playerViewModel, back) }
        composable(
            Routes.ALBUM_DETAIL,
            arguments = listOf(navArgument("albumId") { type = NavType.LongType }),
        ) { entry ->
            val album = libraryViewModel.albumById(entry.arguments?.getLong("albumId") ?: -1)
            if (album != null) {
                DetailScreen(
                    title = album.title, subtitle = album.artist, seed = album.artworkSeed,
                    trackCountLabel = "${album.trackIds.size} tracks • ${album.year}",
                    tracks = libraryViewModel.tracksByIds(album.trackIds),
                    playerViewModel = playerViewModel, onBack = back, onOpenPlayer = { navigate(Routes.PLAYER) },
                )
            }
        }
        composable(
            Routes.ARTIST_DETAIL,
            arguments = listOf(navArgument("artistId") { type = NavType.LongType }),
        ) { entry ->
            val artist = libraryViewModel.artistById(entry.arguments?.getLong("artistId") ?: -1)
            if (artist != null) {
                DetailScreen(
                    title = artist.name, subtitle = "${artist.albumIds.size} albums", seed = artist.artworkSeed,
                    trackCountLabel = "${artist.trackIds.size} tracks",
                    tracks = libraryViewModel.tracksByIds(artist.trackIds),
                    playerViewModel = playerViewModel, onBack = back, onOpenPlayer = { navigate(Routes.PLAYER) },
                )
            }
        }
        composable(
            Routes.PLAYLIST_DETAIL,
            arguments = listOf(navArgument("playlistId") { type = NavType.LongType }),
        ) { entry ->
            val playlist = libraryViewModel.playlistById(entry.arguments?.getLong("playlistId") ?: -1)
            if (playlist != null) {
                DetailScreen(
                    title = playlist.name, subtitle = if (playlist.isSmart) "Smart playlist" else "Playlist", seed = playlist.artworkSeed,
                    trackCountLabel = "${playlist.trackIds.size} tracks",
                    tracks = libraryViewModel.tracksByIds(playlist.trackIds),
                    playerViewModel = playerViewModel, onBack = back, onOpenPlayer = { navigate(Routes.PLAYER) },
                )
            }
        }
    }
}
