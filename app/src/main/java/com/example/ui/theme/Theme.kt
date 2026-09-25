package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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

@Composable
fun AiluaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AiluaTypography,
        shapes = AiluaShapes,
        content = content
    )
}
