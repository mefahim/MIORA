package com.fahim.powerampclone.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fahim.powerampclone.design.Spacing
import com.fahim.powerampclone.library.LibraryViewModel
import com.fahim.powerampclone.model.SortOrder
import com.fahim.powerampclone.model.formatDuration
import com.fahim.powerampclone.navigation.Routes
import com.fahim.powerampclone.player.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryScaffold(title: String, onBack: () -> Unit, sortMenu: @Composable (() -> Unit)? = null, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
                actions = { sortMenu?.invoke() },
            )
        },
    ) { padding -> content(padding) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(libraryViewModel: LibraryViewModel, playerViewModel: PlayerViewModel, onBack: () -> Unit, onOpenPlayer: () -> Unit) {
    val state by libraryViewModel.state.collectAsState()
    val playerState by playerViewModel.state.collectAsState()
    var sortExpanded by remember { mutableStateOf(false) }
    LibraryScaffold(
        title = if (state.selectionMode) "${state.selectedIds.size} selected" else "Songs",
        onBack = { if (state.selectionMode) libraryViewModel.clearSelection() else onBack() },
        sortMenu = {
            IconButton(onClick = { sortExpanded = true }) { Icon(Icons.Filled.Sort, contentDescription = "Sort songs") }
            DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                SortOrder.entries.forEach { order ->
                    DropdownMenuItem(
                        text = { Text(order.name.lowercase().replaceFirstChar { it.uppercase() }.replace('_', ' ')) },
                        onClick = { libraryViewModel.setSortOrder(order); sortExpanded = false },
                    )
                }
            }
        },
    ) { padding ->
        val tracks = libraryViewModel.sortedTracks()
        if (tracks.isEmpty()) {
            EmptyState("No songs yet", "Add music to your library to see songs here.", Modifier.padding(padding))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(vertical = Spacing.sm)) {
                items(tracks) { track ->
                    TrackRow(
                        title = track.title,
                        subtitle = "${track.artist} • ${track.album}",
                        durationMs = track.durationMs,
                        seed = track.artworkSeed,
                        isPlaying = playerState.currentTrack?.id == track.id && playerState.isPlaying,
                        isFavorite = track.id in playerState.favorites,
                        isSelected = track.id in state.selectedIds,
                        onClick = {
                            if (state.selectionMode) libraryViewModel.toggleSelect(track.id)
                            else { playerViewModel.playTrack(track); onOpenPlayer() }
                        },
                        onLongClick = { libraryViewModel.toggleSelect(track.id) },
                        modifier = Modifier.padding(horizontal = Spacing.sm),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(libraryViewModel: LibraryViewModel, playerViewModel: PlayerViewModel, onBack: () -> Unit, onAlbum: (Long) -> Unit) {
    LibraryScaffold(title = "Albums", onBack = onBack) { padding ->
        LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 160.dp), modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            items(libraryViewModel.albums) { album ->
                Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { onAlbum(album.id) }) {
                    AlbumArt(seed = album.artworkSeed, title = album.title, size = 160.dp, cornerRadius = 16.dp)
                    Spacer(Modifier.height(Spacing.sm))
                    Text(text = album.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = "${album.artist} • ${album.year}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = "${album.trackIds.size} tracks", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistsScreen(libraryViewModel: LibraryViewModel, onBack: () -> Unit, onArtist: (Long) -> Unit) {
    LibraryScaffold(title = "Artists", onBack = onBack) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(libraryViewModel.artists) { artist ->
                Row(modifier = Modifier.fillMaxWidth().clickable { onArtist(artist.id) }.padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                    AlbumArt(seed = artist.artworkSeed, title = artist.name, size = 56.dp, cornerRadius = 28.dp)
                    Spacer(Modifier.width(Spacing.lg))
                    Column(Modifier.weight(1f)) {
                        Text(text = artist.name, style = MaterialTheme.typography.bodyLarge)
                        Text(text = "${artist.albumIds.size} albums • ${artist.trackIds.size} tracks", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(libraryViewModel: LibraryViewModel, onBack: () -> Unit, onPlaylist: (Long) -> Unit) {
    LibraryScaffold(title = "Playlists", onBack = onBack) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(libraryViewModel.playlists) { playlist ->
                Row(modifier = Modifier.fillMaxWidth().clickable { onPlaylist(playlist.id) }.padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                    AlbumArt(seed = playlist.artworkSeed, title = playlist.name, size = 56.dp, cornerRadius = 12.dp)
                    Spacer(Modifier.width(Spacing.lg))
                    Column(Modifier.weight(1f)) {
                        Text(text = playlist.name, style = MaterialTheme.typography.bodyLarge)
                        Text(text = "${playlist.trackIds.size} tracks" + if (playlist.isSmart) " • Smart playlist" else "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(libraryViewModel: LibraryViewModel, playerViewModel: PlayerViewModel, onBack: () -> Unit, onOpenPlayer: () -> Unit) {
    LibraryScaffold(title = "Folders", onBack = onBack) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(libraryViewModel.folders) { folder ->
                Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(Spacing.md))
                        Text(text = folder.path, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.width(Spacing.md))
                        Text(text = "${folder.trackIds.size}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    libraryViewModel.tracksByIds(folder.trackIds).forEach { track ->
                        val playerState by playerViewModel.state.collectAsState()
                        TrackRow(
                            title = track.title, subtitle = track.artist, durationMs = track.durationMs,
                            seed = track.artworkSeed,
                            isPlaying = playerState.currentTrack?.id == track.id && playerState.isPlaying,
                            isFavorite = track.id in playerState.favorites, isSelected = false,
                            onClick = { playerViewModel.playTrack(track); onOpenPlayer() },
                            modifier = Modifier.padding(start = Spacing.xl),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenresScreen(libraryViewModel: LibraryViewModel, playerViewModel: PlayerViewModel, onBack: () -> Unit, onOpenPlayer: () -> Unit) {
    LibraryScaffold(title = "Genres", onBack = onBack) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(libraryViewModel.genres) { genre ->
                Column(Modifier.fillMaxWidth().padding(Spacing.lg)) {
                    Text(text = genre.name, style = MaterialTheme.typography.titleMedium)
                    Text(text = "${genre.trackIds.size} tracks", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(Spacing.sm))
                    libraryViewModel.tracksByIds(genre.trackIds).forEach { track ->
                        val playerState by playerViewModel.state.collectAsState()
                        TrackRow(
                            title = track.title, subtitle = track.artist, durationMs = track.durationMs,
                            seed = track.artworkSeed,
                            isPlaying = playerState.currentTrack?.id == track.id && playerState.isPlaying,
                            isFavorite = track.id in playerState.favorites, isSelected = false,
                            onClick = { playerViewModel.playTrack(track); onOpenPlayer() },
                        )
                    }
                }
            }
        }
    }
}

/** Shared detail header used by album/artist/playlist detail screens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    title: String,
    subtitle: String,
    seed: androidx.compose.ui.graphics.Color,
    trackCountLabel: String,
    tracks: List<com.fahim.powerampclone.model.Track>,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit,
) {
    val playerState by playerViewModel.state.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = Spacing.xxl)) {
            item {
                Column(Modifier.fillMaxWidth().padding(Spacing.lg), horizontalAlignment = Alignment.CenterHorizontally) {
                    AlbumArt(seed = seed, title = title, size = 200.dp, cornerRadius = 20.dp)
                    Spacer(Modifier.height(Spacing.lg))
                    Text(text = title, style = MaterialTheme.typography.headlineMedium)
                    Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = trackCountLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(Spacing.lg))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        Button(onClick = {
                            if (tracks.isNotEmpty()) { playerViewModel.playTrack(tracks.first(), tracks); onOpenPlayer() }
                        }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Text("Play all")
                        }
                        FilledTonalButton(onClick = {
                            if (tracks.isNotEmpty()) { playerViewModel.playTrack(tracks.random(), tracks.shuffled()); onOpenPlayer() }
                        }) {
                            Icon(Icons.Filled.Shuffle, contentDescription = null)
                            Text("Shuffle")
                        }
                    }
                }
            }
            items(tracks.withIndex().toList()) { (index, track) ->
                TrackRow(
                    title = track.title, subtitle = "${track.artist} • ${formatDuration(track.durationMs)}",
                    durationMs = track.durationMs, seed = track.artworkSeed,
                    isPlaying = playerState.currentTrack?.id == track.id && playerState.isPlaying,
                    isFavorite = track.id in playerState.favorites, isSelected = false,
                    leadingNumber = index + 1,
                    onClick = { playerViewModel.playTrack(track, tracks); onOpenPlayer() },
                    modifier = Modifier.padding(horizontal = Spacing.sm),
                )
            }
        }
    }
}
