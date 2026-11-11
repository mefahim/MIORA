package com.fahim.powerampclone.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahim.powerampclone.design.Spacing
import com.fahim.powerampclone.model.formatDuration
import com.fahim.powerampclone.playback.EqualizerController
import com.fahim.powerampclone.player.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsScreen(playerViewModel: PlayerViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val state by playerViewModel.state.collectAsState()
    val track = state.currentTrack
    val currentLyrics by playerViewModel.currentTrackLyrics.collectAsState()
    val offsetMs by playerViewModel.lyricOffsetMs.collectAsState()

    // Trigger on-demand companion resolution when screen opens if lyrics not yet resolved
    LaunchedEffect(track?.id) {
        if (track != null && currentLyrics.isEmpty()) {
            playerViewModel.loadLyricsForTrack(track, context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lyrics", style = MaterialTheme.typography.titleLarge) },
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
        if (track == null) {
            EmptyState("No track playing", "Play a song to see its lyrics.", Modifier.padding(padding))
            return@Scaffold
        }

        val effectiveLyrics = if (currentLyrics.isNotEmpty()) currentLyrics else track.lyrics
        val effectivePosition = state.positionMs + offsetMs

        // Find current active lyric line
        val activeIndex = remember(effectivePosition, effectiveLyrics) {
            if (effectiveLyrics.isEmpty()) -1
            else effectiveLyrics.indexOfLast { effectivePosition >= it.startMs }
        }

        val listState = rememberLazyListState()
        var userIsScrolling by remember { mutableStateOf(false) }
        var lastUserScrollTime by remember { mutableLongStateOf(0L) }

        // Track user gesture scrolling so we don't fight their drag
        LaunchedEffect(listState.isScrollInProgress) {
            if (listState.isScrollInProgress) {
                userIsScrolling = true
                lastUserScrollTime = System.currentTimeMillis()
            }
        }

        // Auto-resume sync scrolling after 4 seconds of idle
        LaunchedEffect(userIsScrolling, state.positionMs) {
            if (userIsScrolling && System.currentTimeMillis() - lastUserScrollTime > 4000L) {
                userIsScrolling = false
            }
        }

        // Auto-scroll to active lyric line smoothly
        LaunchedEffect(activeIndex, userIsScrolling) {
            if (!userIsScrolling && activeIndex >= 0 && activeIndex < effectiveLyrics.size) {
                val targetIndex = (activeIndex - 1).coerceAtLeast(0)
                listState.animateScrollToItem(targetIndex)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AlbumArt(
                        seed = track.artworkSeed,
                        title = track.title,
                        size = 52.dp,
                        cornerRadius = 8.dp,
                        albumId = track.albumId,
                        track = track,
                    )
                    Spacer(Modifier.width(Spacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(track.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    if (userIsScrolling) {
                        FilledTonalButton(
                            onClick = { userIsScrolling = false },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp),
                        ) {
                            Text("Resume Sync", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Sync Offset controls toolbar
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.md),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Sync Offset: ${if (offsetMs > 0) "+$offsetMs" else "$offsetMs"} ms",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (offsetMs != 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (offsetMs != 0L) {
                                FilledTonalButton(
                                    onClick = { playerViewModel.resetLyricOffset() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp),
                                ) {
                                    Text("Reset", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            FilledTonalButton(
                                onClick = { playerViewModel.adjustLyricOffset(-500L) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp),
                            ) { Text("-0.5s", style = MaterialTheme.typography.labelSmall) }

                            FilledTonalButton(
                                onClick = { playerViewModel.adjustLyricOffset(-50L) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp),
                            ) { Text("-50ms", style = MaterialTheme.typography.labelSmall) }

                            FilledTonalButton(
                                onClick = { playerViewModel.adjustLyricOffset(50L) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp),
                            ) { Text("+50ms", style = MaterialTheme.typography.labelSmall) }

                            FilledTonalButton(
                                onClick = { playerViewModel.adjustLyricOffset(500L) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp),
                            ) { Text("+0.5s", style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                }
            }

            if (effectiveLyrics.isEmpty()) {
                item {
                    EmptyState(
                        title = "No synced lyrics",
                        message = "No companion .lrc file found for this track. Place a companion file (e.g. \"${track.title}.lrc\") in the same storage directory to enjoy time-synchronized lyrics.",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                items(effectiveLyrics.withIndex().toList()) { (index, line) ->
                    val active = index == activeIndex
                    Text(
                        text = line.text,
                        style = if (active) {
                            MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 21.sp)
                        } else {
                            MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp)
                        },
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val seekTarget = (line.startMs - offsetMs).coerceAtLeast(0L)
                                playerViewModel.seekTo(seekTarget)
                                userIsScrolling = false
                            }
                            .padding(vertical = Spacing.sm),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioInfoScreen(playerViewModel: PlayerViewModel, onBack: () -> Unit) {
    val state by playerViewModel.state.collectAsState()
    val track = state.currentTrack

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Info", style = MaterialTheme.typography.titleLarge) },
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
        if (track == null) {
            EmptyState("No track playing", "Play a song to see its audio information.", Modifier.padding(padding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(Spacing.lg),
        ) {
            item {
                MetadataGroup(
                    title = "Track",
                    pairs = listOf(
                        "Title" to track.title,
                        "Artist" to track.artist,
                        "Album" to track.album,
                        "Genre" to track.genre,
                        "Year" to track.year.toString(),
                    ),
                )
                Spacer(Modifier.height(Spacing.lg))
            }
            item {
                MetadataGroup(
                    title = "Audio",
                    pairs = listOf(
                        "Format" to track.format,
                        "Bitrate" to "${track.bitrateKbps} kbps",
                        "Sample rate" to "${track.sampleRateHz} Hz",
                        "Bit depth" to "${track.bitDepth}-bit",
                        "Duration" to formatDuration(track.durationMs),
                    ),
                )
                Spacer(Modifier.height(Spacing.lg))
            }
            item {
                MetadataGroup(
                    title = "File",
                    pairs = listOf("Path" to track.folderPath),
                )
                Spacer(Modifier.height(Spacing.lg))
            }
            item {
                val isRealFile = track.contentUriString.isNotBlank()
                Text(
                    text = if (isRealFile) {
                        "Direct MediaStore audio track — decoded via native Android audio engine."
                    } else {
                        "Sample metadata for the demo library — values are illustrative for sample tracks."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MetadataGroup(title: String, pairs: List<Pair<String, String>>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.lg)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(Spacing.sm))
            pairs.forEach { (key, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(key, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                }
            }
        }
    }
}

/** Equalizer screen connected to hardware/software AudioEngine equalizer */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    playerViewModel: PlayerViewModel? = null,
    onBack: () -> Unit
) {
    val fallbackController = remember { EqualizerController() }
    val eqState by (playerViewModel?.equalizerState ?: fallbackController.state).collectAsState()
    val presets = EqualizerController.PRESETS
    val frequencies = eqState.frequencies
    val gains = eqState.bandGains
    val preset = eqState.preset
    val isHardwareAttached = eqState.isHardwareAttached

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Equalizer", style = MaterialTheme.typography.titleLarge) },
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
            contentPadding = PaddingValues(Spacing.lg),
        ) {
            item {
                Text("10-band equalizer", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp))
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = if (isHardwareAttached) {
                        "Hardware 10-band equalizer active — processing audio output in real-time."
                    } else {
                        "10-band equalizer active — adjusting audio playback profile."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isHardwareAttached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.lg))

                // Presets chip row
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(presets.keys.toList()) { name ->
                        FilterChip(
                            selected = preset == name,
                            onClick = {
                                if (playerViewModel != null) {
                                    playerViewModel.setEqualizerPreset(name)
                                } else {
                                    fallbackController.setPreset(name)
                                }
                            },
                            label = { Text(name) },
                            shape = RoundedCornerShape(999.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = {
                                if (playerViewModel != null) {
                                    playerViewModel.resetEqualizer()
                                } else {
                                    fallbackController.reset()
                                }
                            },
                            label = { Text("Reset") },
                            leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            shape = RoundedCornerShape(999.dp),
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.lg))
            }

            items(gains.size) { i ->
                val gain = gains[i]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.xs),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "${frequencies.getOrElse(i) { "${i+1}" }} Hz",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            )
                            Text(
                                text = "${if (gain >= 0) "+" else ""}${gain.toInt()} dB",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (gain != 0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Slider(
                            value = gain,
                            onValueChange = { v ->
                                if (playerViewModel != null) {
                                    playerViewModel.setEqualizerBandGain(i, v)
                                } else {
                                    fallbackController.setBandGain(i, v)
                                }
                            },
                            valueRange = -12f..12f,
                            steps = 23,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = "Preset: $preset — drag a fader to switch to Custom.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
