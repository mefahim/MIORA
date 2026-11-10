package com.fahim.powerampclone.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahim.powerampclone.design.LocalMioraPalette
import com.fahim.powerampclone.design.Spacing
import com.fahim.powerampclone.design.TouchTargets
import com.fahim.powerampclone.model.RepeatMode
import com.fahim.powerampclone.model.formatDuration
import com.fahim.powerampclone.navigation.Routes
import com.fahim.powerampclone.player.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val state by playerViewModel.state.collectAsState()
    val track = state.currentTrack
    val palette = LocalMioraPalette.current

    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            palette.playerBg1,
            palette.playerBg2,
            MaterialTheme.colorScheme.background,
        ),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Minimise player", modifier = Modifier.size(28.dp))
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.Cast, contentDescription = "Cast (not available in Phase 2)")
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush)
                .padding(padding),
        ) {
            if (track == null) {
                EmptyState("Nothing playing", "Pick a song from your library to start simulated playback.", Modifier.fillMaxSize())
                return@Box
            }

            val currentDuration = state.currentDurationMs
            val progress = if (currentDuration > 0L) (state.positionMs.toFloat() / currentDuration).coerceIn(0f, 1f) else 0f
            val favoriteScale by animateFloatAsState(
                if (track.id in state.favorites) 1.2f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "favScale",
            )

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val landscape = maxWidth > maxHeight
                if (landscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AlbumArt(
                            seed = track.artworkSeed,
                            title = track.title,
                            size = 280.dp,
                            cornerRadius = 24.dp,
                            albumId = track.albumId,
                        )
                        Spacer(Modifier.width(Spacing.xxl))
                        PlayerControlsColumn(
                            track = track,
                            state = state,
                            playerViewModel = playerViewModel,
                            onNavigate = onNavigate,
                            favoriteScale = favoriteScale,
                            progress = progress,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AlbumArt(
                            seed = track.artworkSeed,
                            title = track.title,
                            size = 300.dp,
                            cornerRadius = 24.dp,
                            albumId = track.albumId,
                        )
                        Spacer(Modifier.height(Spacing.lg))
                        PlayerControlsColumn(
                            track = track,
                            state = state,
                            playerViewModel = playerViewModel,
                            onNavigate = onNavigate,
                            favoriteScale = favoriteScale,
                            progress = progress,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerControlsColumn(
    track: com.fahim.powerampclone.model.Track,
    state: com.fahim.powerampclone.player.PlayerUiState,
    playerViewModel: PlayerViewModel,
    onNavigate: (String) -> Unit,
    favoriteScale: Float,
    progress: Float,
    modifier: Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // Track Title & Favorite Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${track.artist} • ${track.album}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = { playerViewModel.toggleFavorite(track.id) },
                modifier = Modifier
                    .size(TouchTargets.min)
                    .scale(favoriteScale),
            ) {
                Icon(
                    imageVector = if (track.id in state.favorites) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (track.id in state.favorites) "Remove from favourites" else "Add to favourites",
                    tint = if (track.id in state.favorites) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp),
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))

        val currentDuration = state.currentDurationMs
        var isUserSeeking by remember { mutableStateOf(false) }
        var userSeekProgress by remember { mutableFloatStateOf(0f) }

        val activeProgress = if (isUserSeeking) {
            userSeekProgress
        } else {
            if (currentDuration > 0L) (state.positionMs.toFloat() / currentDuration).coerceIn(0f, 1f) else 0f
        }

        // Progress Slider
        Slider(
            value = activeProgress,
            onValueChange = {
                isUserSeeking = true
                userSeekProgress = it
            },
            onValueChangeFinished = {
                val targetMs = (userSeekProgress * currentDuration).toLong()
                playerViewModel.seekTo(targetMs)
                isUserSeeking = false
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        // Elapsed and Remaining Time
        val currentDisplayPos = if (isUserSeeking) (userSeekProgress * currentDuration).toLong() else state.positionMs
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = formatDuration(currentDisplayPos),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "-${formatDuration((currentDuration - currentDisplayPos).coerceAtLeast(0))}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(Spacing.md))

        // Transport Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { playerViewModel.toggleShuffle() },
                modifier = Modifier.size(TouchTargets.min),
            ) {
                Icon(
                    Icons.Filled.Shuffle,
                    contentDescription = if (state.shuffle) "Shuffle on" else "Shuffle off",
                    tint = if (state.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }

            IconButton(
                onClick = { playerViewModel.previous() },
                modifier = Modifier.size(56.dp),
            ) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous track", modifier = Modifier.size(36.dp))
            }

            FilledIconButton(
                onClick = { playerViewModel.togglePlayPause() },
                modifier = Modifier.size(72.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(36.dp),
                )
            }

            IconButton(
                onClick = { playerViewModel.next() },
                modifier = Modifier.size(56.dp),
            ) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next track", modifier = Modifier.size(36.dp))
            }

            IconButton(
                onClick = { playerViewModel.cycleRepeat() },
                modifier = Modifier.size(TouchTargets.min),
            ) {
                val (icon, label, active) = when (state.repeat) {
                    RepeatMode.OFF -> Triple(Icons.Filled.Repeat, "Repeat off", false)
                    RepeatMode.ALL -> Triple(Icons.Filled.Repeat, "Repeat all", true)
                    RepeatMode.ONE -> Triple(Icons.Filled.RepeatOne, "Repeat one", true)
                }
                Icon(
                    icon,
                    contentDescription = label,
                    tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))

        // Ambient Waveform Strip
        WaveformStrip(seed = track.artworkSeed, progress = progress)

        Spacer(Modifier.height(Spacing.lg))

        // Action Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            OutlinedButton(
                onClick = { onNavigate(Routes.LYRICS) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                shape = RoundedCornerShape(999.dp),
            ) {
                Icon(Icons.Filled.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(Spacing.xs))
                Text("Lyrics", style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(
                onClick = { onNavigate(Routes.AUDIO_INFO) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                shape = RoundedCornerShape(999.dp),
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(Spacing.xs))
                Text("Audio Info", style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(
                onClick = { onNavigate(Routes.QUEUE) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                shape = RoundedCornerShape(999.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(Spacing.xs))
                Text("Queue", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(Modifier.height(Spacing.md))

        Text(
            text = "Simulated playback — no audio engine in Phase 2.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Reusable mini-player matching web-preview/js/library.js renderMiniPlayer() */
@Composable
fun MiniPlayer(
    playerViewModel: PlayerViewModel,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by playerViewModel.state.collectAsState()
    val track = state.currentTrack ?: return
    val currentDuration = state.currentDurationMs
    val progress = if (currentDuration > 0L) (state.positionMs.toFloat() / currentDuration).coerceIn(0f, 1f) else 0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp,
    ) {
        Column(Modifier.fillMaxSize()) {
            // Linear Progress Track along top edge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                        .height(2.5.dp)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }

            // Controls & Metadata Row
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onExpand),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AlbumArt(
                        seed = track.artworkSeed,
                        title = track.title,
                        size = 48.dp,
                        cornerRadius = 8.dp,
                        albumId = track.albumId,
                    )
                    Spacer(Modifier.width(Spacing.md))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
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

                IconButton(
                    onClick = { playerViewModel.togglePlayPause() },
                    modifier = Modifier.size(TouchTargets.min),
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp),
                    )
                }

                IconButton(
                    onClick = { playerViewModel.next() },
                    modifier = Modifier.size(TouchTargets.min),
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next track",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}
