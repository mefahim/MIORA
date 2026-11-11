package com.fahim.powerampclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
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
import com.fahim.powerampclone.data.MusicRepository
import com.fahim.powerampclone.data.SettingsStore
import com.fahim.powerampclone.data.local.MediaStoreMusicRepository
import com.fahim.powerampclone.data.local.PermissionHelper
import com.fahim.powerampclone.design.MioraTheme
import com.fahim.powerampclone.library.LibraryViewModel
import com.fahim.powerampclone.library.SearchViewModel
import com.fahim.powerampclone.library.SettingsViewModel
import com.fahim.powerampclone.navigation.Routes
import com.fahim.powerampclone.playback.PlaybackManager
import com.fahim.powerampclone.player.PlayerViewModel
import com.fahim.powerampclone.update.UpdateManager
import com.fahim.powerampclone.ui.AlbumsScreen
import com.fahim.powerampclone.ui.ArtistsScreen
import com.fahim.powerampclone.ui.AudioInfoScreen
import com.fahim.powerampclone.ui.DetailScreen
import com.fahim.powerampclone.ui.EqualizerScreen
import com.fahim.powerampclone.ui.FoldersScreen
import com.fahim.powerampclone.ui.GenresScreen
import com.fahim.powerampclone.ui.HomeScreen
import com.fahim.powerampclone.ui.LibraryScreen
import com.fahim.powerampclone.ui.LyricsScreen
import com.fahim.powerampclone.ui.MiniPlayer
import com.fahim.powerampclone.ui.NowPlayingScreen
import com.fahim.powerampclone.ui.PlaylistsScreen
import com.fahim.powerampclone.ui.PlaylistDetailScreen
import com.fahim.powerampclone.ui.QueueScreen
import com.fahim.powerampclone.ui.SearchScreen
import com.fahim.powerampclone.ui.SettingsScreen
import com.fahim.powerampclone.ui.SongsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MioraApp() }
    }
}

private class SettingsViewModelFactory(
    private val store: SettingsStore,
    private val updateManager: UpdateManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(store, updateManager) as T
}

private class MusicViewModelFactory(
    private val repository: MusicRepository,
    private val playbackManager: PlaybackManager,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LibraryViewModel::class.java) -> LibraryViewModel(repository) as T
            modelClass.isAssignableFrom(PlayerViewModel::class.java) -> PlayerViewModel(repository, playbackManager) as T
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> SearchViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
        }
    }
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.HOME, "Home", Icons.Filled.Home),
    BottomNavItem(Routes.LIBRARY, "Library", Icons.Filled.LibraryMusic),
    BottomNavItem(Routes.SEARCH, "Search", Icons.Filled.Search),
    BottomNavItem(Routes.QUEUE, "Queue", Icons.AutoMirrored.Filled.QueueMusic),
)

@Composable
fun MioraApp() {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { MediaStoreMusicRepository(context.applicationContext) }
    val playbackManager = remember { PlaybackManager.getInstance(context.applicationContext, repository) }
    val musicFactory = remember(repository, playbackManager) { MusicViewModelFactory(repository, playbackManager) }

    val playerViewModel: PlayerViewModel = viewModel(factory = musicFactory)
    val libraryViewModel: LibraryViewModel = viewModel(factory = musicFactory)
    val searchViewModel: SearchViewModel = viewModel(factory = musicFactory)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { _ ->
        libraryViewModel.refresh()
        val scanned = libraryViewModel.allTracks
        if (scanned.isNotEmpty()) {
            playerViewModel.setQueue(scanned)
        }
    }

    val onRequestPermission = {
        permissionLauncher.launch(PermissionHelper.requiredMediaPermission())
    }

    val settingsStore = remember { SettingsStore(context.applicationContext) }
    val updateManager = remember { UpdateManager(context.applicationContext) }
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(settingsStore, updateManager))
    val settingsState by settingsViewModel.state.collectAsState()
    val playerState by playerViewModel.state.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Artwork-derived accent tints the app when playing
    val artworkSeed = if (currentRoute == Routes.PLAYER) playerState.currentTrack?.artworkSeed else null

    MioraTheme(
        themeMode = settingsState.themeMode,
        dynamicColor = settingsState.dynamicColor,
        accent = settingsState.accent,
        artworkSeed = artworkSeed,
    ) {
        Scaffold(
            bottomBar = {
                if (currentRoute in bottomNavItems.map { it.route }) {
                    Column {
                        MiniPlayer(
                            playerViewModel = playerViewModel,
                            onExpand = { navController.navigate(Routes.PLAYER) },
                        )
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
                onRequestPermission = onRequestPermission,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        }
    }
}

/** Backward-compatibility alias */
@Composable
fun PowerampCloneApp() {
    MioraApp()
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    playerViewModel: PlayerViewModel,
    libraryViewModel: LibraryViewModel,
    searchViewModel: SearchViewModel,
    settingsViewModel: SettingsViewModel,
    onRequestPermission: () -> Unit = {},
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
                onOpenSettings = { navigate(Routes.SETTINGS) },
                onRequestPermission = onRequestPermission,
            )
        }
        composable(Routes.LIBRARY) {
            LibraryScreen(
                libraryViewModel = libraryViewModel,
                onNavigate = navigate,
                onOpenSettings = { navigate(Routes.SETTINGS) },
                onRequestPermission = onRequestPermission,
            )
        }
        composable(Routes.SONGS) {
            SongsScreen(libraryViewModel, playerViewModel, back) { navigate(Routes.PLAYER) }
        }
        composable(Routes.ALBUMS) {
            AlbumsScreen(libraryViewModel, playerViewModel, back) { navigate(Routes.album(it)) }
        }
        composable(Routes.ARTISTS) {
            ArtistsScreen(libraryViewModel, back) { navigate(Routes.artist(it)) }
        }
        composable(Routes.PLAYLISTS) {
            PlaylistsScreen(
                libraryViewModel = libraryViewModel,
                playerViewModel = playerViewModel,
                onBack = back,
                onPlaylist = { navigate(Routes.playlist(it)) },
                onOpenPlayer = { navigate(Routes.PLAYER) },
            )
        }
        composable(Routes.FOLDERS) {
            FoldersScreen(libraryViewModel, playerViewModel, back) { navigate(Routes.PLAYER) }
        }
        composable(Routes.GENRES) {
            GenresScreen(libraryViewModel, playerViewModel, back) { navigate(Routes.PLAYER) }
        }
        composable(Routes.SEARCH) {
            SearchScreen(
                searchViewModel = searchViewModel,
                playerViewModel = playerViewModel,
                onBack = back,
                onAlbum = { navigate(Routes.album(it)) },
                onArtist = { navigate(Routes.artist(it)) },
                onPlaylist = { navigate(Routes.playlist(it)) },
                onOpenPlayer = { navigate(Routes.PLAYER) },
            )
        }
        composable(Routes.QUEUE) {
            QueueScreen(playerViewModel, back) { navigate(Routes.PLAYER) }
        }
        composable(Routes.PLAYER) {
            NowPlayingScreen(playerViewModel, back, navigate)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(settingsViewModel, onBack = back) { navigate(Routes.EQUALIZER) }
        }
        composable(Routes.EQUALIZER) {
            EqualizerScreen(playerViewModel, back)
        }
        composable(Routes.LYRICS) {
            LyricsScreen(playerViewModel, back)
        }
        composable(Routes.AUDIO_INFO) {
            AudioInfoScreen(playerViewModel, back)
        }
        composable(
            Routes.ALBUM_DETAIL,
            arguments = listOf(navArgument("albumId") { type = NavType.LongType }),
        ) { entry ->
            val albumId = entry.arguments?.getLong("albumId") ?: -1
            val album = libraryViewModel.albumById(albumId)
            if (album != null) {
                DetailScreen(
                    title = album.title,
                    subtitle = album.artist,
                    seed = album.artworkSeed,
                    trackCountLabel = "${album.trackIds.size} tracks • ${album.year}",
                    tracks = libraryViewModel.tracksByIds(album.trackIds),
                    playerViewModel = playerViewModel,
                    onBack = back,
                    onOpenPlayer = { navigate(Routes.PLAYER) },
                    albumId = album.id,
                )
            }
        }
        composable(
            Routes.ARTIST_DETAIL,
            arguments = listOf(navArgument("artistId") { type = NavType.LongType }),
        ) { entry ->
            val artistId = entry.arguments?.getLong("artistId") ?: -1
            val artist = libraryViewModel.artistById(artistId)
            if (artist != null) {
                DetailScreen(
                    title = artist.name,
                    subtitle = "${artist.albumIds.size} albums",
                    seed = artist.artworkSeed,
                    trackCountLabel = "${artist.trackIds.size} tracks",
                    tracks = libraryViewModel.tracksByIds(artist.trackIds),
                    playerViewModel = playerViewModel,
                    onBack = back,
                    onOpenPlayer = { navigate(Routes.PLAYER) },
                    albumId = artist.albumIds.firstOrNull(),
                )
            }
        }
        composable(
            Routes.PLAYLIST_DETAIL,
            arguments = listOf(navArgument("playlistId") { type = NavType.LongType }),
        ) { entry ->
            val playlistId = entry.arguments?.getLong("playlistId") ?: -1
            PlaylistDetailScreen(
                playlistId = playlistId,
                libraryViewModel = libraryViewModel,
                playerViewModel = playerViewModel,
                onBack = back,
                onOpenPlayer = { navigate(Routes.PLAYER) },
            )
        }
    }
}
