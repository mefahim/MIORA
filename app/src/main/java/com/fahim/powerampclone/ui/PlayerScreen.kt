package com.fahim.powerampclone.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Minimise player") } },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Filled.Cast, contentDescription = "Cast (not available in Phase 2)") }
                    IconButton(onClick = {}) { Icon(Icons.Filled.MoreVert, contentDescription = "More options") }
                },
            )
        },
    ) { padding ->
        if (track == null) {
            EmptyState("Nothing playing", "Pick a song from your library to start simulated playback.", Modifier.padding(padding))
            return@Scaffold
        }
        val progress = if (track.durationMs > 0) state.positionMs.toFloat() / track.durationMs else 0f
        val favoriteScale by animateFloatAsState(if (track.id in state.favorites) 1.15f else 1f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow), label = "favScale")
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
            val landscape = maxWidth > maxHeight
            if (landscape) {
                Row(Modifier.fillMaxSize().padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                    AlbumArt(seed = track.artworkSeed, title = track.title, size = 280.dp, cornerRadius = 24.dp)
                    Spacer(Modifier.width(Spacing.xxl))
                    PlayerControlsColumn(track, state, playerViewModel, onNavigate, favoriteScale, progress, Modifier.weight(1f))
                }
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg), horizontalAlignment = Alignment.CenterHorizontally) {
                    AlbumArt(seed = track.artworkSeed, title = track.title, size = 320.dp, cornerRadius = 24.dp)
                    Spacer(Modifier.height(Spacing.xl))
                    PlayerControlsColumn(track, state, playerViewModel, onNavigate, favoriteScale, progress, Modifier.fillMaxWidth())
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
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(text = track.title, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "${track.artist} • ${track.album}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(
                onClick = { playerViewModel.toggleFavorite(track.id) },
                modifier = Modifier.size(TouchTargets.min).scale(favoriteScale),
            ) {
                Icon(
                    imageVector = if (track.id in state.favorites) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (track.id in state.favorites) "Remove from favourites" else "Add to favourites",
                    tint = if (track.id in state.favorites) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        Slider(
            value = progress,
            onValueChange = { playerViewModel.seekTo((it * track.durationMs).toLong()) },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = formatDuration(state.positionMs), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "-${formatDuration(track.durationMs - state.positionMs)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(Spacing.md))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { playerViewModel.toggleShuffle() }, modifier = Modifier.size(TouchTargets.min)) {
                Icon(Icons.Filled.Shuffle, contentDescription = if (state.shuffle) "Shuffle on" else "Shuffle off", tint = if (state.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { playerViewModel.previous() }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous track", modifier = Modifier.size(36.dp))
            }
            FilledIconButton(onClick = { playerViewModel.togglePlayPause() }, modifier = Modifier.size(72.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = if (state.isPlaying) "Pause (simulated playback)" else "Play (simulated playback)", modifier = Modifier.size(36.dp))
            }
            IconButton(onClick = { playerViewModel.next() }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next track", modifier = Modifier.size(36.dp))
            }
            IconButton(onClick = { playerViewModel.cycleRepeat() }, modifier = Modifier.size(TouchTargets.min)) {
                val (icon, label, active) = when (state.repeat) {
                    RepeatMode.OFF -> Triple(Icons.Filled.Repeat, "Repeat off", false)
                    RepeatMode.ALL -> Triple(Icons.Filled.Repeat, "Repeat all", true)
                    RepeatMode.ONE -> Triple(Icons.Filled.RepeatOne, "Repeat one", true)
                }
                Icon(icon, contentDescription = label, tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        WaveformStrip(seed = track.artworkSeed, progress = progress)
        Spacer(Modifier.height(Spacing.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            OutlinedButton(onClick = { onNavigate(Routes.LYRICS) }) {
                Icon(Icons.Filled.MusicNote, contentDescription = null)
                Spacer(Modifier.width(Spacing.sm))
                Text("Lyrics")
            }
            OutlinedButton(onClick = { onNavigate(Routes.AUDIO_INFO) }) {
                Icon(Icons.Filled.Info, contentDescription = null)
                Spacer(Modifier.width(Spacing.sm))
                Text("Audio Info")
            }
            OutlinedButton(onClick = { onNavigate(Routes.QUEUE) }) {
                Icon(Icons.Filled.QueueMusic, contentDescription = null)
                Spacer(Modifier.width(Spacing.sm))
                Text("Queue")
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = "Simulated playback — no audio engine in Phase 2.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Reusable mini-player shown above the bottom navigation on library screens. */
@Composable
fun MiniPlayer(playerViewModel: PlayerViewModel, onExpand: () -> Unit, modifier: Modifier = Modifier) {
    val state by playerViewModel.state.collectAsState()
    val track = state.currentTrack ?: return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = Spacing.sm)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .clickable(onClick = onExpand),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArt(seed = track.artworkSeed, title = track.title, size = 48.dp, cornerRadius = 8.dp)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(text = track.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = track.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        IconButton(onClick = { playerViewModel.togglePlayPause() }, modifier = Modifier.size(TouchTargets.min)) {
            Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = if (state.isPlaying) "Pause" else "Play")
        }
        IconButton(onClick = { playerViewModel.next() }, modifier = Modifier.size(TouchTargets.min)) {
            Icon(Icons.Filled.SkipNext, contentDescription = "Next track")
        }
    }
}
