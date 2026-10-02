package com.example.ui.systemui.lockscreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.components.LocalOsChromeState
import com.example.ui.themeengine.LocalAiluaTheme
import com.example.ui.themeengine.TypographyFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LockScreenClock(modifier: Modifier = Modifier, compact: Boolean = false) {
    val runtime = LocalAiluaTheme.current
    val timeLabel = LocalOsChromeState.current.timeLabel
    // The chrome refreshes on each device minute and after date/time-zone changes.
    val dateLabel = remember(timeLabel) {
        SimpleDateFormat("M月d日 · EEEE", Locale.SIMPLIFIED_CHINESE).format(Date())
    }
    val family = when (runtime.typography.family) {
        TypographyFamily.SERIF -> FontFamily.Serif
        TypographyFamily.MONO -> FontFamily.Monospace
        else -> FontFamily.SansSerif
    }
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            timeLabel,
            color = runtime.lockscreen.foregroundColor,
            fontSize = ((if (compact) 64f else 82f) * runtime.lockscreen.clockScale).sp,
            lineHeight = ((if (compact) 72f else 94f) * runtime.lockscreen.clockScale).sp,
            fontWeight = if (runtime.id == "glass") FontWeight.Light else runtime.typography.titleWeight,
            fontFamily = family,
            style = runtime.text.display,
            maxLines = 1
        )
        Text(dateLabel, color = runtime.lockscreen.foregroundColor.copy(alpha = 0.88f),
            fontFamily = family, style = LocalAiluaTheme.current.text.body)
    }
}
