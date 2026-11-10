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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahim.powerampclone.design.Spacing
import com.fahim.powerampclone.library.LibraryViewModel
import com.fahim.powerampclone.model.Playlist
import com.fahim.powerampclone.model.Track
import com.fahim.powerampclone.model.formatDuration
import com.fahim.powerampclone.player.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel? = null,
    onBack: () -> Unit,
    onPlaylist: (Long) -> Unit,
    onOpenPlayer: () -> Unit = {},
) {
    val state by libraryViewModel.state.collectAsState()
    val playlists = libraryViewModel.playlists

    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playlists", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Create Playlist")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (playlists.isEmpty()) {
            EmptyState(
                title = "No playlists yet",
                message = "Create custom playlists to organize your favorite music for every mood.",
                modifier = Modifier.padding(padding),
                actionLabel = "Create Playlist",
                onAction = { showCreateDialog = true },
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = Spacing.xxl),
            ) {
                item {
                    Text(
                        text = "${playlists.size} playlists",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                    )
                }

                items(playlists, key = { it.id }) { playlist ->
                    var menuExpanded by remember { mutableStateOf(false) }
                    val firstTrack = libraryViewModel.trackById(playlist.trackIds.firstOrNull() ?: 0L)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlaylist(playlist.id) }
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AlbumArt(
                            seed = playlist.artworkSeed,
                            title = playlist.name,
                            size = 56.dp,
                            cornerRadius = 12.dp,
                            track = firstTrack,
                            albumId = firstTrack?.albumId,
                        )
                        Spacer(Modifier.width(Spacing.lg))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${playlist.trackIds.size} tracks" + if (playlist.isSmart) " • Smart playlist" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "Options")
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Play") },
                                    leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        val tracks = libraryViewModel.tracksForPlaylist(playlist)
                                        if (tracks.isNotEmpty() && playerViewModel != null) {
                                            playerViewModel.playTrack(tracks.first(), tracks)
                                            onOpenPlayer()
                                        }
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Shuffle") },
                                    leadingIcon = { Icon(Icons.Filled.Shuffle, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        val tracks = libraryViewModel.tracksForPlaylist(playlist)
                                        if (tracks.isNotEmpty() && playerViewModel != null) {
                                            val shuffled = tracks.shuffled()
                                            playerViewModel.playTrack(shuffled.first(), shuffled)
                                            onOpenPlayer()
                                        }
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Add to Queue") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        val tracks = libraryViewModel.tracksForPlaylist(playlist)
                                        tracks.forEach { playerViewModel?.addToQueue(it) }
                                    },
                                )
                                if (!playlist.isSmart) {
                                    DropdownMenuItem(
                                        text = { Text("Rename") },
                                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            playlistToRename = playlist
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            menuExpanded = false
                                            playlistToDelete = playlist
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCreateDialog) {
        CreatePlaylistDialog(
            existingNames = playlists.map { it.name },
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                libraryViewModel.createPlaylist(name)
                showCreateDialog = false
            },
        )
    }

    playlistToRename?.let { target ->
        RenamePlaylistDialog(
            currentName = target.name,
            existingNames = playlists.filter { it.id != target.id }.map { it.name },
            onDismiss = { playlistToRename = null },
            onRename = { newName ->
                libraryViewModel.renamePlaylist(target.id, newName)
                playlistToRename = null
            },
        )
    }

    playlistToDelete?.let { target ->
        DeletePlaylistDialog(
            playlistName = target.name,
            onDismiss = { playlistToDelete = null },
            onConfirm = {
                libraryViewModel.deletePlaylist(target.id)
                playlistToDelete = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit,
) {
    val state by libraryViewModel.state.collectAsState()
    val playerState by playerViewModel.state.collectAsState()
    val playlist = libraryViewModel.playlistById(playlistId)

    var showAddTracksDialog by remember { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var trackForAddToOtherPlaylist by remember { mutableStateOf<Track?>(null) }

    if (playlist == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Playlist", style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
            },
        ) { padding ->
            EmptyState("Playlist not found", "This playlist may have been deleted.", Modifier.padding(padding))
        }
        return
    }

    val tracks = libraryViewModel.tracksForPlaylist(playlist)
    val totalDurationMs = libraryViewModel.durationForPlaylist(playlist)
    val firstTrack = tracks.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlist.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!playlist.isSmart) {
                        IconButton(onClick = { showAddTracksDialog = true }) {
                            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add Tracks")
                        }
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
                        seed = playlist.artworkSeed,
                        title = playlist.name,
                        size = 208.dp,
                        cornerRadius = 20.dp,
                        track = firstTrack,
                        albumId = firstTrack?.albumId,
                    )
                    Spacer(Modifier.height(Spacing.lg))
                    Text(text = playlist.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (playlist.isSmart) "Smart playlist" else "Playlist",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${tracks.size} tracks • ${formatDuration(totalDurationMs)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.lg))

                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        Button(
                            onClick = {
                                if (tracks.isNotEmpty()) {
                                    playerViewModel.playTrack(tracks.first(), tracks)
                                    onOpenPlayer()
                                }
                            },
                            enabled = tracks.isNotEmpty(),
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
                            enabled = tracks.isNotEmpty(),
                        ) {
                            Icon(Icons.Filled.Shuffle, contentDescription = null)
                            Spacer(Modifier.width(Spacing.xs))
                            Text("Shuffle")
                        }

                        if (!playlist.isSmart) {
                            OutlinedButton(onClick = { showAddTracksDialog = true }) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(Modifier.width(Spacing.xs))
                                Text("Add")
                            }
                        }
                    }
                }
            }

            if (tracks.isEmpty()) {
                item {
                    EmptyState(
                        title = "No songs in this playlist",
                        message = if (playlist.isSmart) "Songs will appear here automatically based on playback." else "Add songs from your music library.",
                        modifier = Modifier.fillMaxWidth(),
                        actionLabel = if (!playlist.isSmart) "Add Tracks" else null,
                        onAction = if (!playlist.isSmart) { { showAddTracksDialog = true } } else null,
                    )
                }
            } else {
                itemsIndexed(tracks, key = { index, track -> "${track.id}_$index" }) { index, track ->
                    var trackMenuExpanded by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.weight(1f)) {
                            TrackRow(
                                title = track.title,
                                subtitle = "${track.artist} • ${formatDuration(track.durationMs)}",
                                durationMs = track.durationMs,
                                seed = track.artworkSeed,
                                albumId = track.albumId,
                                track = track,
                                isPlaying = playerState.currentTrack?.id == track.id && playerState.isPlaying,
                                isFavorite = track.id in playerState.favorites,
                                isSelected = false,
                                leadingNumber = index + 1,
                                onClick = {
                                    playerViewModel.playTrack(track, tracks)
                                    onOpenPlayer()
                                },
                                onMoreClick = { trackMenuExpanded = true },
                            )
                        }

                        // Reorder buttons for user playlists
                        if (!playlist.isSmart) {
                            if (index > 0) {
                                IconButton(
                                    onClick = { libraryViewModel.reorderPlaylistTracks(playlist.id, index, index - 1) },
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(Icons.Filled.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(18.dp))
                                }
                            }
                            if (index < tracks.size - 1) {
                                IconButton(
                                    onClick = { libraryViewModel.reorderPlaylistTracks(playlist.id, index, index + 1) },
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(Icons.Filled.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Box {
                            IconButton(onClick = { trackMenuExpanded = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More")
                            }

                            DropdownMenu(
                                expanded = trackMenuExpanded,
                                onDismissRequest = { trackMenuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Play Next") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                                    onClick = {
                                        trackMenuExpanded = false
                                        playerViewModel.playNext(track)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Add to Queue") },
                                    leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                                    onClick = {
                                        trackMenuExpanded = false
                                        playerViewModel.addToQueue(track)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Add to another playlist...") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                                    onClick = {
                                        trackMenuExpanded = false
                                        trackForAddToOtherPlaylist = track
                                    },
                                )
                                if (!playlist.isSmart) {
                                    DropdownMenuItem(
                                        text = { Text("Remove from playlist", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            trackMenuExpanded = false
                                            libraryViewModel.removeTrackFromPlaylist(playlist.id, track.id, index)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Tracks Dialog
    if (showAddTracksDialog) {
        AddTracksDialog(
            allTracks = libraryViewModel.allTracks,
            existingTrackIds = playlist.trackIds.toSet(),
            onDismiss = { showAddTracksDialog = false },
            onAddTracks = { selectedIds ->
                libraryViewModel.addTracksToPlaylist(playlist.id, selectedIds)
                showAddTracksDialog = false
            },
        )
    }

    trackForAddToOtherPlaylist?.let { t ->
        AddToPlaylistDialog(
            trackIds = listOf(t.id),
            libraryViewModel = libraryViewModel,
            onDismiss = { trackForAddToOtherPlaylist = null },
        )
    }
}

@Composable
fun CreatePlaylistDialog(
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Playlist") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    isError = errorMessage != null,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        errorMessage = "Playlist name cannot be empty."
                    } else if (existingNames.any { it.equals(trimmed, ignoreCase = true) }) {
                        errorMessage = "A playlist with this name already exists."
                    } else {
                        onCreate(trimmed)
                    }
                },
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun RenamePlaylistDialog(
    currentName: String,
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Playlist") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("New Name") },
                    singleLine = true,
                    isError = errorMessage != null,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        errorMessage = "Name cannot be empty."
                    } else if (existingNames.any { it.equals(trimmed, ignoreCase = true) }) {
                        errorMessage = "A playlist with this name already exists."
                    } else {
                        onRename(trimmed)
                    }
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun DeletePlaylistDialog(
    playlistName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Playlist") },
        text = { Text("Are you sure you want to delete '$playlistName'? The audio files will remain on your device.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun AddTracksDialog(
    allTracks: List<Track>,
    existingTrackIds: Set<Long>,
    onDismiss: () -> Unit,
    onAddTracks: (List<Long>) -> Unit,
) {
    val selectedIds = remember { mutableStateListOf<Long>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Songs to Playlist") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
            ) {
                items(allTracks) { track ->
                    val isAlreadyIn = track.id in existingTrackIds
                    val isChecked = track.id in selectedIds

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isAlreadyIn) {
                                if (isChecked) selectedIds.remove(track.id) else selectedIds.add(track.id)
                            }
                            .padding(vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = isAlreadyIn || isChecked,
                            onCheckedChange = { checked ->
                                if (!isAlreadyIn) {
                                    if (checked) selectedIds.add(track.id) else selectedIds.remove(track.id)
                                }
                            },
                            enabled = !isAlreadyIn,
                        )
                        Spacer(Modifier.width(Spacing.xs))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isAlreadyIn) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${track.artist}${if (isAlreadyIn) " • Already added" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAddTracks(selectedIds.toList()) },
                enabled = selectedIds.isNotEmpty(),
            ) {
                Text("Add (${selectedIds.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun AddToPlaylistDialog(
    trackIds: List<Long>,
    libraryViewModel: LibraryViewModel,
    onDismiss: () -> Unit,
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    val playlists = libraryViewModel.playlists.filter { !it.isSmart }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Playlist") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCreateDialog = true }
                        .padding(vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(Spacing.md))
                    Text(
                        text = "New Playlist...",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary),
                    )
                }

                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(playlists) { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    libraryViewModel.addTracksToPlaylist(playlist.id, trackIds)
                                    onDismiss()
                                }
                                .padding(vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AlbumArt(
                                seed = playlist.artworkSeed,
                                title = playlist.name,
                                size = 44.dp,
                                cornerRadius = 8.dp,
                            )
                            Spacer(Modifier.width(Spacing.md))
                            Column(Modifier.weight(1f)) {
                                Text(playlist.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                                Text("${playlist.trackIds.size} tracks", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )

    if (showCreateDialog) {
        CreatePlaylistDialog(
            existingNames = libraryViewModel.playlists.map { it.name },
            onDismiss = { showCreateDialog = false },
            onCreate = { newName ->
                val created = libraryViewModel.createPlaylist(newName)
                if (created != null) {
                    libraryViewModel.addTracksToPlaylist(created.id, trackIds)
                }
                showCreateDialog = false
                onDismiss()
            },
        )
    }
}
