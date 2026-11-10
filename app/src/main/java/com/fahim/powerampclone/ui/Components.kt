package com.fahim.powerampclone.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fahim.powerampclone.design.Spacing
import com.fahim.powerampclone.design.TouchTargets
import com.fahim.powerampclone.model.formatDuration

/**
 * Original placeholder artwork: a deterministic gradient + soft circles derived
 * from the album seed colour. No proprietary images are used anywhere.
 */
@Composable
fun AlbumArt(seed: Color, title: String, size: Dp, modifier: Modifier = Modifier, cornerRadius: Dp = 12.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.linearGradient(listOf(seed, seed.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.35f))))
            .semantics { contentDescription = "Album artwork for $title" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            drawCircle(color = Color.White.copy(alpha = 0.22f), radius = size.toPx() * 0.32f, center = Offset(size.toPx() * 0.72f, size.toPx() * 0.28f))
            drawCircle(color = Color.White.copy(alpha = 0.14f), radius = size.toPx() * 0.45f, center = Offset(size.toPx() * 0.25f, size.toPx() * 0.78f))
        }
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(size * 0.38f),
        )
    }
}

@Composable
fun TrackRow(
    title: String,
    subtitle: String,
    durationMs: Long,
    seed: Color,
    isPlaying: Boolean,
    isFavorite: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    leadingNumber: Int? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else if (isPlaying) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingNumber != null) {
            Text(text = "$leadingNumber", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(28.dp))
        }
        AlbumArt(seed = seed, title = title, size = 48.dp, cornerRadius = 8.dp)
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (isFavorite) {
            Text(text = "♥", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = Spacing.sm))
        }
        Text(text = formatDuration(durationMs), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        IconButton(onClick = onMoreClick, modifier = Modifier.size(TouchTargets.min)) {
            Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More options for $title")
        }
    }
}

@Composable
fun SectionHeader(title: String, actionLabel: String? = null, onActionClick: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (actionLabel != null && onActionClick != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onActionClick, role = Role.Button)
                    .padding(Spacing.sm),
            )
        }
    }
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(Spacing.xxl), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(imageVector = Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(Spacing.lg))
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.xs))
        Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun WaveformStrip(seed: Color, progress: Float, modifier: Modifier = Modifier) {
    // Deterministic peaks derived from the seed colour so the strip is stable per track.
    val peaks = rememberPeaks(seed)
    Canvas(modifier = modifier.fillMaxWidth().height(48.dp)) {
        val barWidth = size.width / peaks.size
        peaks.forEachIndexed { i, peak ->
            val h = size.height * peak
            val played = i.toFloat() / peaks.size <= progress
            drawRoundRect(
                color = if (played) seed else seed.copy(alpha = 0.35f),
                topLeft = Offset(i * barWidth + 1f, (size.height - h) / 2f),
                size = androidx.compose.ui.geometry.Size(barWidth - 2f, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
            )
        }
    }
}

@Composable
private fun rememberPeaks(seed: Color): List<Float> {
    val base = (seed.value.toLong() and 0xFFFF).toInt()
    return List(48) { i ->
        val v = kotlin.math.sin((i * 0.7 + base % 10).toDouble()).toFloat()
        (0.25f + 0.65f * kotlin.math.abs(v) * (0.6f + 0.4f * kotlin.math.sin(i * 0.31).toFloat().let { kotlin.math.abs(it) }))
    }
}
