package com.fahim.powerampclone.design

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Theme choice persisted in Settings (DataStore). */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Accent choice. DYNAMIC follows Material You on Android 12+; the fixed choices
 * match the MIORA design tokens (Ocean, Forest, Ember, Rose, Graphite).
 */
enum class AccentChoice { DYNAMIC, OCEAN, FOREST, EMBER, ROSE, GRAPHITE }

/**
 * Coordinated palette derived from artwork or accent seeds via HSL color math.
 * Directly mirrors web-preview/js/theme.js derivePalette().
 */
@Immutable
data class MioraPalette(
    val accent: Color,
    val accent2: Color,
    val accent3: Color,
    val softAccent: Color,
    val onAccent: Color,
    val playerBg1: Color,
    val playerBg2: Color,
)

val LocalMioraPalette = staticCompositionLocalOf {
    MioraPalette(
        accent = Color(0xFF0F766E),
        accent2 = Color(0xFF2F5DA8),
        accent3 = Color(0xFF2E7D32),
        softAccent = Color(0x1F0F766E),
        onAccent = Color.White,
        playerBg1 = Color(0xFF0F766E),
        playerBg2 = Color(0xFF0B3D39),
    )
}

fun accentSeed(choice: AccentChoice): Color = when (choice) {
    AccentChoice.OCEAN -> Color(0xFF0F766E)
    AccentChoice.FOREST -> Color(0xFF2E7D32)
    AccentChoice.EMBER -> Color(0xFFC2410C)
    AccentChoice.ROSE -> Color(0xFFBE123C)
    AccentChoice.GRAPHITE -> Color(0xFF64748B)
    AccentChoice.DYNAMIC -> Color(0xFF0F766E)
}

/** Convert Color to HSL [hue (0..360), saturation (0..100), lightness (0..100)] */
fun colorToHsl(color: Color): FloatArray {
    val r = color.red
    val g = color.green
    val b = color.blue
    val maxVal = max(r, max(g, b))
    val minVal = min(r, min(g, b))
    val l = (maxVal + minVal) / 2f
    var h = 0f
    var s = 0f
    if (maxVal != minVal) {
        val d = maxVal - minVal
        s = if (l > 0.5f) d / (2f - maxVal - minVal) else d / (maxVal + minVal)
        h = when (maxVal) {
            r -> ((g - b) / d + (if (g < b) 6f else 0f)) * 60f
            g -> ((b - r) / d + 2f) * 60f
            else -> ((r - g) / d + 4f) * 60f
        }
    }
    return floatArrayOf(h, s * 100f, l * 100f)
}

/** Convert HSL [0..360, 0..100, 0..100] back to Compose Color */
fun hslToColor(hDeg: Float, sPct: Float, lPct: Float, alpha: Float = 1f): Color {
    val h = ((hDeg % 360f) + 360f) % 360f
    val s = sPct.coerceIn(0f, 100f) / 100f
    val l = lPct.coerceIn(0f, 100f) / 100f
    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs(((h / 60f) % 2f) - 1f))
    val m = l - c / 2f
    val (r1, g1, b1) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val r = ((r1 + m) * 255f).roundToInt().coerceIn(0, 255)
    val g = ((g1 + m) * 255f).roundToInt().coerceIn(0, 255)
    val b = ((b1 + m) * 255f).roundToInt().coerceIn(0, 255)
    val a = (alpha * 255f).roundToInt().coerceIn(0, 255)
    return Color((a shl 24) or (r shl 16) or (g shl 8) or b)
}

/** Compute relative luminance for WCAG contrast */
fun colorLuminance(color: Color): Float {
    fun f(c: Float): Float = if (c <= 0.03928f) c / 12.92f else Math.pow(((c + 0.055) / 1.055), 2.4).toFloat()
    return 0.2126f * f(color.red) + 0.7152f * f(color.green) + 0.0722f * f(color.blue)
}

/** Derive coordinated palette matching web-preview/js/theme.js */
fun derivePalette(seed: Color, dark: Boolean): MioraPalette {
    val hsl = colorToHsl(seed)
    val h = hsl[0]
    val s = hsl[1]
    val sat = min(s, 72f)
    val accent = hslToColor(h, sat, if (dark) 68f else 38f)
    val accent2 = hslToColor(h + 40f, min(sat, 60f), if (dark) 62f else 42f)
    val accent3 = hslToColor(h - 40f, min(sat, 60f), if (dark) 62f else 42f)
    val soft = if (dark) Color.White.copy(alpha = 0.08f) else accent.copy(alpha = 0.12f)
    val onAccent = if (colorLuminance(accent) > 0.45f) Color(0xFF141414) else Color.White
    val playerBg1 = hslToColor(h, min(sat, 55f), if (dark) 16f else 88f)
    val playerBg2 = hslToColor(h, min(sat, 60f), if (dark) 8f else 76f)
    return MioraPalette(
        accent = accent,
        accent2 = accent2,
        accent3 = accent3,
        softAccent = soft,
        onAccent = onAccent,
        playerBg1 = playerBg1,
        playerBg2 = playerBg2,
    )
}

/**
 * MIORA theme color scheme generator.
 * Light: Warm off-white surfaces (#FAF9F6, #FFFFFF, #F1EFE9), charcoal text (#171816, #5C5F5A).
 * Dark: Deep charcoal surfaces (#121313, #1A1C1C, #232626), soft cream text (#F2F1EC, #A3A6A0).
 */
private fun mioraColorScheme(dark: Boolean, palette: MioraPalette): ColorScheme {
    return if (dark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
            primaryContainer = palette.softAccent,
            onPrimaryContainer = palette.accent,
            secondary = palette.accent2,
            onSecondary = Color.White,
            secondaryContainer = palette.softAccent,
            onSecondaryContainer = palette.accent2,
            tertiary = palette.accent3,
            onTertiary = Color.White,
            background = Color(0xFF121313),
            onBackground = Color(0xFFF2F1EC),
            surface = Color(0xFF1A1C1C),
            onSurface = Color(0xFFF2F1EC),
            surfaceVariant = Color(0xFF232626),
            onSurfaceVariant = Color(0xFFA3A6A0),
            outline = Color(0xFF2E3232),
            outlineVariant = Color(0xFF383C3C),
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
            primaryContainer = palette.softAccent,
            onPrimaryContainer = palette.accent,
            secondary = palette.accent2,
            onSecondary = Color.White,
            secondaryContainer = palette.softAccent,
            onSecondaryContainer = palette.accent2,
            tertiary = palette.accent3,
            onTertiary = Color.White,
            background = Color(0xFFFAF9F6),
            onBackground = Color(0xFF171816),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF171816),
            surfaceVariant = Color(0xFFF1EFE9),
            onSurfaceVariant = Color(0xFF5C5F5A),
            outline = Color(0xFFE3E0D8),
            outlineVariant = Color(0xFFCCC8BD),
        )
    }
}

/** MIORA Typography scale directly matching web-preview/css/tokens.css */
val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 23.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 17.sp),
)

/** Spacing scale matching web-preview --sp-1 through --sp-7 */
@Immutable
object Spacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 28.dp
    val huge: Dp = 40.dp
}

@Immutable
object TouchTargets { val min: Dp = 48.dp }

@Composable
fun MioraTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    accent: AccentChoice = AccentChoice.DYNAMIC,
    artworkSeed: Color? = null,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current

    val effectiveSeed = if (dynamicColor && artworkSeed != null) {
        artworkSeed
    } else {
        accentSeed(accent)
    }

    val palette = derivePalette(effectiveSeed, dark)

    val scheme: ColorScheme = when {
        dynamicColor && artworkSeed == null && accent == AccentChoice.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val dynamicScheme = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            // Harmonize with MIORA neutral surfaces
            if (dark) {
                dynamicScheme.copy(
                    background = Color(0xFF121313),
                    surface = Color(0xFF1A1C1C),
                    surfaceVariant = Color(0xFF232626),
                    onBackground = Color(0xFFF2F1EC),
                    onSurface = Color(0xFFF2F1EC),
                    onSurfaceVariant = Color(0xFFA3A6A0),
                    outline = Color(0xFF2E3232),
                )
            } else {
                dynamicScheme.copy(
                    background = Color(0xFFFAF9F6),
                    surface = Color(0xFFFFFFFF),
                    surfaceVariant = Color(0xFFF1EFE9),
                    onBackground = Color(0xFF171816),
                    onSurface = Color(0xFF171816),
                    onSurfaceVariant = Color(0xFF5C5F5A),
                    outline = Color(0xFFE3E0D8),
                )
            }
        }
        else -> mioraColorScheme(dark, palette)
    }

    CompositionLocalProvider(LocalMioraPalette provides palette) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
    }
}

/** Backward-compatibility alias for PowerampCloneTheme */
@Composable
fun PowerampCloneTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    accent: AccentChoice = AccentChoice.DYNAMIC,
    artworkSeed: Color? = null,
    content: @Composable () -> Unit,
) {
    MioraTheme(themeMode, dynamicColor, accent, artworkSeed, content)
}
