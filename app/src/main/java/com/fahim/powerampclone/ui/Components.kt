package com.fahim.powerampclone.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahim.powerampclone.design.Spacing
import com.fahim.powerampclone.design.TouchTargets
import com.fahim.powerampclone.model.formatDuration
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Color helpers matching web-preview/js/artwork.js shadeHex() & mixHex() */
fun shadeColor(color: Color, amtInt: Int): Color {
    val factor = amtInt / 255f
    val r = (color.red + factor).coerceIn(0f, 1f)
    val g = (color.green + factor).coerceIn(0f, 1f)
    val b = (color.blue + factor).coerceIn(0f, 1f)
    return Color(r, g, b, color.alpha)
}

fun mixColors(c1: Color, c2: Color, t: Float): Color {
    val r = c1.red + (c2.red - c1.red) * t
    val g = c1.green + (c2.green - c1.green) * t
    val b = c1.blue + (c2.blue - c1.blue) * t
    val a = c1.alpha + (c2.alpha - c1.alpha) * t
    return Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f), a.coerceIn(0f, 1f))
}

/**
 * Procedural vector cover art engine. Directly translates the 7 album motifs
 * from web-preview/js/artwork.js onto a hardware-accelerated Compose Canvas.
 * No external raster assets, scalable to any resolution.
 */
@Composable
fun AlbumArt(
    seed: Color,
    title: String,
    size: Dp,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    albumId: Long? = null,
    motif: String? = null,
    isCircle: Boolean = false,
) {
    val shape = if (isCircle) CircleShape else RoundedCornerShape(cornerRadius)
    val resolvedMotif = motif ?: when (albumId) {
        1L -> "arcs"
        2L -> "sunrise"
        3L -> "neon"
        4L -> "waves"
        5L -> "eq"
        6L -> "skyline"
        7L -> "orbits"
        else -> "fallback"
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .semantics { contentDescription = "Album artwork for $title" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val scaleFactor = this.size.width / 200f
            scale(scaleFactor, scaleFactor, pivot = Offset.Zero) {
                drawMotif(resolvedMotif, seed)
            }
        }
    }
}

private fun DrawScope.drawMotif(motif: String, seed: Color) {
    when (motif) {
        "arcs" -> drawArcsMotif(seed)
        "sunrise" -> drawSunriseMotif(seed)
        "neon" -> drawNeonMotif(seed)
        "waves" -> drawWavesMotif(seed)
        "eq" -> drawEqMotif(seed)
        "skyline" -> drawSkylineMotif(seed)
        "orbits" -> drawOrbitsMotif(seed)
        else -> drawFallbackMotif(seed)
    }
}

// 1 Dreamscape — concentric arcs over a horizon
private fun DrawScope.drawArcsMotif(seed: Color) {
    val light = mixColors(seed, Color.White, 0.55f)
    val dark = shadeColor(seed, -60)
    drawRect(color = dark, size = Size(200f, 200f))
    drawRect(color = seed.copy(alpha = 0.35f), topLeft = Offset(0f, 128f), size = Size(200f, 72f))
    for (i in 0 until 5) {
        val ringColor = if (i % 2 == 1) light else Color.White
        val op = (0.85f - i * 0.13f).coerceIn(0f, 1f)
        drawCircle(
            color = ringColor.copy(alpha = op),
            radius = 26f + i * 15f,
            center = Offset(100f, 128f),
            style = Stroke(width = 5f),
        )
    }
    drawCircle(color = light, radius = 10f, center = Offset(100f, 128f))
}

// 2 Better Tomorrow — sunrise with rays
private fun DrawScope.drawSunriseMotif(seed: Color) {
    val light = mixColors(seed, Color(0xFFFFF7D6), 0.5f)
    val dark = shadeColor(seed, -55)
    drawRect(color = dark, size = Size(200f, 200f))
    for (i in 0 until 7) {
        val rad = Math.toRadians((-150.0 + i * 25.0))
        val x2 = (100.0 + cos(rad) * 86.0).toFloat()
        val y2 = (118.0 + sin(rad) * 86.0).toFloat()
        drawLine(
            color = light.copy(alpha = 0.8f),
            start = Offset(100f, 118f),
            end = Offset(x2, y2),
            strokeWidth = 4f,
            cap = StrokeCap.Round,
        )
    }
    drawCircle(color = light, radius = 34f, center = Offset(100f, 118f))
    for (i in 0 until 4) {
        val op = (0.5f - i * 0.1f).coerceIn(0f, 1f)
        drawRoundRect(
            color = Color.White.copy(alpha = op),
            topLeft = Offset(24f, 138f + i * 12f),
            size = Size(152f, 5f),
            cornerRadius = CornerRadius(2.5f, 2.5f),
        )
    }
}

// 3 Midnight Lights — neon perspective grid
private fun DrawScope.drawNeonMotif(seed: Color) {
    val light = mixColors(seed, Color.White, 0.45f)
    val dark = shadeColor(seed, -70)
    drawRect(color = dark, size = Size(200f, 200f))
    for (i in 0..6) {
        drawLine(
            color = seed.copy(alpha = 0.7f),
            start = Offset(20f + i * 27f, 200f),
            end = Offset(100f + (i - 3) * 16f, 96f),
            strokeWidth = 2f,
        )
    }
    for (i in 0 until 4) {
        val op = (0.55f - i * 0.1f).coerceIn(0f, 1f)
        drawLine(
            color = seed.copy(alpha = op),
            start = Offset(0f, 118f + i * 24f),
            end = Offset(200f, 118f + i * 24f),
            strokeWidth = 2f,
        )
    }
    drawCircle(color = light, radius = 26f, center = Offset(100f, 96f), style = Stroke(width = 3f))
    val dots = listOf(
        Pair(Offset(52f, 66f), 5f),
        Pair(Offset(100f, 44f), 7f),
        Pair(Offset(148f, 66f), 5f),
        Pair(Offset(76f, 88f), 5f),
        Pair(Offset(124f, 88f), 5f),
    )
    dots.forEach { (center, r) ->
        drawCircle(color = light, radius = r, center = center)
    }
}

// 4 Ocean Breeze — layered waves
private fun DrawScope.drawWavesMotif(seed: Color) {
    val dark = shadeColor(seed, -60)
    drawRect(color = dark, size = Size(200f, 200f))

    fun drawWave(y: Float, amp: Float, op: Float, fill: Color) {
        val path = Path().apply {
            moveTo(0f, y)
            quadraticTo(25f, y - amp, 50f, y)
            quadraticTo(75f, y + amp, 100f, y)
            quadraticTo(125f, y - amp, 150f, y)
            quadraticTo(175f, y + amp, 200f, y)
            lineTo(200f, 200f)
            lineTo(0f, 200f)
            close()
        }
        drawPath(path, color = fill.copy(alpha = op))
    }

    drawWave(84f, 22f, 0.55f, mixColors(seed, Color.White, 0.35f))
    drawWave(118f, 26f, 0.7f, seed)
    drawWave(152f, 20f, 0.9f, mixColors(seed, Color.White, 0.15f))
    drawCircle(color = Color.White.copy(alpha = 0.85f), radius = 18f, center = Offset(152f, 48f))
}

// 5 Feel the Beat — equalizer bars
private fun DrawScope.drawEqMotif(seed: Color) {
    val dark = shadeColor(seed, -62)
    drawRect(color = dark, size = Size(200f, 200f))
    val heights = listOf(52f, 96f, 68f, 128f, 84f, 112f, 58f, 92f)
    heights.forEachIndexed { i, h ->
        val fill = if (i % 2 == 1) mixColors(seed, Color.White, 0.35f) else seed
        drawRoundRect(
            color = fill,
            topLeft = Offset(18f + i * 21f, 168f - h),
            size = Size(13f, h),
            cornerRadius = CornerRadius(6.5f, 6.5f),
        )
    }
    drawRoundRect(
        color = Color.White.copy(alpha = 0.35f),
        topLeft = Offset(18f, 172f),
        size = Size(164f, 4f),
        cornerRadius = CornerRadius(2f, 2f),
    )
}

// 6 City Lights — skyline with lit windows
private fun DrawScope.drawSkylineMotif(seed: Color) {
    val dark = shadeColor(seed, -66)
    val light = mixColors(seed, Color.White, 0.5f)
    drawRect(color = dark, size = Size(200f, 200f))
    drawCircle(color = light, radius = 16f, center = Offset(158f, 42f))

    val buildings = listOf(
        listOf(14f, 96f, 34f, 104f),
        listOf(54f, 62f, 30f, 138f),
        listOf(90f, 84f, 36f, 116f),
        listOf(132f, 56f, 30f, 144f),
        listOf(168f, 92f, 22f, 108f),
    )
    buildings.forEach { b ->
        drawRect(color = seed, topLeft = Offset(b[0], b[1]), size = Size(b[2], b[3]))
        val bx = b[0].toInt()
        val by = b[1]
        for (r in 0 until 4) {
            for (c in 0 until 2) {
                if ((r + c + bx) % 3 == 0) {
                    drawRect(
                        color = light.copy(alpha = 0.9f),
                        topLeft = Offset(b[0] + 6f + c * 12f, by + 10f + r * 16f),
                        size = Size(5f, 7f),
                    )
                }
            }
        }
    }
}

// 7 Infinite Moment — orbit rings
private fun DrawScope.drawOrbitsMotif(seed: Color) {
    val dark = shadeColor(seed, -60)
    val light = mixColors(seed, Color.White, 0.5f)
    drawRect(color = dark, size = Size(200f, 200f))
    drawCircle(color = seed, radius = 30f, center = Offset(100f, 100f))

    rotate(degrees = -24f, pivot = Offset(100f, 100f)) {
        drawOval(
            color = light,
            topLeft = Offset(100f - 74f, 100f - 26f),
            size = Size(148f, 52f),
            style = Stroke(width = 3f),
        )
    }

    drawCircle(
        color = light.copy(alpha = 0.7f),
        radius = 56f,
        center = Offset(100f, 100f),
        style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 9f))),
    )
    drawCircle(color = light, radius = 7f, center = Offset(162f, 66f))
    drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 5f, center = Offset(52f, 140f))
}

// Fallback — duotone + circle
private fun DrawScope.drawFallbackMotif(seed: Color) {
    val dark = shadeColor(seed, -55)
    drawRect(color = dark, size = Size(200f, 200f))
    val path = Path().apply {
        moveTo(0f, 200f)
        lineTo(200f, 0f)
        lineTo(200f, 200f)
        close()
    }
    drawPath(path, color = seed.copy(alpha = 0.55f))
    drawCircle(color = Color.White.copy(alpha = 0.85f), radius = 46f, center = Offset(100f, 100f), style = Stroke(width = 4f))
    drawCircle(color = Color.White.copy(alpha = 0.9f), radius = 14f, center = Offset(100f, 100f))
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
    albumId: Long? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingNumber != null) {
            Text(
                text = "$leadingNumber",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp),
            )
        }
        AlbumArt(seed = seed, title = title, size = 48.dp, cornerRadius = 8.dp, albumId = albumId)
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isFavorite) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Favourite",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(17.dp).padding(end = Spacing.xs),
            )
        }
        Text(
            text = formatDuration(durationMs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(onClick = onMoreClick, modifier = Modifier.size(TouchTargets.min)) {
            Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More options for $title")
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    modifier: Modifier = Modifier,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null && onActionClick != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onActionClick, role = Role.Button)
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            )
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(Spacing.lg))
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Spacing.lg))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun WaveformStrip(seed: Color, progress: Float, modifier: Modifier = Modifier) {
    val peaks = rememberPeaks(seed)
    Canvas(modifier = modifier.fillMaxWidth().height(48.dp)) {
        val barWidth = size.width / peaks.size
        peaks.forEachIndexed { i, peak ->
            val h = size.height * peak
            val played = i.toFloat() / peaks.size <= progress
            drawRoundRect(
                color = if (played) seed else seed.copy(alpha = 0.28f),
                topLeft = Offset(i * barWidth + 1f, (size.height - h) / 2f),
                size = Size((barWidth - 2f).coerceAtLeast(1f), h),
                cornerRadius = CornerRadius(2f, 2f),
            )
        }
    }
}

private fun rememberPeaks(seed: Color): List<Float> {
    val base = (seed.value.toLong() and 0xFFFF).toInt() % 10
    return List(48) { i ->
        val v = abs(sin(i * 0.7 + base)).toFloat()
        (0.25f + 0.65f * v * (0.6f + 0.4f * abs(sin(i * 0.31)).toFloat()))
    }
}
