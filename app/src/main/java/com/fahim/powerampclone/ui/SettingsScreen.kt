package com.fahim.powerampclone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.unit.sp
import com.fahim.powerampclone.design.AccentChoice
import com.fahim.powerampclone.design.LocalMioraPalette
import com.fahim.powerampclone.design.Spacing
import com.fahim.powerampclone.design.ThemeMode
import com.fahim.powerampclone.library.LibraryViewModel
import com.fahim.powerampclone.library.SettingsViewModel
import com.fahim.powerampclone.update.UpdateChannel
import com.fahim.powerampclone.update.UpdateManager
import com.fahim.powerampclone.update.UpdateState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    libraryViewModel: LibraryViewModel? = null,
    onBack: (() -> Unit)? = null,
    onNavigateEqualizer: () -> Unit,
) {
    val state by settingsViewModel.state.collectAsState()
    val updateState by settingsViewModel.updateState.collectAsState()
    val updateChannel by settingsViewModel.updateChannel.collectAsState()
    val lastChecked by settingsViewModel.lastCheckedTimestamp.collectAsState()

    val palette = LocalMioraPalette.current
    var themeDialog by remember { mutableStateOf(false) }
    var accentDialog by remember { mutableStateOf(false) }
    var playbackDialog by remember { mutableStateOf(false) }
    var crossfadeDialog by remember { mutableStateOf(false) }
    var channelDialog by remember { mutableStateOf(false) }

    if (channelDialog) {
        ChoiceDialog(
            title = "Update Channel",
            options = listOf(
                "Beta (Pre-release)" to UpdateChannel.BETA,
                "Stable" to UpdateChannel.STABLE,
            ),
            selected = updateChannel,
            onSelect = { settingsViewModel.setUpdateChannel(it); channelDialog = false },
            onDismiss = { channelDialog = false },
        )
    }

    UpdateDialog(
        state = updateState,
        onDownload = { settingsViewModel.startDownload(it) },
        onInstall = { settingsViewModel.installApk(it) },
        onCancelDownload = { settingsViewModel.cancelDownload() },
        onDismiss = { settingsViewModel.dismissUpdate() },
        onRetry = { settingsViewModel.checkForUpdates() },
    )

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
            title = "Accent colour",
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
            title = "Playback mode",
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
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
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                    }
                    Spacer(Modifier.width(Spacing.lg))
                    val trackCount = libraryViewModel?.allTracks?.size ?: 0
                    val trackCountText = "$trackCount ${if (trackCount == 1) "track" else "tracks"}"
                    Column {
                        Text("Music Lover", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Local library • $trackCountText", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item { SettingsGroupTitle("Appearance") }
            item {
                SettingsRow("Theme", "${themeLabel(state.themeMode)} — follows your system when set to System", Icons.Filled.Palette) {
                    themeDialog = true
                }
            }
            item {
                SettingsRow("Accent colour", accentLabel(state.accent), Icons.Filled.Palette) {
                    accentDialog = true
                }
            }
            item {
                SettingsToggleRow("Dynamic colour", "Let the current album's artwork tint the player and accents", state.dynamicColor) {
                    settingsViewModel.setDynamicColor(it)
                }
            }
            item {
                // Live palette preview swatches
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    PaletteSwatch("Accent", palette.accent, palette.onAccent, Modifier.weight(1f))
                    PaletteSwatch("2nd", palette.accent2, Color.White, Modifier.weight(1f))
                    PaletteSwatch("3rd", palette.accent3, Color.White, Modifier.weight(1f))
                    PaletteSwatch("Player", palette.playerBg1, Color.White, Modifier.weight(1f))
                }
            }

            item { SettingsGroupTitle("Library") }
            item {
                SettingsToggleRow("Auto-add playlists", "Add new folders as playlists automatically", state.autoAddPlaylists) {
                    settingsViewModel.setAutoAddPlaylists(it)
                }
            }
            item {
                val trackCount = libraryViewModel?.allTracks?.size ?: 0
                val trackCountText = "$trackCount ${if (trackCount == 1) "track" else "tracks"}"
                SettingsRow("Scan for music", "$trackCountText indexed", Icons.Filled.Folder) {
                    libraryViewModel?.refresh()
                }
            }

            item { SettingsGroupTitle("Player") }
            item {
                SettingsRow("Playback mode", state.playbackMode, Icons.Filled.PlayArrow) {
                    playbackDialog = true
                }
            }
            item {
                SettingsRow("Crossfade", state.crossfade, Icons.Filled.MusicNote) {
                    crossfadeDialog = true
                }
            }
            item {
                SettingsRow("Equalizer", "10-band hardware equalizer, tone controls & reverb", Icons.Filled.Tune) {
                    onNavigateEqualizer()
                }
            }

            item { SettingsGroupTitle("App Updates") }
            item {
                SettingsRow(
                    title = "Current Version",
                    subtitle = "v${UpdateManager.CURRENT_VERSION_NAME} (Build ${UpdateManager.CURRENT_VERSION_CODE})",
                    icon = Icons.Filled.Info,
                ) {}
            }
            item {
                SettingsRow(
                    title = "Update Channel",
                    subtitle = updateChannel.displayName + " — " + updateChannel.description,
                    icon = Icons.Filled.SystemUpdate,
                ) {
                    channelDialog = true
                }
            }
            item {
                val lastCheckText = if (lastChecked > 0L) {
                    val date = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(lastChecked))
                    "Last checked: $date • Tap to check now"
                } else {
                    "Tap to check GitHub for updates"
                }
                SettingsRow(
                    title = "Check for Updates",
                    subtitle = lastCheckText,
                    icon = Icons.Filled.Refresh,
                ) {
                    settingsViewModel.checkForUpdates()
                }
            }

            item { SettingsGroupTitle("About") }
            item {
                SettingsRow("MIORA", "Version ${UpdateManager.CURRENT_VERSION_NAME} • Your music. Your moments.", Icons.Filled.Info) {}
            }
            item {
                Text(
                    text = "Audiophile-grade Android native music player with dynamic colour theming, persistent playlists, background audio playback, and secure in-app updates.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Spacing.lg),
                )
                Spacer(Modifier.height(Spacing.xxl))
            }
        }
    }
}

@Composable
private fun PaletteSwatch(label: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = fg)
    }
}

private fun themeLabel(mode: ThemeMode) = when (mode) {
    ThemeMode.SYSTEM -> "System default"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

private fun accentLabel(choice: AccentChoice) = when (choice) {
    AccentChoice.DYNAMIC -> "Dynamic (Material You)"
    AccentChoice.OCEAN -> "Ocean"
    AccentChoice.FOREST -> "Forest"
    AccentChoice.EMBER -> "Ember"
    AccentChoice.ROSE -> "Rose"
    AccentChoice.GRAPHITE -> "Graphite"
}

@Composable
private fun SettingsGroupTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.lg, bottom = Spacing.xs),
    )
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(Spacing.lg))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                options.forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = value == selected, onClick = { onSelect(value) })
                        Spacer(Modifier.width(Spacing.sm))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}
