package com.fahim.powerampclone.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.fahim.powerampclone.design.Spacing
import com.fahim.powerampclone.model.formatDuration
import com.fahim.powerampclone.player.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsScreen(playerViewModel: PlayerViewModel, onBack: () -> Unit) {
    val state by playerViewModel.state.collectAsState()
    val track = state.currentTrack
    Scaffold(topBar = { TopAppBar(title = { Text("Lyrics") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }) }) { padding ->
        if (track == null) {
            EmptyState("No track playing", "Play a song to see its lyrics.", Modifier.padding(padding))
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(padding).padding(Spacing.lg)) {
            Text(text = track.title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(text = track.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(Spacing.lg))
            if (track.lyrics.isEmpty()) {
                EmptyState("No synced lyrics", "This sample track has no embedded LRC lyrics yet.", Modifier.fillMaxWidth())
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(track.lyrics) { line ->
                        val active = state.positionMs >= line.startMs &&
                            state.positionMs < (track.lyrics.getOrNull(track.lyrics.indexOf(line) + 1)?.startMs ?: Long.MAX_VALUE)
                        Text(
                            text = line.text,
                            style = if (active) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
                        )
                    }
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
    Scaffold(topBar = { TopAppBar(title = { Text("Audio Info") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }) }) { padding ->
        if (track == null) {
            EmptyState("No track playing", "Play a song to see its audio information.", Modifier.padding(padding))
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(padding).padding(Spacing.lg)) {
            val rows = listOf(
                "Title" to track.title,
                "Artist" to track.artist,
                "Album" to track.album,
                "Format" to track.format,
                "Bitrate" to "${track.bitrateKbps} kbps",
                "Sample rate" to "${track.sampleRateHz} Hz",
                "Bit depth" to "${track.bitDepth}-bit",
                "Duration" to formatDuration(track.durationMs),
                "Year" to track.year.toString(),
                "Genre" to track.genre,
                "File" to track.folderPath,
            )
            rows.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth().padding(vertical = Spacing.sm)) {
                    Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(text = value, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(Spacing.lg))
            Text(
                text = "Sample metadata for the demo library — values are illustrative, not measured from a real file.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Equalizer UI preview only. Sliders move and presets apply visually; no audio is processed in Phase 2. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(onBack: () -> Unit) {
    var gains by remember { mutableStateOf(List(10) { 0f }) }
    var preset by remember { mutableStateOf("Flat") }
    val presets = mapOf(
        "Flat" to List(10) { 0f },
        "Rock" to listOf(4f, 3f, 1f, 0f, -1f, 0f, 1f, 3f, 4f, 5f),
        "Pop" to listOf(-1f, 0f, 2f, 3f, 3f, 2f, 0f, -1f, -1f, 0f),
        "Jazz" to listOf(3f, 2f, 1f, 0f, -1f, 0f, 1f, 2f, 3f, 3f),
        "Classical" to listOf(4f, 3f, 2f, 0f, -1f, -1f, 0f, 2f, 3f, 4f),
    )
    val frequencies = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
    Scaffold(topBar = { TopAppBar(title = { Text("Equalizer") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(Spacing.lg)) {
            item {
                Text("10-band graphic equalizer", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Preview only — these sliders do not process audio in Phase 2. The DSP engine arrives in a later phase.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.lg))
                Row(Modifier.fillMaxWidth()) {
                    presets.keys.forEach { name ->
                        androidx.compose.material3.FilterChip(
                            selected = preset == name,
                            onClick = { preset = name; gains = presets.getValue(name) },
                            label = { Text(name) },
                            modifier = Modifier.padding(end = Spacing.sm),
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.lg))
            }
            items(gains.size) { i ->
                Column(Modifier.fillMaxWidth().padding(vertical = Spacing.xs)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text("${frequencies[i]} Hz", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text("${if (gains[i] >= 0) "+" else ""}${gains[i].toInt()} dB", style = MaterialTheme.typography.bodyMedium)
                    }
                    Slider(
                        value = gains[i],
                        onValueChange = { v -> gains = gains.toMutableList().also { it[i] = v }; preset = "Custom" },
                        valueRange = -12f..12f,
                    )
                }
            }
        }
    }
}
