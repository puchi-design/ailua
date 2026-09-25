package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = AiluaMistBlue,
    onPrimary = AiluaWarmWhite,
    primaryContainer = AiluaMistBlueSoft,
    onPrimaryContainer = AiluaCharcoal,
    secondary = AiluaMutedLavender,
    onSecondary = AiluaWarmWhite,
    secondaryContainer = AiluaLavenderSoft,
    onSecondaryContainer = AiluaCharcoal,
    tertiary = AiluaDustyRose,
    onTertiary = AiluaWarmWhite,
    tertiaryContainer = AiluaRoseSoft,
    onTertiaryContainer = AiluaCharcoal,
    background = AiluaCreamBg,
    onBackground = AiluaCharcoal,
    surface = AiluaCardLight,
    onSurface = AiluaCharcoal,
    surfaceVariant = AiluaCardTonal,
    onSurfaceVariant = AiluaTextSecondary,
    outline = AiluaBorder,
    outlineVariant = AiluaDivider
)

private val DarkColorScheme = darkColorScheme(
    primary = AiluaNightMistBlue,
    onPrimary = AiluaNightBg,
    primaryContainer = AiluaNightCardTonal,
    onPrimaryContainer = AiluaNightTextPrimary,
    secondary = AiluaNightLavender,
    onSecondary = AiluaNightBg,
    secondaryContainer = AiluaNightCard,
    onSecondaryContainer = AiluaNightTextPrimary,
    tertiary = AiluaNightRose,
    onTertiary = AiluaNightBg,
    tertiaryContainer = AiluaNightCardTonal,
    onTertiaryContainer = AiluaNightTextPrimary,
    background = AiluaNightBg,
    onBackground = AiluaNightTextPrimary,
    surface = AiluaNightSurface,
    onSurface = AiluaNightTextPrimary,
    surfaceVariant = AiluaNightCard,
    onSurfaceVariant = AiluaNightTextSecondary,
    outline = AiluaNightBorder,
    outlineVariant = AiluaNightDivider
)

val AiluaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

data class AiluaThemeTokens(
    val wallpaperColors: List<Color>,
    val dockSurface: Color,
    val widgetSurface: Color,
    val characterAccent: Color,
    val cardBorder: Color
)

val LightAiluaTokens = AiluaThemeTokens(
    wallpaperColors = listOf(Color(0xFFF9F6F0), Color(0xFFF4F0E8), Color(0xFFEBE6DC)),
    dockSurface = Color(0xFFFFFFFF).copy(alpha = 0.88f),
    widgetSurface = Color(0xFFFFFFFF),
    characterAccent = AiluaMistBlue,
    cardBorder = Color(0xFFE4DFD6).copy(alpha = 0.6f)
)

val DarkAiluaTokens = AiluaThemeTokens(
    wallpaperColors = listOf(Color(0xFF13121C), Color(0xFF191724), Color(0xFF1F1B2F)),
    dockSurface = Color(0xFF1E1C27).copy(alpha = 0.88f),
    widgetSurface = Color(0xFF272435),
    characterAccent = AiluaNightMistBlue,
    cardBorder = Color(0xFF38344A).copy(alpha = 0.6f)
)

val LocalAiluaTokens = staticCompositionLocalOf { LightAiluaTokens }

@Composable
fun AiluaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val tokens = if (darkTheme) DarkAiluaTokens else LightAiluaTokens

    CompositionLocalProvider(LocalAiluaTokens provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AiluaTypography,
            shapes = AiluaShapes,
            content = content
        )
    }
}
