package com.fahim.powerampclone.artwork

import android.graphics.Bitmap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.fahim.powerampclone.model.Track
import kotlin.math.abs
import kotlin.math.max

/**
 * Coordinated palette derived dynamically from the current playing track's artwork or seed.
 */
data class DynamicPalette(
    val dominantColor: Color,
    val primaryColor: Color,
    val secondaryColor: Color,
    val tertiaryColor: Color,
    val surfaceBackground: Color,
    val gradientTop: Color,
    val gradientBottom: Color,
    val onBackground: Color,
    val onPrimary: Color,
    val accent: Color,
    val isDark: Boolean = true,
)

object PaletteGenerator {

    private val paletteCache = object : LinkedHashMap<String, DynamicPalette>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, DynamicPalette>?): Boolean {
            return size > 64
        }
    }

    @Synchronized
    fun generate(
        bitmap: Bitmap?,
        fallbackSeed: Color,
        isDark: Boolean = true,
        cacheKey: String? = null,
    ): DynamicPalette {
        if (cacheKey != null) {
            val key = "${cacheKey}_${if (isDark) "dark" else "light"}"
            val cached = paletteCache[key]
            if (cached != null) return cached
        }

        val palette = if (bitmap != null && !bitmap.isRecycled) {
            extractFromBitmap(bitmap, fallbackSeed, isDark)
        } else {
            extractFromSeed(fallbackSeed, isDark)
        }

        if (cacheKey != null) {
            val key = "${cacheKey}_${if (isDark) "dark" else "light"}"
            synchronized(paletteCache) {
                paletteCache[key] = palette
            }
        }

        return palette
    }

    private fun extractFromBitmap(bitmap: Bitmap, fallbackSeed: Color, isDark: Boolean): DynamicPalette {
        try {
            val width = bitmap.width
            val height = bitmap.height
            val stepX = max(1, width / 20)
            val stepY = max(1, height / 20)

            var bestScore = -1f
            var bestHue = 180f
            var bestSat = 0.6f
            var bestVal = 0.6f

            for (y in 0 until height step stepY) {
                for (x in 0 until width step stepX) {
                    val pixel = bitmap.getPixel(x, y)
                    val r = ((pixel shr 16) and 0xFF) / 255f
                    val g = ((pixel shr 8) and 0xFF) / 255f
                    val b = (pixel and 0xFF) / 255f
                    val (hue, sat, v) = colorToHsv(r, g, b)

                    // Ignore extreme black, extreme white, and grey
                    if (v < 0.12f || (v > 0.90f && sat < 0.15f) || sat < 0.15f) {
                        continue
                    }

                    // Weight by saturation and balance between dark and light
                    val score = sat * (1f - abs(v - 0.5f))
                    if (score > bestScore) {
                        bestScore = score
                        bestHue = hue
                        bestSat = sat
                        bestVal = v
                    }
                }
            }

            if (bestScore > 0f) {
                return buildPaletteFromHsv(bestHue, bestSat, bestVal, isDark)
            }
        } catch (_: Throwable) {}

        return extractFromSeed(fallbackSeed, isDark)
    }

    private fun extractFromSeed(seed: Color, isDark: Boolean): DynamicPalette {
        val (h, s, v) = colorToHsv(seed.red, seed.green, seed.blue)
        return buildPaletteFromHsv(h, s.coerceIn(0.45f, 0.85f), v.coerceIn(0.4f, 0.8f), isDark)
    }

    private fun buildPaletteFromHsv(hue: Float, sat: Float, v: Float, isDark: Boolean): DynamicPalette {
        val dominant = hsvToColor(hue, sat, v)

        // Primary: accessible vibrant tone
        val primarySat = sat.coerceIn(0.55f, 0.95f)
        val primaryVal = if (isDark) v.coerceIn(0.70f, 0.95f) else v.coerceIn(0.35f, 0.55f)
        val primary = hsvToColor(hue, primarySat, primaryVal)

        // Secondary: analogous hue shift +25°
        val secHue = (hue + 25f) % 360f
        val secondary = hsvToColor(secHue, sat.coerceIn(0.5f, 0.85f), if (isDark) 0.80f else 0.45f)

        // Tertiary: accent hue shift -35°
        val tertHue = (hue - 35f + 360f) % 360f
        val tertiary = hsvToColor(tertHue, 0.75f, if (isDark) 0.85f else 0.40f)

        // Ambient Backgrounds
        val surfaceBackground = if (isDark) {
            mixColor(Color(0xFF0F172A), dominant, 0.22f)
        } else {
            mixColor(Color(0xFFF8FAFC), dominant, 0.12f)
        }

        val gradientTop = if (isDark) {
            mixColor(Color(0xFF0B1120), dominant, 0.35f)
        } else {
            mixColor(Color(0xFFFFFFFF), dominant, 0.20f)
        }

        val gradientBottom = if (isDark) {
            Color(0xFF090D16)
        } else {
            Color(0xFFEDF2F7)
        }

        val onBackground = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
        val onPrimary = if (isDark && primaryVal > 0.65f) Color(0xFF090D16) else Color.White
        val accent = hsvToColor((hue + 180f) % 360f, 0.70f, if (isDark) 0.90f else 0.50f)

        return DynamicPalette(
            dominantColor = dominant,
            primaryColor = primary,
            secondaryColor = secondary,
            tertiaryColor = tertiary,
            surfaceBackground = surfaceBackground,
            gradientTop = gradientTop,
            gradientBottom = gradientBottom,
            onBackground = onBackground,
            onPrimary = onPrimary,
            accent = accent,
            isDark = isDark,
        )
    }

    private fun colorToHsv(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        val v = max
        val s = if (max != 0f) delta / max else 0f
        var h = if (delta == 0f) 0f else when (max) {
            r -> 60f * (((g - b) / delta) % 6f)
            g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }
        if (h < 0f) h += 360f
        return Triple(h, s, v)
    }

    private fun hsvToColor(h: Float, s: Float, v: Float): Color {
        val normH = ((h % 360f) + 360f) % 360f
        val c = v * s
        val x = c * (1f - abs(((normH / 60f) % 2f) - 1f))
        val m = v - c
        val (r1, g1, b1) = when ((normH / 60f).toInt() % 6) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return Color(
            (r1 + m).coerceIn(0f, 1f),
            (g1 + m).coerceIn(0f, 1f),
            (b1 + m).coerceIn(0f, 1f),
            1f,
        )
    }

    private fun mixColor(c1: Color, c2: Color, ratio: Float): Color {
        val r = (c1.red + (c2.red - c1.red) * ratio).coerceIn(0f, 1f)
        val g = (c1.green + (c2.green - c1.green) * ratio).coerceIn(0f, 1f)
        val b = (c1.blue + (c2.blue - c1.blue) * ratio).coerceIn(0f, 1f)
        return Color(r, g, b, 1f)
    }
}

/**
 * Remember dynamic palette in Compose, invalidating ONLY when track ID, artwork bitmap,
 * or dark theme changes. Never invalidates on playback timeline progress updates.
 */
@Composable
fun rememberDynamicPalette(
    track: Track?,
    bitmap: Bitmap?,
    isDark: Boolean = isSystemInDarkTheme(),
): DynamicPalette {
    val trackId = track?.id ?: 0L
    val fallbackSeed = track?.artworkSeed ?: Color(0xFF0F766E)

    return remember(trackId, bitmap, isDark) {
        val cacheKey = if (trackId > 0L) "track_$trackId" else null
        PaletteGenerator.generate(bitmap, fallbackSeed, isDark, cacheKey)
    }
}
