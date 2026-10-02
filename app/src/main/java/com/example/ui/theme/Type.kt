package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Six sizes shared by the runtime and Material components. Line height supplies
// reading room; individual screens do not tune fractional font sizes.
private val display = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 30.sp, lineHeight = 38.sp)
private val title = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp)
private val section = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 24.sp)
private val body = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 24.sp)
private val secondary = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 20.sp)
private val caption = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 16.sp)

val AiluaTypography = Typography(
    displayLarge = display, displayMedium = display, displaySmall = display,
    headlineLarge = display, headlineMedium = title, headlineSmall = title,
    titleLarge = title, titleMedium = section, titleSmall = body,
    bodyLarge = body, bodyMedium = body, bodySmall = secondary,
    labelLarge = secondary, labelMedium = caption, labelSmall = caption,
)