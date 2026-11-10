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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.unit.dp
import com.fahim.powerampclone.design.Spacing
import com.fahim.powerampclone.design.TouchTargets
import com.fahim.powerampclone.library.SearchViewModel
import com.fahim.powerampclone.model.formatDuration
import com.fahim.powerampclone.player.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(playerViewModel: PlayerViewModel, onBack: () -> Unit, onOpenPlayer: () -> Unit) {
    val state by playerViewModel.state.collectAsState()
    var showClearConfirm by remember { mutableStateOf(false) }
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear queue?") },
            text = { Text("This removes all upcoming tracks. The current track keeps playing.") },
            confirmButton = { TextButton(onClick = { playerViewModel.clearQueue(); showClearConfirm = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") } },
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Queue", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        val current = state.currentTrack
        if (current == null) {
            EmptyState("Queue is empty", "Play a song and it will appear here.", Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = Spacing.xxl)) {
            item {
                Text(text = "${state.queue.size} tracks", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = Spacing.lg))
                TrackRow(
                    title = current.title, subtitle = "${current.artist} • Now playing",
                    durationMs = current.durationMs, seed = current.artworkSeed,
                    isPlaying = state.isPlaying, isFavorite = current.id in state.favorites, isSelected = false,
                    onClick = onOpenPlayer,
                    modifier = Modifier.padding(horizontal = Spacing.sm),
                )
                SectionHeader("Up Next")
            }
            if (state.upNext.isEmpty()) {
                item { EmptyState("Nothing up next", "The queue ends after the current track.", Modifier.padding(horizontal = Spacing.lg)) }
            } else {
                itemsIndexed(state.upNext) { offset, track ->
                    val queueIndex = state.currentIndex + 1 + offset
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            TrackRow(
                                title = track.title, subtitle = track.artist,
                                durationMs = track.durationMs, seed = track.artworkSeed,
                                isPlaying = false, isFavorite = track.id in state.favorites, isSelected = false,
                                leadingNumber = offset + 1,
                                onClick = { playerViewModel.playTrack(track); onOpenPlayer() },
                            )
                        }
                        IconButton(onClick = { playerViewModel.moveQueueItem(queueIndex, queueIndex - 1) }, modifier = Modifier.size(TouchTargets.min), enabled = queueIndex > state.currentIndex + 1) {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move ${track.title} up in queue")
                        }
                        IconButton(onClick = { playerViewModel.moveQueueItem(queueIndex, queueIndex + 1) }, modifier = Modifier.size(TouchTargets.min), enabled = queueIndex < state.queue.size - 1) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move ${track.title} down in queue")
                        }
                        IconButton(onClick = { playerViewModel.removeQueueItem(queueIndex) }, modifier = Modifier.size(TouchTargets.min)) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove ${track.title} from queue")
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    OutlinedButton(onClick = { showClearConfirm = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(Modifier.width(Spacing.sm))
                        Text("Clear Queue")
                    }
                    FilledTonalButton(onClick = { playerViewModel.shuffleQueue() }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Shuffle, contentDescription = null)
                        Spacer(Modifier.width(Spacing.sm))
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
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TextField(
                value = state.query,
                onValueChange = { searchViewModel.onQueryChange(it) },
                placeholder = { Text("Songs, albums, artists…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) IconButton(onClick = { searchViewModel.clearSearch() }) { Icon(Icons.Filled.Clear, contentDescription = "Clear search") }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            )
            val results = state.results
            when {
                results == null -> {
                    if (state.history.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                            Text("Recent searches", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            TextButton(onClick = { searchViewModel.clearHistory() }) { Text("Clear") }
                        }
                        Column(Modifier.padding(horizontal = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            state.history.forEach { term ->
                                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { searchViewModel.useHistoryTerm(term) }.padding(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(Spacing.md))
                                    Text(term, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    } else {
                        EmptyState("Search your library", "Try a song, album, artist or playlist name.", Modifier.padding(Spacing.lg))
                    }
                }
                results.tracks.isEmpty() && results.albums.isEmpty() && results.artists.isEmpty() && results.playlists.isEmpty() -> {
                    EmptyState("No results", "Nothing in the sample library matches \"${state.query}\".", Modifier.padding(Spacing.lg))
                }
                else -> {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Spacing.xxl)) {
                        if (results.tracks.isNotEmpty()) {
                            item { SectionHeader("Songs") }
                            itemsIndexed(results.tracks) { _, track ->
                                TrackRow(
                                    title = track.title, subtitle = "${track.artist} • ${track.album}",
                                    durationMs = track.durationMs, seed = track.artworkSeed,
                                    isPlaying = playerState.currentTrack?.id == track.id && playerState.isPlaying,
                                    isFavorite = track.id in playerState.favorites, isSelected = false,
                                    onClick = { searchViewModel.submitSearch(); playerViewModel.playTrack(track); onOpenPlayer() },
                                    modifier = Modifier.padding(horizontal = Spacing.sm),
                                )
                            }
                        }
                        if (results.albums.isNotEmpty()) {
                            item { SectionHeader("Albums") }
                            itemsIndexed(results.albums) { _, album ->
                                Row(modifier = Modifier.fillMaxWidth().clickable { onAlbum(album.id) }.padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                                    AlbumArt(seed = album.artworkSeed, title = album.title, size = 48.dp, cornerRadius = 8.dp)
                                    Spacer(Modifier.width(Spacing.md))
                                    Column { Text(album.title, style = MaterialTheme.typography.bodyLarge); Text(album.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                }
                            }
                        }
                        if (results.artists.isNotEmpty()) {
                            item { SectionHeader("Artists") }
                            itemsIndexed(results.artists) { _, artist ->
                                Row(modifier = Modifier.fillMaxWidth().clickable { onArtist(artist.id) }.padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                                    AlbumArt(seed = artist.artworkSeed, title = artist.name, size = 48.dp, cornerRadius = 24.dp)
                                    Spacer(Modifier.width(Spacing.md))
                                    Text(artist.name, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                        if (results.playlists.isNotEmpty()) {
                            item { SectionHeader("Playlists") }
                            itemsIndexed(results.playlists) { _, playlist ->
                                Row(modifier = Modifier.fillMaxWidth().clickable { onPlaylist(playlist.id) }.padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                                    AlbumArt(seed = playlist.artworkSeed, title = playlist.name, size = 48.dp, cornerRadius = 8.dp)
                                    Spacer(Modifier.width(Spacing.md))
                                    Text(playlist.name, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
