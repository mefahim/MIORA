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
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Theme choice persisted in Settings (DataStore). */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Accent choice. DYNAMIC follows Material You on Android 12+; the fixed choices
 *  are tasteful, non-purple fallbacks. No accent is ever hardcoded as the brand. */
enum class AccentChoice { DYNAMIC, OCEAN, FOREST, EMBER, ROSE, GRAPHITE }

/**
 * Fallback tonal palette used when dynamic colour is unavailable or off.
 * The seed is a deep ocean teal-blue; the scheme is built from tonal offsets
 * so light/dark surfaces stay readable without a single permanent brand hue.
 */
private fun fallbackScheme(dark: Boolean, seed: Color): ColorScheme {
    val primary = seed
    return if (dark) {
        darkColorScheme(
            primary = primary,
            secondary = seed.copy(red = (seed.red + 0.12f).coerceAtMost(1f)),
            tertiary = Color(0xFF8AB4F8),
            background = Color(0xFF101414),
            surface = Color(0xFF191C1C),
            surfaceVariant = Color(0xFF232828),
            onPrimary = Color.White,
            onBackground = Color(0xFFE6E8E6),
            onSurface = Color(0xFFE6E8E6),
        )
    } else {
        lightColorScheme(
            primary = seed,
            secondary = Color(0xFF4A6B6B),
            tertiary = Color(0xFF2F5DA8),
            background = Color(0xFFF7F8F7),
            surface = Color.White,
            surfaceVariant = Color(0xFFE9EDED),
            onPrimary = Color.White,
            onBackground = Color(0xFF191C1C),
            onSurface = Color(0xFF191C1C),
        )
    }
}

fun accentSeed(choice: AccentChoice): Color = when (choice) {
    AccentChoice.OCEAN -> Color(0xFF0F766E)
    AccentChoice.FOREST -> Color(0xFF2E7D32)
    AccentChoice.EMBER -> Color(0xFFC2410C)
    AccentChoice.ROSE -> Color(0xFFBE123C)
    AccentChoice.GRAPHITE -> Color(0xFF475569)
    AccentChoice.DYNAMIC -> Color(0xFF0F766E)
}

/** Derive a coordinated accent from an album's seed colour (artwork-derived theming). */
fun ColorScheme.withArtworkAccent(seed: Color): ColorScheme = copy(
    primary = seed,
    secondaryContainer = seed.copy(alpha = 0.18f),
)

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
)

/** Spacing scale — single source of truth, no magic numbers in screens. */
@Immutable
object Spacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
}

/** Touch targets are at least 48dp (Material accessibility minimum). */
@Immutable
object TouchTargets { val min: Dp = 48.dp }

@Composable
fun PowerampCloneTheme(
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
    var scheme: ColorScheme = when {
        dynamicColor && accent == AccentChoice.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> fallbackScheme(dark, accentSeed(accent))
    }
    if (artworkSeed != null) scheme = scheme.withArtworkAccent(artworkSeed)
    MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
}
