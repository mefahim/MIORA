package com.fahim.powerampclone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahim.powerampclone.design.Spacing
import com.fahim.powerampclone.library.LibraryViewModel
import com.fahim.powerampclone.model.SortOrder
import com.fahim.powerampclone.model.formatDuration
import com.fahim.powerampclone.navigation.Routes
import com.fahim.powerampclone.player.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryScaffold(
    title: String,
    onBack: () -> Unit,
    sortMenu: @Composable (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = { sortMenu?.invoke() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding -> content(padding) }
}

/** Segmented Library Hub screen matching web-preview/js/library.js */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    libraryViewModel: LibraryViewModel,
    onNavigate: (String) -> Unit,
    onOpenSettings: () -> Unit = { onNavigate(Routes.SETTINGS) },
    onRequestPermission: () -> Unit = {},
) {
    val libraryState by libraryViewModel.state.collectAsState()
    val cats = listOf(
        Triple("Songs", Icons.Filled.MusicNote, Routes.SONGS) to "${libraryViewModel.allTracks.size} tracks",
        Triple("Albums", Icons.Filled.Album, Routes.ALBUMS) to "${libraryViewModel.albums.size} albums",
        Triple("Artists", Icons.Filled.Person, Routes.ARTISTS) to "${libraryViewModel.artists.size} artists",
        Triple("Playlists", Icons.AutoMirrored.Filled.QueueMusic, Routes.PLAYLISTS) to "${libraryViewModel.playlists.size} playlists",
        Triple("Folders", Icons.Filled.Folder, Routes.FOLDERS) to "${libraryViewModel.folders.size} folders",
        Triple("Genres", Icons.Filled.Audiotrack, Routes.GENRES) to "${libraryViewModel.genres.size} genres",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = Spacing.xxl),
        ) {
            item {
                Text(
                    text = "${libraryViewModel.allTracks.size} songs • ${libraryViewModel.albums.size} albums • ${libraryViewModel.artists.size} artists",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                )
                Spacer(Modifier.height(Spacing.sm))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(cats) { (cat, _) ->
                        FilterChip(
                            selected = false,
                            onClick = { onNavigate(cat.third) },
                            label = { Text(cat.first, style = MaterialTheme.typography.labelMedium) },
                            shape = RoundedCornerShape(999.dp),
                            colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = null,
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.md))
            }

            // Local Music Permission Card
            if (!libraryState.isPermissionGranted) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Audiotrack,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Scan Local Music",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Allow MIORA to access audio files on this device to discover and play your local music.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(12.dp))
                            FilledTonalButton(
                                onClick = onRequestPermission,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text("Grant Permission")
                            }
                        }
                    }
                    Spacer(Modifier.height(Spacing.sm))
                }
            }

            items(cats) { (cat, count) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(cat.third) }
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(cat.second, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(Spacing.lg))
                    Column(Modifier.weight(1f)) {
                        Text(cat.first, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                        Text(count, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit,
) {
    val state by libraryViewModel.state.collectAsState()
    val playerState by playerViewModel.state.collectAsState()
    var sortExpanded by remember { mutableStateOf(false) }

    LibraryScaffold(
        title = if (state.selectionMode) "${state.selectedIds.size} selected" else "Songs",
        onBack = { if (state.selectionMode) libraryViewModel.clearSelection() else onBack() },
        sortMenu = {
            IconButton(onClick = { sortExpanded = true }) {
                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort songs")
            }
            IconButton(onClick = {
                if (state.selectionMode) libraryViewModel.clearSelection()
                else libraryViewModel.toggleSelect(libraryViewModel.allTracks.first().id)
            }) {
                Icon(Icons.Filled.Check, contentDescription = "Select tracks")
            }
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = Spacing.xxl),
            ) {
                item {
                    Text(
                        text = "${tracks.size} songs • sorted by ${state.sortOrder.name.lowercase().replace('_', ' ')}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                    )
                    if (state.selectionMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    state.selectedIds.forEach { playerViewModel.toggleFavorite(it) }
                                    libraryViewModel.clearSelection()
                                },
                            ) {
                                Text("Add to favourites")
                            }
                            OutlinedButton(onClick = { libraryViewModel.clearSelection() }) {
                                Text("Clear selection")
                            }
                        }
                    }
                }
                items(tracks) { track ->
                    TrackRow(
                        title = track.title,
                        subtitle = "${track.artist} • ${track.album}",
                        durationMs = track.durationMs,
                        seed = track.artworkSeed,
                        albumId = track.albumId,
                        isPlaying = playerState.currentTrack?.id == track.id && playerState.isPlaying,
                        isFavorite = track.id in playerState.favorites,
                        isSelected = track.id in state.selectedIds,
                        onClick = {
                            if (state.selectionMode) libraryViewModel.toggleSelect(track.id)
                            else { playerViewModel.playTrack(track, tracks); onOpenPlayer() }
                        },
                        onLongClick = { libraryViewModel.toggleSelect(track.id) },
                        onMoreClick = { playerViewModel.playTrack(track, tracks) },
                        modifier = Modifier.padding(horizontal = Spacing.sm),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    onAlbum: (Long) -> Unit,
) {
    LibraryScaffold(title = "Albums", onBack = onBack) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            items(libraryViewModel.albums) { album ->
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onAlbum(album.id) },
                ) {
                    AlbumArt(
                        seed = album.artworkSeed,
                        title = album.title,
                        size = 160.dp,
                        cornerRadius = 16.dp,
                        albumId = album.id,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${album.artist} • ${album.year}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${album.trackIds.size} tracks",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistsScreen(
    libraryViewModel: LibraryViewModel,
    onBack: () -> Unit,
    onArtist: (Long) -> Unit,
) {
    LibraryScaffold(title = "Artists", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = Spacing.xxl),
        ) {
            item {
                Text(
                    text = "${libraryViewModel.artists.size} artists",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                )
            }
            items(libraryViewModel.artists) { artist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onArtist(artist.id) }
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AlbumArt(
                        seed = artist.artworkSeed,
                        title = artist.name,
                        size = 56.dp,
                        isCircle = true,
                        albumId = artist.albumIds.firstOrNull(),
                    )
                    Spacer(Modifier.width(Spacing.lg))
                    Column(Modifier.weight(1f)) {
                        Text(text = artist.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                        Text(
                            text = "${artist.albumIds.size} album${if (artist.albumIds.size > 1) "s" else ""} • ${artist.trackIds.size} tracks",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    libraryViewModel: LibraryViewModel,
    onBack: () -> Unit,
    onPlaylist: (Long) -> Unit,
) {
    LibraryScaffold(title = "Playlists", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = Spacing.xxl),
        ) {
            item {
                Text(
                    text = "${libraryViewModel.playlists.size} playlists",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                )
            }
            items(libraryViewModel.playlists) { playlist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlaylist(playlist.id) }
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AlbumArt(seed = playlist.artworkSeed, title = playlist.name, size = 56.dp, cornerRadius = 12.dp)
                    Spacer(Modifier.width(Spacing.lg))
                    Column(Modifier.weight(1f)) {
                        Text(text = playlist.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                        Text(
                            text = "${playlist.trackIds.size} tracks" + if (playlist.isSmart) " • Smart playlist" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit,
) {
    val playerState by playerViewModel.state.collectAsState()
    LibraryScaffold(title = "Folders", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = Spacing.xxl),
        ) {
            item {
                Text(
                    text = "Music / ${libraryViewModel.folders.size} folders",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                )
            }
            items(libraryViewModel.folders) { folder ->
                val folderName = folder.path.split("/").filter { it.isNotEmpty() }.lastOrNull() ?: folder.path
                Column(Modifier.fillMaxWidth().padding(vertical = Spacing.sm)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(Spacing.sm))
                        Text(
                            text = folderName,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${folder.trackIds.size} tracks",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    val folderTracks = libraryViewModel.tracksByIds(folder.trackIds)
                    folderTracks.forEach { track ->
                        TrackRow(
                            title = track.title,
                            subtitle = track.artist,
                            durationMs = track.durationMs,
                            seed = track.artworkSeed,
                            albumId = track.albumId,
                            isPlaying = playerState.currentTrack?.id == track.id && playerState.isPlaying,
                            isFavorite = track.id in playerState.favorites,
                            isSelected = false,
                            onClick = { playerViewModel.playTrack(track, folderTracks); onOpenPlayer() },
                            onMoreClick = { playerViewModel.playTrack(track, folderTracks) },
                            modifier = Modifier.padding(start = Spacing.md),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenresScreen(
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit,
) {
    val playerState by playerViewModel.state.collectAsState()
    LibraryScaffold(title = "Genres", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = Spacing.xxl),
        ) {
            item {
                Text(
                    text = "${libraryViewModel.genres.size} genres",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                )
            }
            items(libraryViewModel.genres) { genre ->
                Column(Modifier.fillMaxWidth().padding(vertical = Spacing.sm)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(text = genre.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = "${genre.trackIds.size} tracks",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    val genreTracks = libraryViewModel.tracksByIds(genre.trackIds)
                    genreTracks.forEach { track ->
                        TrackRow(
                            title = track.title,
                            subtitle = track.artist,
                            durationMs = track.durationMs,
                            seed = track.artworkSeed,
                            albumId = track.albumId,
                            isPlaying = playerState.currentTrack?.id == track.id && playerState.isPlaying,
                            isFavorite = track.id in playerState.favorites,
                            isSelected = false,
                            onClick = { playerViewModel.playTrack(track, genreTracks); onOpenPlayer() },
                            onMoreClick = { playerViewModel.playTrack(track, genreTracks) },
                            modifier = Modifier.padding(start = Spacing.sm),
                        )
                    }
                }
            }
        }
    }
}

/** Detail screen for Albums, Artists, and Playlists */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    title: String,
    subtitle: String,
    seed: Color,
    trackCountLabel: String,
    tracks: List<com.fahim.powerampclone.model.Track>,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit,
    albumId: Long? = null,
) {
    val playerState by playerViewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = Spacing.xxl),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AlbumArt(
                        seed = seed,
                        title = title,
                        size = 208.dp,
                        cornerRadius = 20.dp,
                        albumId = albumId,
                    )
                    Spacer(Modifier.height(Spacing.lg))
                    Text(text = title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = trackCountLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(Spacing.lg))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        Button(
                            onClick = {
                                if (tracks.isNotEmpty()) {
                                    playerViewModel.playTrack(tracks.first(), tracks)
                                    onOpenPlayer()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(Spacing.xs))
                            Text("Play")
                        }
                        FilledTonalButton(
                            onClick = {
                                if (tracks.isNotEmpty()) {
                                    val shuffled = tracks.shuffled()
                                    playerViewModel.playTrack(shuffled.first(), shuffled)
                                    onOpenPlayer()
                                }
                            },
                        ) {
                            Icon(Icons.Filled.Shuffle, contentDescription = null)
                            Spacer(Modifier.width(Spacing.xs))
                            Text("Shuffle")
                        }
                    }
                }
            }
            items(tracks.withIndex().toList()) { (index, track) ->
                TrackRow(
                    title = track.title,
                    subtitle = "${track.artist} • ${formatDuration(track.durationMs)}",
                    durationMs = track.durationMs,
                    seed = track.artworkSeed,
                    albumId = track.albumId,
                    isPlaying = playerState.currentTrack?.id == track.id && playerState.isPlaying,
                    isFavorite = track.id in playerState.favorites,
                    isSelected = false,
                    leadingNumber = index + 1,
                    onClick = { playerViewModel.playTrack(track, tracks); onOpenPlayer() },
                    onMoreClick = { playerViewModel.playTrack(track) },
                    modifier = Modifier.padding(horizontal = Spacing.sm),
                )
            }
        }
    }
}
