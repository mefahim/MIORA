package com.fahim.powerampclone.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahim.powerampclone.design.Spacing
import com.fahim.powerampclone.update.ReleaseInfo
import com.fahim.powerampclone.update.UpdateInstaller
import com.fahim.powerampclone.update.UpdateState
import java.io.File

@Composable
fun UpdateDialog(
    state: UpdateState,
    onDownload: (ReleaseInfo) -> Unit,
    onInstall: (File) -> Unit,
    onCancelDownload: () -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    val context = LocalContext.current

    when (state) {
        is UpdateState.Idle -> { /* No-op */ }

        is UpdateState.Checking -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = { CircularProgressIndicator(modifier = Modifier.padding(Spacing.sm)) },
                title = { Text("Checking for updates…", style = MaterialTheme.typography.titleMedium) },
                text = { Text("Connecting to GitHub Releases…", style = MaterialTheme.typography.bodyMedium) },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            )
        }

        is UpdateState.UpToDate -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = { Text("You're up to date", style = MaterialTheme.typography.titleLarge) },
                text = {
                    Text(
                        "MIORA v${state.currentVersion} is currently the latest version on your channel.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text("OK") }
                }
            )
        }

        is UpdateState.UpdateAvailable -> {
            val release = state.release
            val sizeMb = if (release.apkAsset.sizeBytes > 0) {
                " • %.1f MB".format(release.apkAsset.sizeBytes / (1024f * 1024f))
            } else ""

            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        Icons.Filled.NewReleases,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "New Update Available",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = release.tagName,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (release.isPrerelease) {
                                Spacer(Modifier.width(Spacing.xs))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "BETA",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                            Text(
                                text = sizeMb,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                text = {
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            text = "Release Notes:",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(Spacing.md)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = release.changelog.ifBlank { "No detailed changelog provided." },
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 18.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { onDownload(release) }) {
                        Icon(Icons.Filled.Download, contentDescription = null)
                        Spacer(Modifier.width(Spacing.xs))
                        Text("Download & Install")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Later") }
                }
            )
        }

        is UpdateState.Downloading -> {
            val progress = state.progressPercent / 100f
            val downloadedMb = state.bytesDownloaded / (1024f * 1024f)
            val totalMb = state.totalBytes / (1024f * 1024f)

            AlertDialog(
                onDismissRequest = onCancelDownload,
                title = { Text("Downloading Update…", style = MaterialTheme.typography.titleLarge) },
                text = {
                    Column(Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                text = "${state.progressPercent}%",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.weight(1f))
                            if (state.totalBytes > 0) {
                                Text(
                                    text = "%.1f / %.1f MB".format(downloadedMb, totalMb),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = onCancelDownload) { Text("Cancel") }
                }
            )
        }

        is UpdateState.ReadyToInstall -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = { Text("Update Ready", style = MaterialTheme.typography.titleLarge) },
                text = {
                    Column {
                        Text(
                            text = "MIORA ${state.release.tagName} has been downloaded and verified.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        if (state.checksumVerified) {
                            Text(
                                text = "✓ Cryptographic SHA-256 signature verified",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { onInstall(state.apkFile) }) {
                        Text("Install Now")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Later") }
                }
            )
        }

        is UpdateState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        Icons.Filled.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("Update Check Failed", style = MaterialTheme.typography.titleLarge) },
                text = {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    if (state.canRetry) {
                        Button(onClick = onRetry) { Text("Retry") }
                    } else {
                        Button(onClick = onDismiss) { Text("OK") }
                    }
                },
                dismissButton = {
                    if (state.canRetry) {
                        TextButton(onClick = onDismiss) { Text("Dismiss") }
                    }
                }
            )
        }
    }
}
