package com.fahim.powerampclone.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.fahim.powerampclone.design.AccentChoice
import com.fahim.powerampclone.design.Spacing
import com.fahim.powerampclone.design.ThemeMode
import com.fahim.powerampclone.library.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel, onNavigateEqualizer: () -> Unit) {
    val state by settingsViewModel.state.collectAsState()
    var themeDialog by remember { mutableStateOf(false) }
    var accentDialog by remember { mutableStateOf(false) }
    var playbackDialog by remember { mutableStateOf(false) }
    var crossfadeDialog by remember { mutableStateOf(false) }

    if (themeDialog) {
        ChoiceDialog(
            title = "Theme",
            options = listOf("System default" to ThemeMode.SYSTEM, "Light" to ThemeMode.LIGHT, "Dark" to ThemeMode.DARK),
            selected = state.themeMode,
            onSelect = { settingsViewModel.setThemeMode(it); themeDialog = false },
            onDismiss = { themeDialog = false },
        )
    }
    if (accentDialog) {
        ChoiceDialog(
            title = "Accent Color",
            options = listOf(
                "Dynamic (Material You)" to AccentChoice.DYNAMIC,
                "Ocean" to AccentChoice.OCEAN,
                "Forest" to AccentChoice.FOREST,
                "Ember" to AccentChoice.EMBER,
                "Rose" to AccentChoice.ROSE,
                "Graphite" to AccentChoice.GRAPHITE,
            ),
            selected = state.accent,
            onSelect = { settingsViewModel.setAccent(it); accentDialog = false },
            onDismiss = { accentDialog = false },
        )
    }
    if (playbackDialog) {
        ChoiceDialog(
            title = "Playback Mode",
            options = listOf("Normal" to "Normal", "Gapless (preview)" to "Gapless", "Crossfade (preview)" to "Crossfade"),
            selected = state.playbackMode,
            onSelect = { settingsViewModel.setPlaybackMode(it); playbackDialog = false },
            onDismiss = { playbackDialog = false },
        )
    }
    if (crossfadeDialog) {
        ChoiceDialog(
            title = "Crossfade",
            options = listOf("Off" to "Off", "2 seconds" to "2s", "5 seconds" to "5s", "10 seconds" to "10s"),
            selected = state.crossfade,
            onSelect = { settingsViewModel.setCrossfade(it); crossfadeDialog = false },
            onDismiss = { crossfadeDialog = false },
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings", style = MaterialTheme.typography.titleLarge) }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Row(Modifier.fillMaxWidth().padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp).clip(CircleShape))
                    Spacer(Modifier.width(Spacing.lg))
                    Column {
                        Text("Music Lover", style = MaterialTheme.typography.titleMedium)
                        Text("Local library • Sample data", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { SettingsGroupTitle("Appearance") }
            item { SettingsRow("Theme", themeLabel(state.themeMode), Icons.Filled.Palette) { themeDialog = true } }
            item { SettingsRow("Accent Color", accentLabel(state.accent), Icons.Filled.Palette) { accentDialog = true } }
            item {
                SettingsToggleRow("Dynamic Color", "Follow Material You on Android 12+", state.dynamicColor) { settingsViewModel.setDynamicColor(it) }
            }
            item { SettingsGroupTitle("Library") }
            item {
                SettingsToggleRow("Auto Add Playlists", "Add new folders as playlists automatically", state.autoAddPlaylists) { settingsViewModel.setAutoAddPlaylists(it) }
            }
            item { SettingsRow("Scan for Music", "Sample library — 20 tracks indexed", Icons.Filled.Palette) {} }
            item { SettingsGroupTitle("Player") }
            item { SettingsRow("Playback Mode", state.playbackMode, Icons.Filled.Palette) { playbackDialog = true } }
            item { SettingsRow("Crossfade", state.crossfade + " — preview only, no audio engine yet", Icons.Filled.Palette) { crossfadeDialog = true } }
            item { SettingsRow("Equalizer", "Preview only — sliders do not process audio in Phase 2", Icons.Filled.Palette) { onNavigateEqualizer() } }
            item { SettingsGroupTitle("About") }
            item { SettingsRow("Poweramp Clone", "Version 1.0.0 • Phase 2 UI foundation", Icons.Filled.Info) {} }
            item {
                Text(
                    text = "Clean-room UI. Simulated playback only — no audio decoding, DSP or streaming in this phase.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Spacing.lg),
                )
                Spacer(Modifier.height(Spacing.xxl))
            }
        }
    }
}

private fun themeLabel(mode: ThemeMode) = when (mode) { ThemeMode.SYSTEM -> "System default"; ThemeMode.LIGHT -> "Light"; ThemeMode.DARK -> "Dark" }
private fun accentLabel(choice: AccentChoice) = when (choice) {
    AccentChoice.DYNAMIC -> "Dynamic (Material You)"
    AccentChoice.OCEAN -> "Ocean"; AccentChoice.FOREST -> "Forest"; AccentChoice.EMBER -> "Ember"
    AccentChoice.ROSE -> "Rose"; AccentChoice.GRAPHITE -> "Graphite"
}

@Composable
private fun SettingsGroupTitle(title: String) {
    Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm))
}

@Composable
private fun SettingsRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(Spacing.lg))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun <T> ChoiceDialog(title: String, options: List<Pair<String, T>>, selected: T, onSelect: (T) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (label, value) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(value) }.padding(vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.RadioButton(selected = value == selected, onClick = { onSelect(value) })
                        Spacer(Modifier.width(Spacing.sm))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}
