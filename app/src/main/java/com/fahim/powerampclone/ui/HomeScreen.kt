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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fahim.powerampclone.design.Spacing
import com.fahim.powerampclone.library.LibraryViewModel
import com.fahim.powerampclone.model.formatDuration
import com.fahim.powerampclone.navigation.Routes
import com.fahim.powerampclone.player.PlayerViewModel
import java.util.Calendar

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning!"
    in 12..16 -> "Good afternoon!"
    in 17..21 -> "Good evening!"
    else -> "Good night!"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    onNavigate: (String) -> Unit,
    onOpenPlayer: () -> Unit,
    onSearch: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Poweramp Clone", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, contentDescription = "Search") }
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = Spacing.xxl)) {
            item {
                Text(text = greeting(), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs))
                Text(text = "Your music, your way ♫", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = Spacing.lg))
                Spacer(Modifier.height(Spacing.lg))
            }
            item {
                SectionHeader("Recently Played", "See all") { onNavigate(Routes.SONGS) }
                LazyRow(contentPadding = PaddingValues(horizontal = Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    items(libraryViewModel.playlists) { playlist ->
                        Column(modifier = Modifier.width(120.dp).clip(RoundedCornerShape(16.dp)).clickable { onNavigate(Routes.playlist(playlist.id)) }) {
                            AlbumArt(seed = playlist.artworkSeed, title = playlist.name, size = 120.dp, cornerRadius = 16.dp)
                            Spacer(Modifier.height(Spacing.sm))
                            Text(text = playlist.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(text = "${playlist.trackIds.size} tracks", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.lg))
            }
            item {
                SectionHeader("Browse by Category")
                val categories: List<Triple<String, ImageVector, String>> = listOf(
                    Triple("Songs", Icons.Filled.MusicNote, Routes.SONGS),
                    Triple("Albums", Icons.Filled.Album, Routes.ALBUMS),
                    Triple("Artists", Icons.Filled.Person, Routes.ARTISTS),
                    Triple("Playlists", Icons.Filled.QueueMusic, Routes.PLAYLISTS),
                    Triple("Folders", Icons.Filled.Folder, Routes.FOLDERS),
                    Triple("Genres", Icons.Filled.Category, Routes.GENRES),
                )
                Column(Modifier.padding(horizontal = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    categories.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            row.forEach { (label, icon, route) ->
                                Card(
                                    onClick = { onNavigate(route) },
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                ) {
                                    Column(Modifier.padding(Spacing.lg), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                        Spacer(Modifier.height(Spacing.sm))
                                        Text(label, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.lg))
            }
            item { SectionHeader("All Songs", "See all") { onNavigate(Routes.SONGS) } }
            items(libraryViewModel.sortedTracks().take(8)) { track ->
                val state = playerViewModel.state
                TrackRow(
                    title = track.title,
                    subtitle = "${track.artist} • ${formatDuration(track.durationMs)}",
                    durationMs = track.durationMs,
                    seed = track.artworkSeed,
                    isPlaying = state.value.currentTrack?.id == track.id && state.value.isPlaying,
                    isFavorite = track.id in state.value.favorites,
                    isSelected = false,
                    onClick = { playerViewModel.playTrack(track); onOpenPlayer() },
                    onMoreClick = { playerViewModel.playTrack(track) },
                    modifier = Modifier.padding(horizontal = Spacing.sm),
                )
            }
        }
    }
}
