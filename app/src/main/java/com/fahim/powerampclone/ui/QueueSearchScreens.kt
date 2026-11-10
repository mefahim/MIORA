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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fahim.powerampclone.design.Spacing
import com.fahim.powerampclone.design.TouchTargets
import com.fahim.powerampclone.library.SearchViewModel
import com.fahim.powerampclone.player.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit,
) {
    val state by playerViewModel.state.collectAsState()
    var showClearConfirm by remember { mutableStateOf(false) }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear queue?") },
            text = { Text("This removes all upcoming tracks. The current track keeps playing.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        playerViewModel.clearQueue()
                        showClearConfirm = false
                    },
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Queue", style = MaterialTheme.typography.titleLarge) },
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
        val current = state.currentTrack
        if (current == null) {
            EmptyState("Queue is empty", "Play a song and it will appear here.", Modifier.padding(padding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = Spacing.xxl),
        ) {
            item {
                Text(
                    text = "${state.queue.size} tracks in queue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "NOW PLAYING",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                )
                TrackRow(
                    title = current.title,
                    subtitle = "${current.artist} • ${current.album}",
                    durationMs = current.durationMs,
                    seed = current.artworkSeed,
                    albumId = current.albumId,
                    isPlaying = state.isPlaying,
                    isFavorite = current.id in state.favorites,
                    isSelected = false,
                    onClick = onOpenPlayer,
                    modifier = Modifier.padding(horizontal = Spacing.sm),
                )
                Spacer(Modifier.height(Spacing.md))
                SectionHeader("Up next", null)
            }

            if (state.upNext.isEmpty()) {
                item {
                    EmptyState(
                        title = "Nothing up next",
                        message = "The queue ends after the current track. Add songs from any list to keep the music going.",
                        modifier = Modifier.padding(horizontal = Spacing.lg),
                    )
                }
            } else {
                itemsIndexed(state.upNext) { offset, track ->
                    val queueIndex = state.currentIndex + 1 + offset
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.sm, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TrackRow(
                            title = track.title,
                            subtitle = track.artist,
                            durationMs = track.durationMs,
                            seed = track.artworkSeed,
                            albumId = track.albumId,
                            isPlaying = false,
                            isFavorite = track.id in state.favorites,
                            isSelected = false,
                            leadingNumber = offset + 1,
                            onClick = { playerViewModel.playTrack(track) },
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { playerViewModel.moveQueueItem(queueIndex, queueIndex - 1) },
                            enabled = queueIndex > state.currentIndex + 1,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up")
                        }
                        IconButton(
                            onClick = { playerViewModel.moveQueueItem(queueIndex, queueIndex + 1) },
                            enabled = queueIndex < state.queue.size - 1,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down")
                        }
                        IconButton(
                            onClick = { playerViewModel.removeQueueItem(queueIndex) },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove")
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(Spacing.lg))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    OutlinedButton(
                        onClick = { showClearConfirm = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(Spacing.xs))
                        Text("Clear queue")
                    }
                    FilledTonalButton(
                        onClick = { playerViewModel.shuffleQueue() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(Spacing.xs))
                        Text("Shuffle")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    onAlbum: (Long) -> Unit,
    onArtist: (Long) -> Unit,
    onPlaylist: (Long) -> Unit,
    onOpenPlayer: () -> Unit,
) {
    val state by searchViewModel.state.collectAsState()
    val playerState by playerViewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search", style = MaterialTheme.typography.titleLarge) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { searchViewModel.onQueryChange(it) },
                placeholder = { Text("Songs, albums, artists…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { searchViewModel.onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            )

            val results = state.results
            if (results == null) {
                if (state.history.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Recent searches", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { searchViewModel.clearHistory() }) { Text("Clear") }
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = Spacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        items(state.history) { term ->
                            AssistChip(
                                onClick = { searchViewModel.onQueryChange(term) },
                                label = { Text(term) },
                                leadingIcon = { Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                shape = RoundedCornerShape(999.dp),
                            )
                        }
                    }
                } else {
                    EmptyState(
                        title = "Search your library",
                        message = "Try a song, album, artist or playlist name — results appear as you type.",
                    )
                }
            } else if (results.tracks.isEmpty() && results.albums.isEmpty() && results.artists.isEmpty() && results.playlists.isEmpty()) {
                EmptyState(
                    title = "No results for “${state.query}”",
                    message = "Nothing in the sample library matches. Check the spelling or try an artist name like Aurora Lane.",
                    actionLabel = "Clear search",
                    onAction = { searchViewModel.onQueryChange("") },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = Spacing.xxl),
                ) {
                    if (results.tracks.isNotEmpty()) {
                        item { SectionHeader("Songs", "${results.tracks.size}") }
                        items(results.tracks) { track ->
                            TrackRow(
                                title = track.title,
                                subtitle = "${track.artist} • ${track.album}",
                                durationMs = track.durationMs,
                                seed = track.artworkSeed,
                                albumId = track.albumId,
                                isPlaying = playerState.currentTrack?.id == track.id && playerState.isPlaying,
                                isFavorite = track.id in playerState.favorites,
                                isSelected = false,
                                onClick = {
                                    searchViewModel.submitSearch()
                                    playerViewModel.playTrack(track)
                                    onOpenPlayer()
                                },
                                modifier = Modifier.padding(horizontal = Spacing.sm),
                            )
                        }
                    }

                    if (results.albums.isNotEmpty()) {
                        item { SectionHeader("Albums", null) }
                        items(results.albums) { album ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        searchViewModel.submitSearch()
                                        onAlbum(album.id)
                                    }
                                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AlbumArt(
                                    seed = album.artworkSeed,
                                    title = album.title,
                                    size = 48.dp,
                                    cornerRadius = 8.dp,
                                    albumId = album.id,
                                )
                                Spacer(Modifier.width(Spacing.md))
                                Column(Modifier.weight(1f)) {
                                    Text(album.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                                    Text(album.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    if (results.artists.isNotEmpty()) {
                        item { SectionHeader("Artists", null) }
                        items(results.artists) { artist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        searchViewModel.submitSearch()
                                        onArtist(artist.id)
                                    }
                                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AlbumArt(
                                    seed = artist.artworkSeed,
                                    title = artist.name,
                                    size = 48.dp,
                                    isCircle = true,
                                    albumId = artist.albumIds.firstOrNull(),
                                )
                                Spacer(Modifier.width(Spacing.md))
                                Text(artist.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
                                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    if (results.playlists.isNotEmpty()) {
                        item { SectionHeader("Playlists", null) }
                        items(results.playlists) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        searchViewModel.submitSearch()
                                        onPlaylist(playlist.id)
                                    }
                                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AlbumArt(
                                    seed = playlist.artworkSeed,
                                    title = playlist.name,
                                    size = 48.dp,
                                    cornerRadius = 8.dp,
                                )
                                Spacer(Modifier.width(Spacing.md))
                                Text(playlist.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
                                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
