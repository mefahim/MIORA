package com.fahim.powerampclone.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahim.powerampclone.design.Spacing
import com.fahim.powerampclone.model.formatDuration
import com.fahim.powerampclone.player.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsScreen(playerViewModel: PlayerViewModel, onBack: () -> Unit) {
    val state by playerViewModel.state.collectAsState()
    val track = state.currentTrack

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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AlbumArt(
                        seed = track.artworkSeed,
                        title = track.title,
                        size = 52.dp,
                        cornerRadius = 8.dp,
                        albumId = track.albumId,
                    )
                    Spacer(Modifier.width(Spacing.md))
                    Column {
                        Text(track.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (track.lyrics.isEmpty()) {
                item {
                    EmptyState(
                        title = "No synced lyrics",
                        message = "This sample track has no embedded lyrics yet. Lyrics are shown only when the track actually carries them — none are invented here.",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                items(track.lyrics.withIndex().toList()) { (index, line) ->
                    val nextStart = track.lyrics.getOrNull(index + 1)?.startMs ?: Long.MAX_VALUE
                    val active = state.positionMs in line.startMs until nextStart
                    Text(
                        text = line.text,
                        style = if (active) {
                            MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        } else {
                            MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp)
                        },
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .fillMaxWidth()
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
                Text(
                    text = "Sample metadata for the demo library — values are illustrative, not measured from a real file.",
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

/** Equalizer UI preview matching web-preview/js/settings.js equalizer() */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(onBack: () -> Unit) {
    val presets = mapOf(
        "Flat" to listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        "Rock" to listOf(4f, 3f, 1f, 0f, -1f, 0f, 1f, 3f, 4f, 5f),
        "Pop" to listOf(-1f, 0f, 2f, 3f, 3f, 2f, 0f, -1f, -1f, 0f),
        "Jazz" to listOf(3f, 2f, 1f, 0f, -1f, 0f, 1f, 2f, 3f, 3f),
        "Classical" to listOf(4f, 3f, 2f, 0f, -1f, -1f, 0f, 2f, 3f, 4f),
    )
    val frequencies = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

    var gains by remember { mutableStateOf(presets.getValue("Flat")) }
    var preset by remember { mutableStateOf("Flat") }

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
                    "Preview only — these faders do not process audio in Phase 2. The DSP engine arrives in a later phase.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.lg))

                // Presets chip row
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(presets.keys.toList()) { name ->
                        FilterChip(
                            selected = preset == name,
                            onClick = {
                                preset = name
                                gains = presets.getValue(name)
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
                                preset = "Flat"
                                gains = presets.getValue("Flat")
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
                                text = "${frequencies[i]} Hz",
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
                                gains = gains.toMutableList().also { it[i] = v }
                                preset = "Custom"
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
