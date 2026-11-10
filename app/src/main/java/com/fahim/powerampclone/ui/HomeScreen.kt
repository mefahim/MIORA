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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.fahim.powerampclone.model.formatDuration
import com.fahim.powerampclone.navigation.Routes
import com.fahim.powerampclone.player.PlayerViewModel
import java.util.Calendar

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Good night"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    onNavigate: (String) -> Unit,
    onOpenPlayer: () -> Unit,
    onSearch: () -> Unit,
    onOpenSettings: () -> Unit = { onNavigate(Routes.SETTINGS) },
    onRequestPermission: () -> Unit = {},
) {
    val libraryState by libraryViewModel.state.collectAsState()
    val playerState by playerViewModel.state.collectAsState()
    val featuredAlbum = libraryViewModel.albums.firstOrNull()
    val recentTracks = libraryViewModel.recentlyPlayed

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "MIORA",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        ),
                    )
                },
                actions = {
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
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
            // Editorial Greeting
            item {
                Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
                    Text(
                        text = "${greeting()},\nmusic lover.",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 28.sp,
                            lineHeight = 34.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = "Your library, beautifully in tune.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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

            // Featured Album Hero Card
            if (featuredAlbum != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg)
                            .height(180.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                val tracks = libraryViewModel.tracksByIds(featuredAlbum.trackIds)
                                if (tracks.isNotEmpty()) {
                                    playerViewModel.playTrack(tracks.first(), tracks)
                                    onOpenPlayer()
                                }
                            },
                    ) {
                        // Background Vector Art
                        AlbumArt(
                            seed = featuredAlbum.artworkSeed,
                            title = featuredAlbum.title,
                            size = 180.dp,
                            cornerRadius = 20.dp,
                            albumId = featuredAlbum.id,
                            modifier = Modifier.fillMaxSize(),
                        )

                        // Dark Gradient Scrim Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.2f),
                                            Color.Black.copy(alpha = 0.65f),
                                            Color.Black.copy(alpha = 0.92f),
                                        ),
                                    ),
                                ),
                        )

                        // Hero Content
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(Spacing.lg),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "FEATURED ALBUM",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.height(Spacing.xs))
                                Text(
                                    text = featuredAlbum.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${featuredAlbum.artist} • ${featuredAlbum.year} • ${featuredAlbum.trackIds.size} tracks",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            FilledIconButton(
                                onClick = {
                                    val tracks = libraryViewModel.tracksByIds(featuredAlbum.trackIds)
                                    if (tracks.isNotEmpty()) {
                                        playerViewModel.playTrack(tracks.first(), tracks)
                                        onOpenPlayer()
                                    }
                                },
                                modifier = Modifier.size(48.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Play Album", modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(Spacing.xl))
                }
            }

            // Recently Played Shelf
            item {
                SectionHeader("Recently played", "See all") { onNavigate(Routes.SONGS) }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    items(recentTracks) { track ->
                        Column(
                            modifier = Modifier
                                .width(132.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    playerViewModel.playTrack(track, recentTracks)
                                    onOpenPlayer()
                                },
                        ) {
                            AlbumArt(
                                seed = track.artworkSeed,
                                title = track.title,
                                size = 132.dp,
                                cornerRadius = 16.dp,
                                albumId = track.albumId,
                                track = track,
                            )
                            Spacer(Modifier.height(Spacing.sm))
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.xl))
            }

            // Quick Category Strip
            item {
                SectionHeader("Browse your library")
                val categories = listOf(
                    Triple("Songs", Icons.Filled.MusicNote, Routes.SONGS),
                    Triple("Albums", Icons.Filled.Album, Routes.ALBUMS),
                    Triple("Artists", Icons.Filled.Person, Routes.ARTISTS),
                    Triple("Playlists", Icons.AutoMirrored.Filled.QueueMusic, Routes.PLAYLISTS),
                    Triple("Folders", Icons.Filled.Folder, Routes.FOLDERS),
                    Triple("Genres", Icons.Filled.Audiotrack, Routes.GENRES),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(categories) { (label, icon, route) ->
                        FilterChip(
                            selected = false,
                            onClick = { onNavigate(route) },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            leadingIcon = {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            },
                            shape = RoundedCornerShape(999.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                            ),
                            border = null,
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.xl))
            }

            // Curated Playlists Shelf
            item {
                SectionHeader("Curated playlists", "See all") { onNavigate(Routes.PLAYLISTS) }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    items(libraryViewModel.playlists) { playlist ->
                        val firstTrack = libraryViewModel.trackById(playlist.trackIds.firstOrNull() ?: 0L)
                        Column(
                            modifier = Modifier
                                .width(132.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onNavigate(Routes.playlist(playlist.id)) },
                        ) {
                            AlbumArt(
                                seed = playlist.artworkSeed,
                                title = playlist.name,
                                size = 132.dp,
                                cornerRadius = 16.dp,
                                track = firstTrack,
                                albumId = firstTrack?.albumId,
                            )
                            Spacer(Modifier.height(Spacing.sm))
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${playlist.trackIds.size} tracks${if (playlist.isSmart) " • Smart" else ""}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.xl))
            }

            // Continue Listening Track List
            val continueList = libraryViewModel.sortedTracks().take(6)
            item {
                SectionHeader("Continue listening", "See all") { onNavigate(Routes.SONGS) }
            }
            items(continueList) { track ->
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
                        playerViewModel.playTrack(track, continueList)
                        onOpenPlayer()
                    },
                    onMoreClick = {
                        playerViewModel.playTrack(track, continueList)
                    },
                    modifier = Modifier.padding(horizontal = Spacing.sm),
                )
            }
        }
    }
}
